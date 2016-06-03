package com.nickrobison.trixie.db.oracle;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;
import com.nickrobison.trixie.db.IOntologyDatabase;
import oracle.spatial.rdf.client.jena.GraphOracleSem;
import oracle.spatial.rdf.client.jena.ModelOracleSem;
import oracle.spatial.rdf.client.jena.Oracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by nrobison on 6/1/16.
 */
public class OracleDatabase implements IOntologyDatabase {

    private static final Logger logger = LoggerFactory.getLogger(OracleDatabase.class);

    private final Oracle oracle;
    private final Model model;
    private final Graph graph;

    public OracleDatabase() throws SQLException {
        oracle = new Oracle("jdbc:oracle:thin:@oracle:1521:spatial", "spatial", "spatialUser");
        model = ModelOracleSem.createOracleSemModel(oracle, "test2");

        graph = model.getGraph();
    }

//    public OracleDatabase(String jdbcURL, String username, String password) {
//
//        oracle = new Oracle(jdbcURL, username, password);
//    }

    public void loadBaseOntology(String filename) {
        final InputStream is = FileManager.get().open(filename);
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
            throw new RuntimeException("Cannot rebuild application indexes", e);
        }
    }

    @Override
    public void writeTuple(String subject, String predicate, String object) {
//        model.createResource();
//        final Statement statement = model.createStatement(subject, predicate, object);
//        model.
    }
}
