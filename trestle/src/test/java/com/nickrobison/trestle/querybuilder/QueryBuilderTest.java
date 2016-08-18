package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.exceptions.UnsupportedFeatureException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.expectThrows;

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
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT ?f ?s WHERE { ?m rdf:type :GAUL . ?m :has_relation ?r . ?r rdf:type :Concept_Relation . ?r :Relation_Strength ?s . ?r :has_relation ?f . ?f rdf:type :GAUL FILTER(?m = <http://nickrobison.com/dissertation/trestle.owl#test_muni4> && ?s >= \"0.6\"^^xsd:double)}";

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
            "SELECT ?m WHERE { ?m rdf:type :GAUL .?m ogc:asWKT ?wkt FILTER(ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String virtuosoSpatialString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT ?m WHERE { ?m rdf:type :GAUL .?m ogc:asWKT ?wkt FILTER(bif:st_intersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral, \"0.0\"^^xsd:double)) }";

    @BeforeAll
    public static void createPrefixes() {
        df = OWLManager.getOWLDataFactory();
        pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://nickrobison.com/dissertation/trestle.owl#");
//        TODO(nrobison): This should be broken into its own thing. Maybe a function to add prefixes?
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("ogcf:", "http://www.opengis.net/def/function/geosparql/");
        pm.setPrefix("ogc:", "http://www.opengis.net/ont/geosparql#");
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

    @Test
    public void testSpatial() throws UnsupportedFeatureException {
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final String wktString = "Point(39.5398864750001 -12.0671005249999)";

//        final String generatedOracle = qb.buildOracleIntersection(gaulClass, wktString);
        final String generatedOracle = qb.buildSpatialIntersection(QueryBuilder.DIALECT.ORACLE, gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM);
        assertEquals(oracleSpatialString, generatedOracle, "Should be equal");

//        Test virtuoso
        final String generatedVirtuoso = qb.buildSpatialIntersection(QueryBuilder.DIALECT.VIRTUOSO, gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM);
        assertEquals(virtuosoSpatialString, generatedVirtuoso, "Should be equal");

//        Check unsupported
        assertThrows(UnsupportedFeatureException.class, () -> qb.buildSpatialIntersection(QueryBuilder.DIALECT.STARDOG, gaulClass, wktString, 0.0, QueryBuilder.UNITS.MILE));

    }
}
