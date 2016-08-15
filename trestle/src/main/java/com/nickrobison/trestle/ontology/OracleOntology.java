package com.nickrobison.trestle.ontology;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import oracle.spatial.rdf.client.jena.*;
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

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    private static GraphOracleSem graph;
    private static Oracle oracle;
    private boolean locked = false;
    //    private final String ontologyName;
//    private final OWLOntology ontology;
////    private final PelletReasoner reasoner;
//    private final DefaultPrefixManager pm;
//    private final Oracle oracle;
//    private final OWLDataFactory df;
//    private final Model model;
//    private final GraphOracleSem graph;

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, createOracleModel(name, connectionString, username, password), ont, pm);
    }

    private static Model createOracleModel(String ontologyName, String connectionString, String username, String password) {
        final Attachment owlprime = Attachment.createInstance(
                new String[]{}, "OWLPRIME",
                InferenceMaintenanceMode.UPDATE_WHEN_COMMIT, QueryOptions.DEFAULT);
        oracle = new Oracle(connectionString, username, password);
        try {
//            We need this so that it actually creates the model if it doesn't exist
            ModelOracleSem.createOracleSemModel(oracle, ontologyName);
            graph = new GraphOracleSem(oracle, ontologyName, owlprime);
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
        try {
            runInference();
        } catch (SQLException e) {
            logger.error("Cannot setup inference engine on {}", ontologyName, e);
        }

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
     *
     * @throws SQLException
     */
    public void runInference() throws SQLException {

        logger.info("Rebuilding graph and performing inference");
        graph.analyze();
        graph.performInference();
        graph.rebuildApplicationTableIndex();
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
    }

    @Override
    public void lock() {
        this.locked = true;
    }

    @Override
    public void openAndLock(boolean write) {
        if (!locked) {
            logger.debug("Locking open transaction");
            openTransaction(write);
            lock();
        } else {
            logger.debug("Already locked, moving on");
        }
    }

    @Override
    public void unlock() {
        this.locked = false;
    }

    @Override
    public void unlockAndCommit() {
        logger.debug("Unlocking and committing");
        unlock();
        commitTransaction();
    }

    @Override
    public void commitTransaction() {
        if (!locked) {
            logger.info("Committing model transaction");
            model.commit();
        } else {
            logger.debug("Transaction locked, continuing");
        }
    }

    @Override
    public void openTransaction(boolean write) {
        if (!locked) {
            logger.debug("Opening transaction");
            model.begin();
        } else {
            logger.debug("Model is locked, keeping transaction alive");
        }
    }

    public ResultSet executeSPARQL(String queryString) {
        this.openTransaction(false);
        final Query query = QueryFactory.create(queryString);

        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        ResultSet resultSet = qExec.execSelect();
        try {
            resultSet = ResultSetFactory.copyResults(resultSet);
        } catch (Exception e) {
            logger.error("Problem with copying data", e);
        }
//        ResultSetFormatter.out(System.out, resultSet, query);
        qExec.close();
        this.commitTransaction();

        return resultSet;
    }

        /**
     * Return the number of asserted triples in the ontology
     *
     * @return long - Number of triples in ontology
     */
    public long getTripleCount() {

        return this.graph.getCount(Triple.ANY);
    }
}
