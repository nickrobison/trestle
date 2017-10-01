package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"initialization.fields.uninitialized"})
public class OracleQueryBuilderTest {

    private static DefaultPrefixManager pm;
    private static OWLDataFactory df;
    private QueryBuilder qb;

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
            "SELECT DISTINCT ?m ?tStart ?tEnd WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f ogc:asWKT ?wkt .OPTIONAL{?f trestle:valid_from ?tStart} .OPTIONAL{?f trestle:valid_to ?tEnd} .OPTIONAL{?f trestle:valid_at ?tAt} .?f trestle:database_from ?df .OPTIONAL{?f trestle:database_to ?dt} .FILTER(?df <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime && (!bound(?dt) || ?dt > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) .FILTER(((!bound(?tStart) || ?tStart <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

    private static final String tsOracleConceptString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
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
            "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:Trestle_Concept .?m trestle:related_by ?r .?r trestle:Relation_Strength ?rs .?r trestle:relation_of ?object .?object trestle:has_fact ?f .OPTIONAL {?f trestle:valid_from ?tStart }.OPTIONAL {?f trestle:valid_to ?tEnd }.OPTIONAL {?f trestle:valid_at ?tAt }.?f trestle:database_from ?df .OPTIONAL {?f trestle:database_to ?dt }.?f ogc:asWKT ?wkt .FILTER(?rs >= \"0.0\"^^xsd:double) .FILTER(?df <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime && (!bound(?dt) || ?dt > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) .FILTER(((!bound(?tStart) || ?tStart <= \"2014-01-01T00:00:00Z\"^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > \"2014-01-01T00:00:00Z\"^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, \"POINT (39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

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
        qb = new OracleQueryBuilder(pm);
    }

    @Test
    public void testOracleSpatial() throws UnsupportedFeatureException {
        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final String wktString = "Point(39.5398864750001 -12.0671005249999)";

        assertAll(() -> {
            final String generatedOracleTS = qb.buildTemporalSpatialIntersection(gaulClass, wktString, 0.0, QueryBuilder.Units.KM, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
            assertEquals(oracleTSString, generatedOracleTS, "Oracle TS should be equal");
        },
                () -> {
                    final String generatedOracleTSConceptString = qb.buildTemporalSpatialConceptIntersection(wktString, 0.0, 0.0, OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC), OffsetDateTime.of(LocalDate.of(2014, 1, 1).atStartOfDay(), ZoneOffset.UTC));
                    assertEquals(tsOracleConceptString, generatedOracleTSConceptString, "TS Concept intersection be equal");
                });
    }
}
