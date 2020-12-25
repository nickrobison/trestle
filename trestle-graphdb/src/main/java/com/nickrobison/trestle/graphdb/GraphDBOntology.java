package com.nickrobison.trestle.graphdb;

import com.nickrobison.trestle.ontology.RDF4JOntology;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.ontology.utils.RDF4JLiteralFactory;
import com.nickrobison.trestle.ontology.utils.SharedOntologyFunctions;
import com.ontotext.trree.config.OWLIMSailSchema;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
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
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

/**
 * Created by nrobison on 1/10/17.
 */

@SuppressWarnings({"initialization.fields.uninitialized"})
public class GraphDBOntology extends RDF4JOntology {

    private static final Logger logger = LoggerFactory.getLogger(GraphDBOntology.class);
    private static final String DATA_DIRECTORY = "target/data";
    private static final String GEOSPARQL_ENABLE = "PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
            "INSERT DATA {_:s :enabled \"true\".}";
    private static RepositoryManager repositoryManager;
    //    private static RepositoryConnection connection;
//    private static Repository repository;
    private static final Config config = ConfigFactory.load().getConfig("trestle.ontology.graphdb");

    GraphDBOntology(String ontologyName, @Nullable String connectionString, String username, String password, OWLOntology ont, DefaultPrefixManager pm, RDF4JLiteralFactory factory) {
        super(ontologyName, constructRepository(ontologyName, connectionString, username, password, factory), ont, pm, factory);
    }

    private static Repository constructRepository(String ontologyName, @Nullable String connectionString, String username, String password, RDF4JLiteralFactory factory) {
        logger.debug("Constructing GraphDB ontology with connection string {}", connectionString != null ? connectionString : "Null");
        if (connectionString == null) {
//            Connect to local repository
            repositoryManager = new LocalRepositoryManager(new File(DATA_DIRECTORY));
        } else {
//            Connect to remote repository
            repositoryManager = RemoteRepositoryManager.getInstance(connectionString, username, password);
        }

        repositoryManager.init();
        final Repository repository = repositoryManager.getRepository(ontologyName);
//        If the repository doesn't exist, create it
        if (repository == null) {
            return setupNewRepository(ontologyName, factory);
        }
        return repository;
    }

    @SuppressWarnings({"argument.type.incompatible"})
    private static Repository setupNewRepository(String ontologyName, RDF4JLiteralFactory factory) {
        logger.info("Creating new Repository {}", ontologyName);
        final SimpleValueFactory vf = factory.getValueFactory();
        final TreeModel graph = new TreeModel();

//        Read configuration file
        final ClassLoader classLoader = GraphDBOntology.class.getClassLoader();
        if (classLoader == null) {
            throw new RuntimeException("Unable to get classloader for GraphDB Ontology");
        }
        logger.debug("Classloader: {}", classLoader);
        final String defaultsFileString = config.getString("defaults-file");
        logger.debug("Defaults File: {}", defaultsFileString);
        final InputStream is = classLoader.getResourceAsStream(defaultsFileString);
        if (is == null) {
            throw new MissingResourceException("Unable to load GraphDB defaults", GraphDBOntology.class.getName(), defaultsFileString);
        }
        logger.debug("GraphDB defaults file: {}", is);
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
    public void initializeOntology() {
        logger.info("Initializing new ontology {}", this.ontologyName);
        logger.info("Removing all statements from repository");

        this.adminConnection.begin();
        this.adminConnection.remove(SESAME.WILDCARD, SESAME.WILDCARD, SESAME.WILDCARD);
        this.adminConnection.commit();
        this.adminConnection.begin();
        try {
            this.adminConnection.add(SharedOntologyFunctions.ontologytoIS(this.ontology), "urn:base", RDFFormat.RDFXML);
        } catch (IOException | OWLOntologyStorageException e) {
            logger.error("Cannot load ontology", e);
            throw new RuntimeException("Cannot load ontology", e);
        } finally {
            this.adminConnection.commit();
        }

//        logger.debug("Enabling Spatial support");
//        this.adminConnection.begin();
//        try {
//            final Update update = this.adminConnection.prepareUpdate(QueryLanguage.SPARQL, GEOSPARQL_ENABLE);
//            update.execute();
//        } catch (Exception e) {
//            logger.error("Cannot enable Geosparql", e);
//            this.adminConnection.rollback();
//            throw e;
//        } finally {
//            this.adminConnection.commit();
//        }
        logger.info("Ontology {} ready to go", this.ontologyName);
    }

    @Override
    public void closeDatabase(boolean drop) {
        if (drop) {
            logger.info("Dropping model {} at {}", this.ontologyName, DATA_DIRECTORY);
            if (repositoryManager.isSafeToRemove(this.ontologyName)) {
                repositoryManager.removeRepository(this.ontologyName);
                repositoryManager.shutDown();
                if ((repositoryManager instanceof LocalRepositoryManager) && config.getBoolean("removeDirectory")) {
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
        } else {
            repositoryManager.shutDown();
        }
    }

    @Override
    public Completable executeUpdateSPARQL(String queryString) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            final Update update = this.getThreadConnection().prepareUpdate(QueryLanguage.SPARQL, queryString);
            update.execute();
        })
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    @SuppressWarnings({"return.type.incompatible"})
    public Flowable<TrestleResult> executeSPARQLResults(String queryString) {
        final TrestleResultSet results;
        this.openTransaction(false);
        final TupleQuery tupleQuery = this.getThreadConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult resultSet = tupleQuery.evaluate();
        return Flowable.fromIterable(resultSet)
                .map(this::buildResult)
                .doOnError(error -> this.unlockAndAbort(false))
                .doOnComplete(() -> this.commitTransaction(false))
                .doFinally(resultSet::close);
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        if (this.tc.get() == null) {
            logger.debug("Thread has no open connection, creating a new one");
            this.setOntologyConnection();
        }
        this.getThreadConnection().begin();
        logger.debug("Opened GraphDB transaction");
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        this.getThreadConnection().commit();
        this.resetThreadConnection();
        logger.debug("GraphDB model transaction committed");
    }

    @Override
    public void abortDatasetTransaction(boolean write) {
        this.getThreadConnection().rollback();
        this.resetThreadConnection();
        logger.debug("GraphDB model transaction aborted");
    }
}
