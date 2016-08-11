package com.nickrobison.trestle.querybuilder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 8/11/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization"})
public class QueryBuilderTest {

    private static DefaultPrefixManager pm;
    private static OWLDataFactory df;
    private QueryBuilder qb;

    private static final String relationString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX geosparql: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX spatial: <http://www.jena.apache.org/spatial#>\n" +
            "PREFIX main_geo: <http://nickrobison.com/dissertation/main_geo.owl#>\n" +
            "SELECT ?f WHERE { ?m rdf:type :GAUL .?m :has_relation ?r .?r rdf:type :Concept_Relation .?r :Relation_Strength ?s .?r :has_relation ?f .?f rdf:type :GAUL FILTER(?m = :test_muni4 && ?s >= \"0.6\"^^xsd:double)}";

    @BeforeAll
    public static void createPrefixes() {
        df = OWLManager.getOWLDataFactory();
        pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://nickrobison.com/dissertation/trestle.owl#");
//        TODO(nrobison): This should be broken into its own thing. Maybe a function to add prefixes?
        pm.setPrefix("main_geo:", "http://nickrobison.com/dissertation/main_geo.owl#");
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
//        Jena doesn't use the normal geosparql prefix, so we need to define a separate spatial class
        pm.setPrefix("spatial:", "http://www.jena.apache.org/spatial#");
        pm.setPrefix("geosparql:", "http://www.opengis.net/ont/geosparql#");
        pm.setPrefix("trestle:", "http://nickrobison.com/dissertation/trestle.owl#");
    }

    @BeforeEach
    public void setup() {
        qb = new QueryBuilder(pm);
    }

    @Test
    public void testRelationQuery() {
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));

        final String relationQuery = qb.buildRelationQuery(test_muni4, gaulClass, 0.6);
        assertEquals(relationString, relationQuery, "String should be equal");

    }
}
