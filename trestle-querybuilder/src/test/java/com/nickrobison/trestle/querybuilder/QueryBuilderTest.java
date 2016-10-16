package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            "SELECT DISTINCT ?m WHERE { ?m rdf:type :GAUL .?m :has_fact ?f .?f ogc:asWKT ?wkt FILTER(ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

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
            "SELECT DISTINCT ?m WHERE { ?m rdf:type :GAUL .?m :has_fact ?f .?f ogc:asWKT ?wkt FILTER(bif:st_intersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral, \"0.0\"^^xsd:double)) }";

    private static final String oracleTSString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
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
            "SELECT DISTINCT ?m ?tStart ?tEnd WHERE { ?m :has_fact ?f .?f ogc:asWKT ?wkt .?m :has_temporal ?t .{ ?t :valid_from ?tStart} UNION {?t :exists_from ?tStart} .OPTIONAL{{ ?t :valid_to ?tEnd} UNION {?t :exists_to ?tEnd}} .FILTER((?tStart < \"2014-01-01T00:00:00\"^^xsd:dateTime && ?tEnd >= \"2014-01-01T00:00:00\"^^xsd:dateTime) && ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String objectPropertyStartIntervalString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?f WHERE { ?m :has_fact ?f .?f :database_time ?d .?d :valid_from ?tStart .OPTIONAL{ ?d :valid_to ?tEnd} . FILTER(?m = <http://nickrobison.com/dissertation/trestle.owl#test_muni4> && (?tStart < \"1989-03-26T00:00:00\"^^xsd:dateTime && ?tEnd >= \"1989-03-26T00:00:00\"^^xsd:dateTime))}";

    private static final String objectPropertyEmptyIntervalString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?f WHERE { ?m :has_fact ?f .?f :database_time ?d .?d :valid_from ?tStart .OPTIONAL{ ?d :valid_to ?tEnd} . FILTER(?m = <http://nickrobison.com/dissertation/trestle.owl#test_muni4> && !bound(?tEnd))}";

    private static final String individualQueryNullClasString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE {?m rdf:type :Dataset .FILTER (contains(lcase(str(?m)), \"4372\"))}";

    private static final String individualQueryTestClass = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE {?m rdf:type :testClass .FILTER (contains(lcase(str(?m)), \"4372\"))}";

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

//        Test Oracle temporal
        final String generatedOracleTS = qb.buildTemporalSpatialIntersection(QueryBuilder.DIALECT.ORACLE, gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
        assertEquals(oracleTSString, generatedOracleTS, "Should be equal");

//        Check unsupported
        assertThrows(UnsupportedFeatureException.class, () -> qb.buildSpatialIntersection(QueryBuilder.DIALECT.STARDOG, gaulClass, wktString, 0.0, QueryBuilder.UNITS.MILE));

//        Check Object Property Retrieval

    }

    @Test
    public void testObjectProperty() {
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final String generatedObjectStartInterval = qb.buildObjectPropertyRetrievalQuery(test_muni4, OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(objectPropertyStartIntervalString, generatedObjectStartInterval, "Should be equal");

        final String generatedObjectEmptyInterval = qb.buildObjectPropertyRetrievalQuery(test_muni4, null, null);
        assertEquals(objectPropertyEmptyIntervalString, generatedObjectEmptyInterval, "Should be equal");
    }

    @Test
    public void testIndividualQuery() {
        final String nullClassQuery = qb.buildIndividualSearchQuery("4372", null);
        assertEquals(individualQueryNullClasString, nullClassQuery, "Should be equal");
        final OWLClass testClass = df.getOWLClass(IRI.create("trestle:", "testClass"));
        final String testClassQueryString = qb.buildIndividualSearchQuery("4372", testClass);
        assertEquals(individualQueryTestClass, testClassQueryString, "Should be equal");
    }
}
