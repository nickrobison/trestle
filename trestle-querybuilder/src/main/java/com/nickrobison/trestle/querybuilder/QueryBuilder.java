package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.apache.jena.query.ParameterizedSparqlString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.PREFIX;

/**
 * Created by nrobison on 8/11/16.
 */
public class QueryBuilder {

    public static final double OFFSET = 0.01;
    public static final int SCALE = 100000;

    public enum DIALECT {
        ORACLE,
        VIRTUOSO,
        STARDOG,
        JENA,
        SESAME
    }

    public enum UNITS {
        KM,
        MILE,
        METER,
        CM,
    }

    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
    private final String prefixes;
    private final DefaultPrefixManager pm;
    private final String baseURI;
    private final Map<String, String> trimmedPrefixMap;
    private static final WKTReader reader = new WKTReader();
    private static final WKTWriter writer = new WKTWriter();

    public QueryBuilder(DefaultPrefixManager pm) {
        trimmedPrefixMap = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        final Set<Map.Entry<String, String>> entries = pm.getPrefixName2PrefixMap().entrySet();
//        From the given prefix manager, extract the prefixes and build the prefix String
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals(":")) {
//                continue;
                trimmedPrefixMap.put("", entry.getValue());
            }
            trimmedPrefixMap.put(entry.getKey().replace(":", ""), entry.getValue());
            builder.append(String.format("PREFIX %s : <%s>\n", entry.getKey().replace(":", ""), entry.getValue()));
        }
        this.prefixes = builder.toString();
        final String defaultPrefix = pm.getDefaultPrefix();
