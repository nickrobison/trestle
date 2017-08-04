package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import oracle.spatial.rdf.client.jena.*;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.Lock;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

/**
 * Created by nrobison on 5/23/16.
 */
@SuppressWarnings({"initialization"})
public class OracleOntology extends JenaOntology {

    private static final Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    private static GraphOracleSem graph;
    private static Oracle oracle;
    private static boolean updateOnCommit;

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, createOracleModel(name, connectionString, username, password), ont, pm, QueryBuilder.Dialect.ORACLE);
    }

    private static Model createOracleModel(String ontologyName, String connectionString, String username, String password) {
//        Read in the settings file
        final Config config = ConfigFactory.load().getConfig("trestle.ontology.oracle");
        updateOnCommit = config.getBoolean("updateOnCommit");
        if (updateOnCommit) {
            logger.info("Initializing Oracle: Updating inference on write commit");
        } else {
            logger.info("Initializing Oracle: Manually updating inference");
        }
        final Attachment reasonerAttachment = Attachment.createInstance(
                new String[]{}, "OWL2RL",
                InferenceMaintenanceMode.NO_UPDATE, QueryOptions.DEFAULT);
        reasonerAttachment.setInferenceOption(String.format("INC=T,RAW8=T,DOP=%s", config.getInt("parallelism")));
        oracle = new Oracle(connectionString, username, password);
        try {
//            We need this so that it actually creates the model if it doesn't exist
            ModelOracleSem.createOracleSemModel(oracle, ontologyName);
            graph = new GraphOracleSem(oracle, ontologyName, reasonerAttachment);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create oracle model", e);
        }

        return new ModelOracleSem(graph);
    }

    @Override
    public boolean isConsistent() {
        logger.warn("Validation isn't implemented");
        return true;
    }

    @Override
    public void initializeOntology() {
        if (!model.isEmpty()) {
            logger.info("Dropping Oracle ontology {}", ontologyName);
            model.removeAll();
        }

        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        try {
            graph.dropApplicationTableIndex();
        } catch (SQLException e) {
            logger.error("Cannot drop application index", e);
        }
        logger.debug("Writing out the ontology to byte array");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
//            Jena doesn't support OWL/XML, so we need base RDF.
            ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Cannot save ontology to bytearray", e);
        }

        final ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
        logger.debug("Reading model from byte stream");
        logger.info("Creating new model {}", ontologyName);

        model.read(is, null);

//        Setup Inference engine
        runInference();

        logger.info("Rebuilding indexes for {}", this.ontologyName);

        try {
            graph.rebuildApplicationTableIndex();
        } catch (SQLException e) {
            logger.error("Cannot rebuild indexes for {}", this.ontologyName, e);
        }
    }

    /**
     * Run the inference engine and rebuild the indexes.
     * The inference engine is only run manually, via this method.
     */
    @Override
    public void runInference() {
        try {
            graph.performInference();
        } catch (SQLException e) {
            logger.error("Cannot run inference on Oracle ontology", e);
        }
    }

    @Override
    public void close(boolean drop) {
        logger.debug("Disconnecting");
        model.close();
        graph.close();
        if (drop) {
            logger.info("Dropping model: {}", this.ontologyName);
            try {
                OracleUtils.dropSemanticModel(oracle, this.ontologyName);
            } catch (SQLException e) {
                throw new RuntimeException("Cannot drop oracle model", e);
            }
        }
        try {
            oracle.dispose();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot disconnect from oracle database");
        }
        logger.debug("Opened {} transaction, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        this.model.commit();
        if (write) {
            try {
                graph.commitTransaction();
                if (updateOnCommit) {
                    logger.debug("Committed write transaction, updating inference");
                    runInference();
                }
            } catch (SQLException e) {
                logger.error("Cannot commit graph transaction", e);
            }
        }
        logger.debug("Transaction closed and critical section left");
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        this.model.begin();
        logger.debug("Transaction opened and critical section entered");
    }

    @Override
    public void abortDatasetTransaction(boolean write) {
        this.model.abort();
        if (write) {
            try {
                graph.rollbackTransaction();
            } catch (SQLException e) {
                logger.error("Cannot rollback graph transaction", e);
            }
        }
        logger.debug("Transaction aborted");
    }

    @Override
    @SuppressWarnings({"return.type.incompatible"})
    public TrestleResultSet executeSPARQLResults(String queryString) {
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        final TrestleResultSet resultSet;
        try {
            resultSet = this.buildResultSet(qExec.execSelect());
        } finally {
            qExec.close();
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        return resultSet;
    }

    /**
     * Return the number of asserted triples in the ontology
     *
     * @return long - Number of triples in ontology
     */
    public long getTripleCount() {

        return graph.getCount(Triple.ANY);
    }
}
