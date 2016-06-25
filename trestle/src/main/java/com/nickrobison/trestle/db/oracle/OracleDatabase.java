package com.nickrobison.trestle.db.oracle;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.nickrobison.trestle.db.IOntologyDatabase;
import oracle.spatial.rdf.client.jena.GraphOracleSem;
import oracle.spatial.rdf.client.jena.ModelOracleSem;
import oracle.spatial.rdf.client.jena.Oracle;
import oracle.spatial.rdf.client.jena.OracleUtils;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * Created by nrobison on 6/1/16.
 */
@Deprecated
public class OracleDatabase implements IOntologyDatabase {

    private static final Logger logger = LoggerFactory.getLogger(OracleDatabase.class);
    private final String modelName;
    private final Oracle oracle;
    private Model model;
    private Graph graph;

    public OracleDatabase(String connectionString, String username, String password, String modelName) throws SQLException {
//        oracle = new Oracle("jdbc:oracle:thin:@oracle:1521:spatial", "spatial", "spatialUser");
        this.modelName = modelName;
        oracle = new Oracle(connectionString, username, password);
        model = ModelOracleSem.createOracleSemModel(oracle, modelName);

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
            OracleUtils.dropSemanticModel(oracle, modelName);
        } catch (SQLException e) {
            logger.error("Cannot drop model {}", modelName, e);
        }
        logger.debug("Dropped model: {}", modelName);
        try {
            model = ModelOracleSem.createOracleSemModel(oracle, modelName);
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
            logger.error("Cannot rebuild the indexes on {}", modelName, e);
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

    public Resource getIndividual(IRI iri) {
        final NsIterator nsIterator = model.listNameSpaces();
        while (nsIterator.hasNext()) {
            logger.debug("{}", nsIterator.nextNs());
        }
        final Resource resource1 = model.getResource(iri.toString());
        final StmtIterator stmtIterator = resource1.listProperties();
        while (stmtIterator.hasNext()) {
            logger.debug("{}", stmtIterator.next());
        }

////        Resource r = model.createResource(iri.toString());
//        List<Resource> resources = new ArrayList<>();
////        ResultSet materializedResults;
//        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//                "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
//                "PREFIX main_geo: <http://nickrobison.com/dissertation/main_geo.owl#> " +
//                "SELECT distinct ?m WHERE { ?m ?p ?o FILTER ( ?m =" + iri.toString() + " )}";
////                "SELECT * WHERE { " + iri.toString() +
////                " ?p ?o . FILTER( ?p not in (rdf:type))}";
//
//        final Query query = QueryFactory.create(queryString);
//        final QueryExecution qExec = QueryExecutionFactory.create(query, model);
//        final ResultSet resultSet = qExec.execSelect();
////        materializedResults = ResultSetFactory.copyResults(resultSet);
////        qExec.close();
//        while (resultSet.hasNext()) {
//            final QuerySolution querySolution = resultSet.nextSolution();
//            final Resource resource = querySolution.getResource("p");
////            final Literal literal = querySolution.getLiteral("o");
////            r.addProperty((Property) resource, literal.getLexicalForm());
//            resources.add(resource);
//        }
//        qExec.close();


        return resource1;
    }

    public void disconnect() {
        graph.close();
        model.close();
        try {
            oracle.dispose();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot dispose of Oracle connection", e);
        }
    }


}