//        Because the madness of nulls
        if (defaultPrefix != null) {
            final String prefix = pm.getPrefix(defaultPrefix);
            if (prefix != null) {
                baseURI = prefix;
            } else {
                baseURI = PREFIX;
            }
        } else {
            this.baseURI = PREFIX;
        }
        this.pm = pm;
    }

    public String buildRelationQuery(OWLNamedIndividual individual, @Nullable OWLClass datasetClass, double relationshipStrength) {
        final ParameterizedSparqlString ps = buildBaseString();
//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
        ps.setCommandText(String.format("SELECT ?f ?s" +
                " WHERE" +
                " { ?m rdf:type ?t . " +
                "?m :has_relation ?r . " +
                "?r rdf:type :Concept_Relation . " +
                "?r :Relation_Strength ?s . " +
                "?r :has_relation ?f . " +
                "?f rdf:type ?t " +
                "FILTER(?m = %s && ?s >= ?st)}", String.format("<%s>", getFullIRIString(individual))));
//        Replace the variables
        if (datasetClass != null) {
            ps.setIri("t", getFullIRIString(datasetClass));
        } else {
            ps.setIri("t", getFullIRI(IRI.create("trestle:", ":Dataset")).toString());
        }
        ps.setLiteral("st", relationshipStrength);

//        return ps.toString().replace("<", "").replace(">", "");
        logger.debug(ps.toString());
        return ps.toString();
    }

    @SuppressWarnings("Duplicates")
    public String buildObjectPropertyRetrievalQuery(OWLNamedIndividual individual, @Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal) {
        final ParameterizedSparqlString ps = buildBaseString();

//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
//        If the start temporal is null, pull the currently valid property
//        FIXME(nrobison): The union is a horrible hack to get things working for the time being. We need to fix it.
        ps.setCommandText(String.format("SELECT DISTINCT ?f" +
                " WHERE" +
                " { ?m :has_fact ?f ." +
                "?f :database_time ?d ." +
                "{ ?d :valid_from ?tStart} UNION {?d :exists_from ?tStart} ." +
                "OPTIONAL{{ ?d :valid_to ?tEnd} UNION {?d :exists_to ?tEnd}} ."));
        if (startTemporal == null) {
            ps.append(String.format("FILTER(?m = %s && !bound(?tEnd))}", String.format("<%s>", getFullIRIString(individual))));
//            Otherwise, we'll find the correct property that satisfies the temporal interval
        } else {
            ps.append(String.format("FILTER(?m = %s && (?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime))}", String.format("<%s>", getFullIRIString(individual))));
            ps.setLiteral("startVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (endTemporal != null) {
                ps.setLiteral("endVariable", endTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                ps.setLiteral("endVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        logger.debug(ps.toString());
        return ps.toString();
    }

    @SuppressWarnings("Duplicates")
    public String buildObjectPropertyRetrievalQueryOptimized(OWLNamedIndividual individual, @Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal) {
        final ParameterizedSparqlString ps = buildBaseString();

//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
//        If the start temporal is null, pull the currently valid property
//        FIXME(nrobison): The union is a horrible hack to get things working for the time being. We need to fix it.
        ps.setCommandText(String.format("SELECT DISTINCT ?fact ?property ?object" +
                " WHERE" +
                " { ?m :has_fact ?fact ." +
                "?fact :database_time ?d ." +
                "{ ?d :valid_from ?tStart} UNION {?d :exists_from ?tStart} ." +
                "OPTIONAL{{ ?d :valid_to ?tEnd} UNION {?d :exists_to ?tEnd}} ." +
                "?fact ?property ?object ."));
        if (startTemporal == null) {
            ps.append(String.format("FILTER(datatype(?object) != '' && ?m = %s && !bound(?tEnd))}", String.format("<%s>", getFullIRIString(individual))));
//            Otherwise, we'll find the correct property that satisfies the temporal interval
        } else {
            ps.append(String.format("FILTER(datatype(?object) != '' && ?m = %s && (?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime))}", String.format("<%s>", getFullIRIString(individual))));
            ps.setLiteral("startVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (endTemporal != null) {
                ps.setLiteral("endVariable", endTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                ps.setLiteral("endVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        logger.debug(ps.toString());
        return ps.toString();
    }

    public String buildSpatialIntersection(DIALECT dialect, OWLClass datasetClass, String wktValue, double buffer, UNITS unit) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m :has_fact ?f ." +
                "?f ogc:asWKT ?wkt ");
        switch (dialect) {
            case ORACLE: {
//                We need to remove this, otherwise Oracle substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
                ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
//                TODO(nrobison): Fix this, gross
                ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }
            case VIRTUOSO: {
                logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
                ps.append("FILTER(bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
                ps.setLiteral("distance", buffer);
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }
        ps.setIri("type", getFullIRIString(datasetClass));
//        We need to simplify the WKT to get under the 4000 character SQL limit.
        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));

        logger.debug(ps.toString());
        return ps.toString();
    }

    public String buildTemporalSpatialIntersection(DIALECT dialect, OWLClass datasetClass, String wktValue, double buffer, UNITS unit, OffsetDateTime atTime) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m ?tStart ?tEnd" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m :has_fact ?f ." +
                "?f ogc:asWKT ?wkt ." +
                "?m :has_temporal ?t ." +
                "{ ?t :valid_from ?tStart} UNION {?t :exists_from ?tStart} ." +
                "OPTIONAL{{ ?t :valid_to ?tEnd} UNION {?t :exists_to ?tEnd}} .");
        switch (dialect) {
            case ORACLE: {
//                We need to remove this, otherwise Oracle substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
                ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
                ps.append("FILTER((?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }
            case VIRTUOSO: {
                logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
                ps.append("FILTER((?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime) && bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
                ps.setLiteral("distance", buffer);
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }
        ps.setIri("type", getFullIRIString(datasetClass));
//        We need to simplify the WKT to get under the 4000 character SQL limit.
        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Return individuals with IRIs that match the given search string
     * @param individual - String to search for matching individual
     * @param owlClass - Optional OWLClass to limit search results
     * @param limit - Optional limit of query results
     * @return - SPARQL Query string
     */
    public String buildIndividualSearchQuery(String individual, @Nullable OWLClass owlClass, @Nullable Integer limit) {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE {" +
                "?m rdf:type ?type ." +
                "FILTER (contains(lcase(str(?m)), ?string))} LIMIT ?limit");

        if (owlClass == null) {
//            We need to get the fully expanded Prefix, otherwise Jena won't expanded it properly and give us an <> IRI, which will fail.
            ps.setIri("type", IRI.create(PREFIX, "Dataset").toString());
        } else {
            ps.setIri("type", getFullIRIString(owlClass));
        }
        ps.setLiteral("string", individual);

//        Set the limit
        if (limit == null) {
            ps.setLiteral("limit", 10);
        } else {
            ps.setLiteral("limit", limit);
        }


        logger.debug(ps.toString());
        return ps.toString();
    }

    private ParameterizedSparqlString buildBaseString() {
        final ParameterizedSparqlString ps = new ParameterizedSparqlString();
        ps.setBaseUri(baseURI);
        ps.setNsPrefixes(this.trimmedPrefixMap);
        return ps;
    }
// TODO(nrobison): Move this to trestle-common
    private IRI getFullIRI(IRI iri) {
        //        Check to see if it's already been expanded
        if (pm.getPrefix(iri.getScheme() + ":") == null) {
            return iri;
        } else {
            return pm.getIRI(iri.toString());
        }
    }

    private String getFullIRIString(OWLNamedObject object) {
        return getFullIRI(object.getIRI()).toString();
    }

    private static String simplifyWkt(String wkt, double factor, double buffer) {

        final Geometry geom;
        try {
            geom = reader.read(wkt);
        } catch (ParseException e) {
            logger.error("Cannot read wkt into geom", e);
            return wkt;
        }

//        If needed, add a buffer
        if (buffer > 0.0) {
            geom.buffer(buffer);
        }


        if (wkt.length() < 3000) {
            return writer.write(geom);
        }

        if (factor == 0.0) {
//            Reduce precision to 5 decimal points.
            logger.warn("String too long, reducing precision to {}", SCALE);
            return simplifyWkt(writer.write(GeometryPrecisionReducer.reduce(geom,
                    new PrecisionModel(SCALE))),
                    factor + OFFSET,
                    0);
        }

        logger.warn("String too long, simplifying with {}", factor);

        final Geometry simplified = TopologyPreservingSimplifier.simplify(geom, factor);
        return simplifyWkt(writer.write(simplified), factor + OFFSET, 0);
    }
}
