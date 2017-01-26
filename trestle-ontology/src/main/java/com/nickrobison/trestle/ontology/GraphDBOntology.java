package com.nickrobison.trestle.ontology;

import afu.org.apache.commons.io.FileUtils;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.ontotext.trree.config.OWLIMSailSchema;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema;
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
import static org.eclipse.rdf4j.model.vocabulary.SESAME.WILDCARD;

/**
 * Created by nrobison on 1/10/17.
 */

public class GraphDBOntology extends SesameOntology {

    private static final Logger logger = LoggerFactory.getLogger(GraphDBOntology.class);
    private static final String DATA_DIRECTORY = "./target/data";
    private static RepositoryManager repositoryManager;
//    private static RepositoryConnection connection;
//    private static Repository repository;
    private static final Config config = ConfigFactory.load().getConfig("trestle.ontology.graphdb");


    GraphDBOntology(String ontologyName, @Nullable String connectionString, String username, String password, OWLOntology ont, DefaultPrefixManager pm) {
        super(ontologyName, constructRepository(ontologyName, connectionString, username, password), ont, pm);
    }

    private static Repository constructRepository(String ontologyName, @Nullable String connectionString, String username, String password) {

        if (connectionString == null) {
//            Connect to local repository
            repositoryManager = new LocalRepositoryManager(new File(DATA_DIRECTORY));
        } else {
//            Connect to remote repository
            repositoryManager = RemoteRepositoryManager.getInstance(connectionString, username, password);
        }

        repositoryManager.initialize();
        final Repository repository = repositoryManager.getRepository(ontologyName);
//        If the repository doesn't exist, create it
        if (repository == null) {
            return setupNewRepository(ontologyName);
        }
        return repository;
    }

    private static Repository setupNewRepository(String ontologyName) {
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

        final Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);
        graph.add(repositoryNode, RepositoryConfigSchema.REPOSITORYID, vf.createLiteral(ontologyName));
        graph.add(repositoryNode, RDFS.LABEL, vf.createLiteral(String.format("Trestle Ontology: %s", ontologyName)));

//        Manually set some parameters
        final Resource configNode = (Resource) Models.object(graph.filter(null, SailRepositorySchema.SAILIMPL, null)).orElse(null);
//        Set reasoner profile
        final org.eclipse.rdf4j.model.IRI reasonerKey = vf.createIRI(OWLIMSailSchema.NAMESPACE, "ruleset");
        final Literal reasonerValue = vf.createLiteral(config.getString("ruleset"));
        graph.remove(configNode, reasonerKey, null);
        graph.add(configNode, reasonerKey, reasonerValue);

        final RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
        repositoryManager.addRepositoryConfig(repositoryConfig);

        return repositoryManager.getRepository(ontologyName);

//        connection = repository.getConnection();
//        connection.setIsolationLevel(IsolationLevels.READ_COMMITTED);
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
        logger.info("Removing all statements from repository");
        this.adminConnection.begin();
        this.adminConnection.remove(WILDCARD, WILDCARD, WILDCARD);
        this.adminConnection.commit();
        this.adminConnection.begin();
        try {
            this.adminConnection.add(ontologytoIS(this.ontology), "urn:base", RDFFormat.RDFXML);
        } catch (IOException | OWLOntologyStorageException e) {
            logger.error("Cannot load ontology", e);
            throw new RuntimeException("Cannot load ontology", e);
        } finally {
            this.adminConnection.commit();
        }

        //        Enable GeoSPARQL support
        logger.info("Enabling GeoSPARQL support");
        final String enableGEOSPARQL = "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
                "\n" +
                "INSERT DATA {\n" +
                "  _:s :enabled 'true' .\n" +
                "}";

//        final Update update = connection.prepareUpdate(QueryLanguage.SPARQL, enableGEOSPARQL);
//        update.execute();
        logger.info("Ontology {} ready to go", this.ontologyName);
    }

    @Override
    public void closeDatabase(boolean drop) {
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
    }

    @Override
    public void runInference() {
    }

    @Override
    public void executeUpdateSPARQL(String queryString) {
        this.openTransaction(true);
        final Update update = this.tc.get().prepareUpdate(QueryLanguage.SPARQL, queryString);
        update.execute();
        this.commitTransaction(true);
    }

    @Override
    public TrestleResultSet executeSPARQLResults(String queryString) {
        final TrestleResultSet results;
        this.openTransaction(false);
        final TupleQuery tupleQuery = this.tc.get().prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        final TupleQueryResult resultSet = tupleQuery.evaluate();
        try {
            results = buildResultSet(resultSet);
        } finally {
            resultSet.close();
            this.commitTransaction(false);
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
        if (this.tc.get() == null) {
            logger.warn("Thread has no open connection, creating a new one");
            this.setOntologyConnection();
        }
        this.tc.get().begin();
        logger.debug("Opened GraphDB transaction");
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        this.tc.get().commit();
        this.resetThreadConnection();
        logger.debug("GraphDB model transaction committed");
    }
}
