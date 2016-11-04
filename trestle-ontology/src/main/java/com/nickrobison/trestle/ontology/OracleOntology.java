package com.nickrobison.trestle.ontology;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import oracle.spatial.rdf.client.jena.*;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.Lock;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by nrobison on 5/23/16.
 */
@SuppressWarnings({"initialization"})
public class OracleOntology extends JenaOntology {

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    private static GraphOracleSem graph;
    private static Oracle oracle;
    private boolean locked = false;

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, createOracleModel(name, connectionString, username, password), ont, pm);
    }

    private static Model createOracleModel(String ontologyName, String connectionString, String username, String password) {
//        Read in the settings file
        final Config config = ConfigFactory.load().getConfig("trestle.ontology.oracle");
        final InferenceMaintenanceMode mode;
        if (config.getBoolean("updateOnCommit")) {
            logger.info("Initializing Oracle: Updating reasoner on Commit");
            mode = InferenceMaintenanceMode.UPDATE_WHEN_COMMIT;
        } else {
            logger.info("Initializing Oracle: Manually updating reasoner");
            mode = InferenceMaintenanceMode.NO_UPDATE;
        }
        final Attachment owlprime = Attachment.createInstance(
                new String[]{}, "OWLPRIME",
                mode, QueryOptions.DEFAULT);
        owlprime.setInferenceOption(String.format("INC=T,RAW8=T,DOP=%s", config.getInt("parallelism")));
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
    public void runInference() {

        logger.info("Analyzing graph and performing inference");
        try {
            graph.analyze();
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
            } catch (SQLException e) {
                logger.error("Cannot commit graph transaction", e);
            }
        }
        logger.debug("Transaction closed and critical section left");
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        this.model.begin();
//        this.model.enterCriticalSection(getJenaLock(write));
        logger.debug("Transaction opened and critical section entered");
    }

    public ResultSet executeSPARQL(String queryString) {
        ResultSet resultSet;
        final Query query = QueryFactory.create(queryString);
        final long queryEnd;
        final long copyEnd;
        long queryStart = System.currentTimeMillis();
        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            resultSet = qExec.execSelect();
            queryEnd = System.currentTimeMillis();
            try {
                resultSet = ResultSetFactory.copyResults(resultSet);
            } catch (Exception e) {
                logger.error("Problem with copying data", e);
            }
            copyEnd = System.currentTimeMillis();
        } finally {
            qExec.close();
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        logger.debug("Query took {} milliseconds to complete", queryEnd - queryStart);
        logger.debug("Copying the ResultSet took {} milliseconds", copyEnd - queryEnd);

        return resultSet;
    }

    public Optional<List<Map<String, OWLObject>>> sparqlResults(String queryString) {
        List<Map<String, OWLObject>> results = new ArrayList<>();
        final Query query = QueryFactory.create(queryString);
//        Get the query result vars
        final List<String> resultVars = query.getResultVars();
        long queryStart = System.currentTimeMillis();
        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            ResultSet resultSet = qExec.execSelect();
            long queryEnd = System.currentTimeMillis();

            try {
                while (resultSet.hasNext()) {
                    Map<String, OWLObject> rowValues = new HashMap<>();
//                For each result, get the params and do what's needed
                    final QuerySolution next = resultSet.next();
                    resultVars.forEach(var -> {
                        final RDFNode rdfNode = next.get(var);
                        if (rdfNode.isResource()) {
                            rowValues.put(var, df.getOWLNamedIndividual(rdfNode.asResource().getURI()));
                        } else if (rdfNode.isLiteral()) {
                            final Optional<OWLLiteral> owlLiteral = this.parseLiteral(rdfNode.asLiteral());
                            if (owlLiteral.isPresent()) {
                                rowValues.put(var, owlLiteral.get());
                            } else {
                                logger.warn("Unable to parse OWL Literal {} for {}", rdfNode.toString(), var);
                            }
                        } else {
                            logger.warn("Unable to parse {} for {}", rdfNode.toString(), var);
                        }
                    });
                    results.add(rowValues);
                }
            } catch (Exception e) {
                logger.error("Problem iterating through resultset", e);
            } finally {
                qExec.close();
            }
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }

        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results);
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
