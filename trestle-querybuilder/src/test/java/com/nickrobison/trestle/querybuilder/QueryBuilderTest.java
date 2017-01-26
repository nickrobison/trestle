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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 8/11/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization"})
public class QueryBuilderTest {

    private static DefaultPrefixManager pm;
    private static OWLDataFactory df;
    private QueryBuilder qb;

    private static final String relationString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT ?f ?s WHERE { ?m rdf:type trestle:GAUL . ?m trestle:has_relation ?r . ?r rdf:type trestle:Concept_Relation . ?r trestle:Relation_Strength ?s . ?r trestle:relation_of ?f . ?f rdf:type trestle:GAUL . VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>}FILTER(?s >= \"0.6\"^^xsd:double)}";

    private static final String conceptQueryNoFilter = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?concept ?individual WHERE { ?i trestle:has_relation ?r .?r trestle:Relation_Strength ?strength .?r trestle:related_to ?concept .?concept trestle:related_by ?rc .?rc trestle:Relation_Strength ?strength .?rc trestle:relation_of ?individual .VALUES ?i {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>} .FILTER(?strength >= \"0.6\"^^xsd:double)}";

    private static final String conceptQueryFilter = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?concept ?individual WHERE { ?i trestle:has_relation ?r .?r trestle:Relation_Strength ?strength .?r trestle:related_to ?concept .?concept trestle:related_by ?rc .?rc trestle:Relation_Strength ?strength .?rc trestle:relation_of ?individual .VALUES ?i {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>} .FILTER(?strength >= \"0.6\"^^xsd:double). VALUES ?concept {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>}}";

    private static final String oracleSpatialString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX ORACLE_SEM_HT_NS: <http://oracle.com/semtech#leading(?wkt)>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f ogc:asWKT ?wkt FILTER(ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String virtuosoSpatialString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f ogc:asWKT ?wkt FILTER(bif:st_intersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral, \"0.0\"^^xsd:double)) }";

    private static final String oracleTSString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX ORACLE_SEM_HT_NS: <http://oracle.com/semtech#leading(?wkt)>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m ?tStart ?tEnd WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f ogc:asWKT ?wkt .?f trestle:valid_time ?t .{?t trestle:start_temporal ?tStart} .OPTIONAL{?t trestle:end_temporal ?tEnd} .FILTER((?tStart < \"2014-01-01T00:00:00\"^^xsd:dateTime && ?tEnd >= \"2014-01-01T00:00:00\"^^xsd:dateTime) && ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String objectPropertyStartIntervalString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?individual ?fact ?property ?object WHERE { ?individual trestle:has_fact ?fact .?fact trestle:database_time ?d .{?d trestle:valid_from ?tStart} .OPTIONAL{?d trestle:valid_to ?tEnd} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .FILTER(?tStart < \"1989-03-26T00:00:00\"^^xsd:dateTime && ?tEnd >= \"1989-03-26T00:00:00\"^^xsd:dateTime)}";

    private static final String objectPropertyEmptyIntervalString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?individual ?fact ?property ?object WHERE { ?individual trestle:has_fact ?fact .?fact trestle:database_time ?d .{?d trestle:valid_from ?tStart} .OPTIONAL{?d trestle:valid_to ?tEnd} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .}";

    private static final String objectPropertyMultiIRIString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?individual ?fact ?property ?object WHERE { ?individual trestle:has_fact ?fact .?fact trestle:database_time ?d .{?d trestle:valid_from ?tStart} .OPTIONAL{?d trestle:valid_to ?tEnd} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> <http://nickrobison.com/dissertation/trestle.owl#test_muni2> <http://nickrobison.com/dissertation/trestle.owl#test_muni5> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .FILTER(?tStart < \"1989-03-26T00:00:00\"^^xsd:dateTime && ?tEnd >= \"2012-01-01T00:00:00\"^^xsd:dateTime)}";

    private static final String individualQueryNullClassString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE {?m rdf:type trestle:Dataset .FILTER (contains(lcase(str(?m)), \"4372\"))} LIMIT 10";

    private static final String individualQueryTestClass = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE {?m rdf:type trestle:testClass .FILTER (contains(lcase(str(?m)), \"4372\"))} LIMIT 50";

    private static final String individualTemporalQueryString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?individual ?temporal ?property ?object WHERE { ?individual trestle:exists_time ?temporal . OPTIONAL{?temporal trestle:exists_at ?tAt} . OPTIONAL{?temporal trestle:exists_from ?tStart} . OPTIONAL{?temporal trestle:exists_to ?tEnd} . ?temporal ?property ?object VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } . FILTER(!isURI(?object) && !isBlank(?object)) .}";

    private static final String tsConceptString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX ORACLE_SEM_HT_NS: <http://oracle.com/semtech#leading(?wkt)>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:Trestle_Concept .?m trestle:related_by ?r .?r trestle:relation_of ?object .?object trestle:has_fact ?f .?f trestle:valid_time ?ft .?f ogc:asWKT ?wkt .FILTER(ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String individualRelationString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m ?o ?p WHERE { { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Temporal_Relation } UNION { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Spatial_Relation .} . VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>}}";

    @BeforeAll
    public static void createPrefixes() {
        df = OWLManager.getOWLDataFactory();
        pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://nickrobison.com/test/trestle.owl#");
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("ogcf:", "http://www.opengis.net/def/function/geosparql/");
        pm.setPrefix("ogc:", "http://www.opengis.net/ont/geosparql#");
        pm.setPrefix("trestle:", "http://nickrobison.com/dissertation/trestle.owl#");
    }

    @BeforeEach
    public void setup() {
        qb = new QueryBuilder(QueryBuilder.DIALECT.ORACLE, pm);
    }

    @Test
    public void testRelationQuery() {
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));

        assertAll(() -> {
                    final String relationQuery = qb.buildRelationQuery(test_muni4, gaulClass, 0.6);
                    assertEquals(relationString, relationQuery, "Relation string query");
                },
                () -> {
                    final String nullConceptQuery = qb.buildConceptRetrievalQuery(test_muni4, null, 0.6);
                    assertEquals(conceptQueryNoFilter, nullConceptQuery, "Concept query, no filter");
                },
                () -> {
                    final String filteredConceptQuery = qb.buildConceptRetrievalQuery(test_muni4, test_muni4, 0.6);
                    assertEquals(conceptQueryFilter, filteredConceptQuery, "Concept query, filtered");
                });
    }

    @Test
    public void testSpatial() throws UnsupportedFeatureException {
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final String wktString = "Point(39.5398864750001 -12.0671005249999)";

        assertAll(() -> {
                    final String generatedOracle = qb.buildSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM);
                    assertEquals(oracleSpatialString, generatedOracle, "Should be equal");
                },
                () -> {
                    //        Test virtuoso
                    QueryBuilder virtuosoQB = new QueryBuilder(QueryBuilder.DIALECT.VIRTUOSO, pm);
                    final String generatedVirtuoso = virtuosoQB.buildSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM);
                    assertEquals(virtuosoSpatialString, generatedVirtuoso, "Should be equal");
                },
                () -> {
                    //        Test Oracle temporal
                    final String generatedOracleTS = qb.buildTemporalSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.UNITS.KM, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
                    assertEquals(oracleTSString, generatedOracleTS, "Should be equal");
                },
                () -> {
                    //        Test concept
                    final String generatedTSConceptString = qb.buildTemporalSpatialConceptIntersection(wktString, 0.0, null);
                    assertEquals(tsConceptString, generatedTSConceptString, "Should be equal");
                },
                () -> {
                    //        Check unsupported
                    QueryBuilder stardogQB = new QueryBuilder(QueryBuilder.DIALECT.STARDOG, pm);
                    assertThrows(UnsupportedFeatureException.class, () -> stardogQB.buildSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.UNITS.MILE));
                });
    }

    @Test
    public void testObjectProperty() {

        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        assertAll(() -> {
                    final String generatedObjectStartInterval = qb.buildObjectPropertyRetrievalQuery(OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), null, true, test_muni4);
                    assertEquals(objectPropertyStartIntervalString, generatedObjectStartInterval, "Should be equal");
                },
                () -> {
                    final String generatedObjectEmptyInterval = qb.buildObjectPropertyRetrievalQuery(null, null, true, test_muni4);
                    assertEquals(objectPropertyEmptyIntervalString, generatedObjectEmptyInterval, "Should be equal");
                },
                () -> {
                    final String generatedMultiIRI = qb.buildObjectPropertyRetrievalQuery(OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2012, 1, 1).atStartOfDay(), ZoneOffset.UTC), true, test_muni4, df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni2")), df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni5")));
                    assertEquals(objectPropertyMultiIRIString, generatedMultiIRI, "Should be equal");
                },
                () -> {
            final String generatedIndividualRelation = qb.buildIndividualRelationQuery(test_muni4);
            assertEquals(individualRelationString, generatedIndividualRelation, "Individual object query");
        });
    }

    @Test
    public void testIndividualQuery() {
        assertAll(() -> {
                    final String nullClassQuery = qb.buildIndividualSearchQuery("4372", null, null);
                    assertEquals(individualQueryNullClassString, nullClassQuery, "Should be equal");
                },
                () -> {
                    final OWLClass testClass = df.getOWLClass(IRI.create("trestle:", "testClass"));
                    final String testClassQueryString = qb.buildIndividualSearchQuery("4372", testClass, 50);
                    assertEquals(individualQueryTestClass, testClassQueryString, "Should be equal");
                });
    }

    @Test
    public void testTemporalQuery() {
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final String temporalQuery = qb.buildIndividualTemporalQuery(test_muni4);
        assertEquals(individualTemporalQueryString, temporalQuery, "Should be equal");
    }
}
