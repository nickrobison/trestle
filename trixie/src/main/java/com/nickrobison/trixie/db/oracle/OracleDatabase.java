package com.nickrobison.trixie.db.oracle;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.nickrobison.trixie.db.IOntologyDatabase;
import oracle.spatial.rdf.client.jena.ModelOracleSem;
import oracle.spatial.rdf.client.jena.Oracle;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by nrobison on 6/1/16.
 */
public class OracleDatabase implements IOntologyDatabase {

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

    public void loadOntology(String filename) {
        final InputStream is = FileManager.get().open(filename);
        model.read(is, null);
    }
}
