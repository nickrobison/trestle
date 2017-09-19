package com.nickrobison.trestle.querybuilder;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 8/11/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "Duplicates"})
public class QueryBuilderTest {

    private static DefaultPrefixManager pm;
    private static OWLDataFactory df;
    private QueryBuilder qb;

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
            "SELECT DISTINCT ?individual ?fact ?property ?object ?df ?dt ?vf ?vt ?va WHERE { ?individual trestle:has_fact ?fact .{?fact trestle:database_from ?df} .OPTIONAL{?fact trestle:database_to ?dt} .OPTIONAL{?fact trestle:valid_from ?vf} .OPTIONAL{?fact trestle:valid_to ?vt} .OPTIONAL{?fact trestle:valid_at ?va} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .FILTER((!bound(?vf) || ?vf <= \"1989-03-26T00:00:00Z\"^^xsd:dateTime) && (!bound(?vt) || ?vt > \"1989-03-26T00:00:00Z\"^^xsd:dateTime)) .FILTER(!bound(?va) || ?va = \"1989-03-26T00:00:00Z\"^^xsd:dateTime) .FILTER((!bound(?df) || ?df <= \"2012-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?dt) || ?dt > \"2012-01-01T00:00:00Z\"^^xsd:dateTime)) . FILTER NOT EXISTS {?property rdfs:subPropertyOf trestle:Temporal_Property}}";
    private static final String objectPropertyFilteredFactString ="BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?individual ?fact ?property ?object ?df ?dt ?vf ?vt ?va WHERE { ?individual trestle:has_fact ?fact .{?fact trestle:database_from ?df} .OPTIONAL{?fact trestle:database_to ?dt} .OPTIONAL{?fact trestle:valid_from ?vf} .OPTIONAL{?fact trestle:valid_to ?vt} .OPTIONAL{?fact trestle:valid_at ?va} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .VALUES ?property { <trestle:test_property_1> <trestle:test_property_2> } .FILTER((!bound(?vf) || ?vf <= \"1989-03-26T00:00:00Z\"^^xsd:dateTime) && (!bound(?vt) || ?vt > \"1989-03-26T00:00:00Z\"^^xsd:dateTime)) .FILTER(!bound(?va) || ?va = \"1989-03-26T00:00:00Z\"^^xsd:dateTime) .FILTER((!bound(?df) || ?df <= \"2012-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?dt) || ?dt > \"2012-01-01T00:00:00Z\"^^xsd:dateTime)) . FILTER NOT EXISTS {?property rdfs:subPropertyOf trestle:Temporal_Property}}";

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
            "SELECT DISTINCT ?individual ?fact ?property ?object ?df ?dt ?vf ?vt ?va WHERE { ?individual trestle:has_fact ?fact .{?fact trestle:database_from ?df} .OPTIONAL{?fact trestle:database_to ?dt} .OPTIONAL{?fact trestle:valid_from ?vf} .OPTIONAL{?fact trestle:valid_to ?vt} .OPTIONAL{?fact trestle:valid_at ?va} .?fact ?property ?object .VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> <http://nickrobison.com/dissertation/trestle.owl#test_muni2> <http://nickrobison.com/dissertation/trestle.owl#test_muni5> } .FILTER(!isURI(?object) && !isBlank(?object)) .FILTER(!bound(?tEnd)) .FILTER((!bound(?vf) || ?vf <= \"1989-03-26T00:00:00Z\"^^xsd:dateTime) && (!bound(?vt) || ?vt > \"1989-03-26T00:00:00Z\"^^xsd:dateTime)) .FILTER(!bound(?va) || ?va = \"1989-03-26T00:00:00Z\"^^xsd:dateTime) .FILTER((!bound(?df) || ?df <= \"2012-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?dt) || ?dt > \"2012-01-01T00:00:00Z\"^^xsd:dateTime)) . FILTER NOT EXISTS {?property rdfs:subPropertyOf trestle:Temporal_Property}}";

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
            "SELECT DISTINCT ?individual ?property ?object WHERE { OPTIONAL{?individual trestle:exists_at ?tAt} . OPTIONAL{?individual trestle:exists_from ?tStart} . OPTIONAL{?individual trestle:exists_to ?tEnd} . ?individual ?property ?object VALUES ?individual { <http://nickrobison.com/dissertation/trestle.owl#test_muni4> } . FILTER(!isURI(?object) && !isBlank(?object)) .}";

