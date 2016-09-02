package com.nickrobison.trestle;

import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.OracleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Created by nrobison on 9/2/16.
 */
public class SPARQLBenchmark {

    private static final String oracleSpatialString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX ORACLE_SEM_HT_NS: <http://oracle.com/semtech#leading(?wkt)>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT ?m WHERE { ?m rdf:type :GAUL_Test .?m ogc:asWKT ?wkt FILTER(ogcf:sfIntersects(?wkt, \"POLYGON ((31.08333950117128 -25.57202871446725, 31.08333950117128 -24.57695170392678, 33.8656270988277 -24.57695170392678, 33.8656270988277 -25.57202871446725, 31.08333950117128 -25.57202871446725))\"^^ogc:wktLiteral)) }";

    private static OWLDataFactory df;
    private static OracleOntology ontology;

    @BeforeAll
    public static void setup() throws OWLOntologyCreationException {
        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();

        ontology = (OracleOntology) new OntologyBuilder()

                .name("sparql_test")
                .fromIRI(iri)
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .build().get();

        ontology.initializeOntology();
    }

    @Test
    public void copy() throws MalformedURLException {

        final Model model = ontology.getUnderlyingModel();
        model.begin();
        model.read(new File("/Users/nrobison/Desktop/spatial.owl").toURI().toString(), null);
        model.commit();

        final Query query = QueryFactory.create(oracleSpatialString);

//        Try to directly get some records, to list
        Instant start = Instant.now();
        QueryExecution qExec = QueryExecutionFactory.create(query, model);
        ResultSet resultSet = qExec.execSelect();
        List<QuerySolution> querySolutions = ResultSetFormatter.toList(resultSet);
        qExec.close();
        Instant end = Instant.now();
        System.out.print(String.format("\n\n\n======================\nQuery took %s ms\n======================\n\n\n", Duration.between(start, end).toMillis()));

//        Try the full copy
        start = Instant.now();
        qExec = QueryExecutionFactory.create(query, model);
        resultSet = qExec.execSelect();
        resultSet = ResultSetFactory.copyResults(resultSet);
        qExec.close();
        end = Instant.now();
        System.out.print(String.format("\n\n\n======================\nQuery took %s ms\n======================\n\n\n", Duration.between(start, end).toMillis()));
//
////        How about a natural iterator
//        start = Instant.now();
//        qExec = QueryExecutionFactory.create(query, model);
//        resultSet = qExec.execSelect();
//
//
//        while (resultSet.hasNext()) {
//
//        }

    }

    @AfterAll
    public static void close() {
        ontology.close(true);
    }
}
