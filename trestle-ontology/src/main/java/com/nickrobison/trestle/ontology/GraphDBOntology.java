package com.nickrobison.trestle.ontology;

import afu.org.apache.commons.io.FileUtils;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.utils.RDF4JLiteralFactory.createOWLLiteral;
import static com.nickrobison.trestle.utils.SharedOntologyFunctions.ontologytoIS;

/**
 * Created by nrobison on 1/10/17.
 */

public class GraphDBOntology extends SesameOntology {

    private static final Logger logger = LoggerFactory.getLogger(GraphDBOntology.class);
    private static final String DATA_DIRECTORY = "./target/data";
    private static RepositoryManager repositoryManager;
    private static RepositoryConnection connection;
    private static Repository repository;
    private static final Config config = ConfigFactory.load().getConfig("trestle.ontology.graphdb");


    GraphDBOntology(String ontologyName, @Nullable String connectionString, String username, String password, OWLOntology ont, DefaultPrefixManager pm) {
        super(ontologyName, constructRepository(ontologyName, connectionString, username, password), ont, pm);
    }

    private static RepositoryConnection constructRepository(String ontologyName, @Nullable String connectionString, String username, String password) {

        if (connectionString == null) {
//            Connect to local repository
            repositoryManager = new LocalRepositoryManager(new File(DATA_DIRECTORY));
        } else {
//            Connect to remote repository
            repositoryManager = RemoteRepositoryManager.getInstance(connectionString, username, password);
        }

        repositoryManager.initialize();
        repository = repositoryManager.getRepository(ontologyName);
//        If the repository doesn't exist, create it
        if (repository == null) {
            constructLocalRepository(ontologyName);
        } else {
            connection = repository.getConnection();
        }
        return connection;
    }

    private static void constructLocalRepository(String ontologyName) {
        logger.info("Creating new Local Repository {}", ontologyName);
        final TreeModel graph = new TreeModel();

//        Read configuration file
        final InputStream is = GraphDBOntology.class.getClassLoader().getResourceAsStream(config.getString("defaults-file"));
        final RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        parser.setRDFHandler(new StatementCollector(graph));
        try {
            parser.parse(is, RepositoryConfigSchema.NAMESPACE);
            is.close();
        } catch (IOException e) {
            logger.error("Cannot parse config file", e);
            throw new RuntimeException(e);
        }

        final Resource repositoryNode = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RepositoryConfigSchema.REPOSITORY);
        final RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
        repositoryConfig.setID(ontologyName);
        repositoryConfig.setTitle(String.format("Trestle Ontology: %s", ontologyName));
        repositoryManager.addRepositoryConfig(repositoryConfig);

        repository = repositoryManager.getRepository(ontologyName);

        connection = repository.getConnection();
    }

    private static void setupNewRepository(String ontologyName) {

        if (repositoryManager instanceof LocalRepositoryManager) {
            constructLocalRepository(ontologyName);
        }
    }

//    private static Model constructJenaModel() {
//        OwlimSchemaRepository schema = new OwlimSchemaRepository();
//        schema.setDataDir(new File(DATA_DIRECTORY));
//        Map<String, String> schemaParams = new HashMap<>();
//        schemaParams.put("storage-folder", "./");
//        schemaParams.put("repository-type", "file-repository");
//        schemaParams.put("ruleset", "owl-horst-optimized");
//        schemaParams.put("base-URL", "http://nickrobison.com/dissertation/trestle.owl#");
//        schemaParams.put("defaultNS", "");
//        schema.setParameters(schemaParams);
//        final SailRepository repository = new SailRepository(schema);
//        repository.initialize();
//        connection = repository.getConnection();
//        dataset = new SesameDataset(connection);
//
//        return ModelFactory.createModelForGraph(dataset.getDefaultGraph());
//
//    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public void initializeOntology() {
        logger.info("Initializing new ontology {}", this.ontologyName);
        connection.begin();
        try {
            connection.add(ontologytoIS(this.ontology), "urn:base", RDFFormat.RDFXML);
        } catch (IOException | OWLOntologyStorageException e) {
            logger.error("Cannot load ontology", e);
            throw new RuntimeException("Cannot load ontology", e);
        } finally {
            connection.commit();
        }

        //        Enable GeoSPARQL support
        logger.info("Enabling GeoSPARQL support");
        final String enableGEOSPARQL = "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                "\n" +
                "INSERT DATA {\n" +
                "  _:s :enabled 'true' .\n" +
                "}";

        final Update update = connection.prepareUpdate(QueryLanguage.SPARQL, enableGEOSPARQL);
        update.execute();
        logger.info("Ontology {} ready to go", this.ontologyName);
    }

    @Override
    public void close(boolean drop) {
        connection.close();
        repository.shutDown();

        if (drop) {
            logger.info("Dropping model {} at {}", this.ontologyName, DATA_DIRECTORY);
            if (repositoryManager.isSafeToRemove(this.ontologyName)) {
                repositoryManager.removeRepository(this.ontologyName);
                if (config.getBoolean("removeDirectory") && (repositoryManager instanceof LocalRepositoryManager)) {
                    logger.info("Removing base directory {}", DATA_DIRECTORY);
                    try {
                        FileUtils.deleteDirectory(new File(DATA_DIRECTORY));
                    } catch (IOException e) {
                        logger.error("Could not delete data directory {}", DATA_DIRECTORY, e);
                    }
                }
            } else {
                logger.error("Cannot remove repository {}", this.ontologyName);
            }
        }
        repositoryManager.shutDown();
        logger.debug("Opened {} transactions, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

    @Override
    public void runInference() {
    }

    @Override
    public TrestleResultSet executeSPARQLTRS(String queryString) {
        final TrestleResultSet results;
        final TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        final TupleQueryResult resultSet = tupleQuery.evaluate();
        try {
            results = buildResultSet(resultSet);
        } finally {
            resultSet.close();
        }
        return results;
    }

    TrestleResultSet buildResultSet(TupleQueryResult resultSet) {
        final TrestleResultSet trestleResultSet = new TrestleResultSet(0);
        while (resultSet.hasNext()) {
            final BindingSet next = resultSet.next();
            final TrestleResult results = new TrestleResult();
            final Set<String> varNames = next.getBindingNames();
            varNames.forEach(varName -> {
                final Value value = next.getBinding(varName).getValue();
//                FIXME(nrobison): This is broken, figure out how to get the correct subtypes
                if (value instanceof Literal) {
                    final Optional<OWLLiteral> owlLiteral = createOWLLiteral(Literal.class.cast(value));
                    owlLiteral.ifPresent(owlLiteral1 -> results.addValue(varName, owlLiteral1));
                } else {
                    results.addValue(varName, df.getOWLNamedIndividual(IRI.create(value.stringValue())));
                }
            });
            trestleResultSet.addResult(results);
        }
        return trestleResultSet;
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        connection.begin();
        logger.debug("Opened GraphDB transaction");
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        connection.commit();
        logger.debug("GraphDB model transaction committed");
    }
}