    private static final String tsIntersectString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m ?tStart ?tEnd WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f ogc:asWKT ?wkt .OPTIONAL{?f trestle:valid_from ?tStart} .OPTIONAL{?f trestle:valid_to ?tEnd} .OPTIONAL{?f trestle:valid_at ?tAt} .?f trestle:database_from ?df .OPTIONAL{?f trestle:database_to ?dt} .FILTER(?df <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime && (!bound(?dt) || ?dt > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) .FILTER(((!bound(?tStart) || ?tStart <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, \"Point(39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String tsConceptString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:Trestle_Concept .?m trestle:related_by ?r .?r trestle:Relation_Strength ?rs .?r trestle:relation_of ?object .?object trestle:has_fact ?f .OPTIONAL {?f trestle:valid_from ?tStart }.OPTIONAL {?f trestle:valid_to ?tEnd }.OPTIONAL {?f trestle:valid_at ?tAt }.?f trestle:database_from ?df .OPTIONAL {?f trestle:database_to ?dt }.?f ogc:asWKT ?wkt .FILTER(?rs >= \"0.0\"^^xsd:double) .FILTER(?df <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime && (!bound(?dt) || ?dt > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) .FILTER(((!bound(?tStart) || ?tStart <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, \"Point(39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

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
            "SELECT DISTINCT ?m ?o ?p WHERE { { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Temporal_Relation } UNION { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Spatial_Relation .} UNION { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Event_Relation . ?p rdf:type trestle:Trestle_Object} . VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>}}";

    private static final String updateTemporalString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
            "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
            "INSERT {?m trestle:database_to \"2017-03-11T00:00:00Z\"^^xsd:dateTime}  WHERE { VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#test_muni4>} . OPTIONAL{?m trestle:database_to ?dt} . ?m rdf:type trestle:Interval_Object .FILTER(!bound(?dt))}";

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
        qb = new QueryBuilder(QueryBuilder.Dialect.ORACLE, pm);
    }

    @Test
    public void testRelationQuery() {
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));

        assertAll(() -> {
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
                    //        Test st-intersection
                    final String tsString = qb.buildTemporalSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.Units.KM, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
                    assertEquals(tsIntersectString, tsString, "TS should be equal");
                },
                () -> {
                    //        Test concept
                    final String generatedTSConceptString = qb.buildTemporalSpatialConceptIntersection(wktString, 0.0, 0.0, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
                    assertEquals(tsConceptString, generatedTSConceptString, "TS Concept intersection be equal");
                });
    }

    @Test
    public void testObjectProperty() {
        final ImmutableList<OWLDataProperty> propertyList = ImmutableList.of(
                df.getOWLDataProperty(IRI.create("trestle:", "test_property_1")),
                df.getOWLDataProperty(IRI.create("trestle:", "test_property_2")));

        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        assertAll(() -> {
                    final String generatedObjectStartInterval = qb.buildObjectFactRetrievalQuery(OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2012, 1, 1).atStartOfDay(), ZoneOffset.UTC), true, null, test_muni4);
                    assertEquals(objectPropertyStartIntervalString, generatedObjectStartInterval, "Start interval query should be equal");
                },
                () -> {
                    final String generatedObjectFactFilteredQuery = qb.buildObjectFactRetrievalQuery(OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2012, 1, 1).atStartOfDay(), ZoneOffset.UTC), true, propertyList, test_muni4);
                    assertEquals(generatedObjectFactFilteredQuery, generatedObjectFactFilteredQuery, "Should have filtered data properties");
                },
                () -> {
                    final String generatedMultiIRI = qb.buildObjectFactRetrievalQuery(OffsetDateTime.of(LocalDate.of(1989, 3, 26).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2012, 1, 1).atStartOfDay(), ZoneOffset.UTC), true, null, test_muni4, df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni2")), df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni5")));
                    assertEquals(objectPropertyMultiIRIString, generatedMultiIRI, "Multi IRI property query Should be equal");
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
        assertAll(() -> {
                    final String temporalQuery = qb.buildIndividualTemporalQuery(test_muni4);
                    assertEquals(individualTemporalQueryString, temporalQuery, "Should be equal");
                },
                () -> {
                    final String updateUnboundedTemporal = qb.buildUpdateUnboundedTemporal(OffsetDateTime.of(2017, 3, 11, 0, 0, 0, 0, ZoneOffset.UTC), test_muni4);
                    assertEquals(updateTemporalString, updateUnboundedTemporal, "Update unbounded temporal should match");
                });
    }
}
