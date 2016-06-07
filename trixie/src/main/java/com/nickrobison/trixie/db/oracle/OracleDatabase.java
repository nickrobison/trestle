package com.nickrobison.trixie.db.oracle;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.nickrobison.trixie.db.IOntologyDatabase;
import oracle.spatial.rdf.client.jena.GraphOracleSem;
import oracle.spatial.rdf.client.jena.ModelOracleSem;
import oracle.spatial.rdf.client.jena.Oracle;
import oracle.spatial.rdf.client.jena.OracleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by nrobison on 6/1/16.
 */
public class OracleDatabase implements IOntologyDatabase {

    private static final Logger logger = LoggerFactory.getLogger(OracleDatabase.class);
    public static final String MODEL_NAME = "test1";

    private final Oracle oracle;
    private Model model;
    private Graph graph;

    public OracleDatabase(String connectionString, String username, String password) throws SQLException {
//        oracle = new Oracle("jdbc:oracle:thin:@oracle:1521:spatial", "spatial", "spatialUser");
        oracle = new Oracle(connectionString, username, password);
        model = ModelOracleSem.createOracleSemModel(oracle, MODEL_NAME);

        graph = model.getGraph();
    }

//    public OracleDatabase(String jdbcURL, String username, String password) {
//
//        oracle = new Oracle(jdbcURL, username, password);
//    }

    public void loadBaseOntology(String filename) {
        loadBaseOntology(FileManager.get().open(filename));
    }

    public void loadBaseOntology(InputStream is) {
//        Drop the existing model and reload
        try {
            OracleUtils.dropSemanticModel(oracle, MODEL_NAME);
        } catch (SQLException e) {
//            throw new RuntimeException("Can't drop model", e);
            logger.error("Cannot drop model {}", MODEL_NAME, e);
        }
        logger.debug("Dropped model: {}", MODEL_NAME);
        try {
            model = ModelOracleSem.createOracleSemModel(oracle, MODEL_NAME);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create new model", e);
        }
        model.read(is, null);
    }

    public void exportBaseOntology(String filename) {

    }

    public void enableBulkLoading() {
//        Cast the model as an OracleGraph and drop the indexes
        final GraphOracleSem oracleGraph = (GraphOracleSem) model.getGraph();
        try {
            oracleGraph.dropApplicationTableIndex();
        } catch (SQLException e) {
//            throw new RuntimeException("Cannot drop application index", e);
            logger.warn("Cannot drop application index", e);
        }
    }

    public void rebuildIndexes() {
        final GraphOracleSem oracleGraph = (GraphOracleSem) model.getGraph();
        try {
            oracleGraph.rebuildApplicationTableIndex();
        } catch (SQLException e) {
//            throw new RuntimeException("Cannot rebuild application indexes", e);
            logger.error("Cannot rebuild the indexes on {}", MODEL_NAME, e);
        }
    }

    public void writeTuple(String subject, String predicate, String object) {
//        model.createResource();
//        final Statement statement = model.createStatement(subject, predicate, object);
//        model.
    }

    public ResultSet executeRawSPARQL(String queryString) {
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = QueryExecutionFactory.create(query, model);
        final ResultSet resultSet = qExec.execSelect();

        ResultSetFormatter.out(System.out, resultSet, query);
        qExec.close();
        return resultSet;
    }

    public void disconnect() {
//        graph.close();
        model.close();
        try {
            oracle.dispose();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot dispose of Oracle connection", e);
        }
    }


}
