package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.sparql.util.NodeUtils;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

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
    private final DIALECT dialect;
    private final String prefixes;
    private final DefaultPrefixManager pm;
    private final String baseURI;
    private final Map<String, String> trimmedPrefixMap;
    private static final WKTReader reader = new WKTReader();
    private static final WKTWriter writer = new WKTWriter();

    public QueryBuilder(DIALECT dialect, DefaultPrefixManager pm) {
        this.dialect = dialect;
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
            builder.append(String.format("TRESTLE_PREFIX %s : <%s>\n", entry.getKey().replace(":", ""), entry.getValue()));
        }
        this.prefixes = builder.toString();
        final String defaultPrefix = pm.getDefaultPrefix();
//        Because the madness of nulls
        if (defaultPrefix != null) {
            final String prefix = pm.getPrefix(defaultPrefix);
            if (prefix != null) {
                baseURI = prefix;
            } else {
                baseURI = TRESTLE_PREFIX;
            }
        } else {
            this.baseURI = TRESTLE_PREFIX;
        }
        this.pm = pm;
    }

    public String buildDatasetQuery() {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT ?dataset" +
                " WHERE { ?dataset rdfs:subClassOf trestle:Dataset }");
        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build the SPARQL query to return the concepts and individual IRIs related to the given OWLNamedIndividual
     * If the conceptFilter is specified, results are filtered to only return that concept, if the individual is a member of that concept
     * @param individual - OWLNamedIndividual
     * @param conceptFilter - Nullable OWLNamedIndividual of Trestle_Concept to filter on
     * @param relationshipStrength - double of cutoff value of minimum relation strength to consider an individual a member of that concept
     * @return - SPARQL Query with variables ?concept ?individual
     */
    public String buildConceptRetrievalQuery(OWLNamedIndividual individual, @Nullable OWLNamedIndividual conceptFilter, double relationshipStrength) {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("SELECT DISTINCT ?concept ?individual" +
                " WHERE " +
                "{ ?i trestle:has_relation ?r ." +
                "?r trestle:Relation_Strength ?strength ." +
                "?r trestle:related_to ?concept ." +
                "?concept trestle:related_by ?rc ." +
                "?rc trestle:Relation_Strength ?strength ." +
                "?rc trestle:relation_of ?individual ." +
                "VALUES ?i {%s} ." +
                "FILTER(?strength >= ?st)", String.format("<%s>", getFullIRIString(individual))));
        if (conceptFilter != null) {
            ps.append(String.format(". VALUES ?concept {%s}", String.format("<%s>", getFullIRIString(conceptFilter))));
        }
        ps.append("}");
        ps.setLiteral("st", relationshipStrength);
        logger.debug(ps.toString());
        return ps.toString();
    }

    @Deprecated
    public String buildRelationQuery(OWLNamedIndividual individual, @Nullable OWLClass datasetClass, double relationshipStrength) {
        final ParameterizedSparqlString ps = buildBaseString();
//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
//        Virtuoso needs to limit the transitive depth, Oracle doesn't not, and fails
        if (dialect == DIALECT.VIRTUOSO) {
            ps.setCommandText(String.format("SELECT ?f ?s" +
                    " WHERE" +
                    " { ?m rdf:type ?t . " +
                    "?m trestle:has_relation{,?depth} ?r . " +
                    "?r rdf:type trestle:Concept_Relation . " +
                    "?r trestle:Relation_Strength ?s . " +
                    "?r trestle:relation_of{,?depth} ?f . " +
                    "?f rdf:type ?t ." +
                    " VALUES ?m {%s}" +
                    "FILTER(?s >= ?st)}", String.format("<%s>", getFullIRIString(individual))));
            ps.setLiteral("depth", 1);
        } else {
            ps.setCommandText(String.format("SELECT ?f ?s" +
                    " WHERE" +
                    " { ?m rdf:type ?t . " +
                    "?m trestle:has_relation ?r . " +
                    "?r rdf:type trestle:Concept_Relation . " +
                    "?r trestle:Relation_Strength ?s . " +
                    "?r trestle:relation_of ?f . " +
                    "?f rdf:type ?t ." +
                    " VALUES ?m {%s}" +
                    "FILTER(?s >= ?st)}", String.format("<%s>", getFullIRIString(individual))));
        }
//        Replace the variables
        if (datasetClass != null) {
//            ps.setParam("t", NodeFactory.createURI(String.format(":%s", datasetClass)));
            ps.setIri("t", getFullIRIString(datasetClass));
        } else {
            ps.setIri("t", getFullIRI(IRI.create("trestle:", "Dataset")).toString());
        }
        ps.setLiteral("st", relationshipStrength);

        logger.debug(ps.toString());
        return ps.toString();
    }

    public String buildObjectPropertyRetrievalQuery(@Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal, OWLNamedIndividual... individual) {
        final ParameterizedSparqlString ps = buildBaseString();

        final String individualValues = Arrays.stream(individual)
                .map(this::getFullIRIString)
                .map(ind -> String.format("<%s>", ind))
                .collect(Collectors.joining(" "));

//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
//        If the start temporal is null, pull the currently valid property
        ps.setCommandText(String.format("SELECT DISTINCT ?individual ?fact ?property ?object" +
                " WHERE" +
                " { ?individual trestle:has_fact ?fact ." +
                "?fact trestle:database_time ?d ." +
                "{?d trestle:valid_from ?tStart} ." +
                "OPTIONAL{?d trestle:valid_to ?tEnd} ." +
                "?fact ?property ?object ." +
                "VALUES ?individual { %s } ." +
//                Oracle doesn't support isLiteral() on CLOB types, so we have to do this gross inverse filter.
                "FILTER(!isURI(?object) && !isBlank(?object)) ." +
                "FILTER(!bound(?tEnd)) .", individualValues));
        if (startTemporal != null) {
            ps.append("FILTER(?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime)");
            ps.setLiteral("startVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (endTemporal != null) {
                ps.setLiteral("endVariable", endTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                ps.setLiteral("endVariable", startTemporal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        ps.append("}");
        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL query to return all temporal/spatial relations for a given object
     * @param individual - OWLNamedIndividual to retrieve relations for
     * @return - SPARQL query string
     */
    public String buildIndividualRelationQuery(OWLNamedIndividual individual) {
        final ParameterizedSparqlString ps = buildBaseString();

        ps.setCommandText(String.format("SELECT DISTINCT ?m ?o ?p " +
                "WHERE { { " +
                "?m ?o ?p . " +
                "?o rdfs:subPropertyOf trestle:Temporal_Relation } " +
                "UNION { " +
                "?m ?o ?p . " +
                "?o rdfs:subPropertyOf trestle:Spatial_Relation ." +
                "} . " +
                "VALUES ?m {<%s>}}", getFullIRIString(individual)));

        logger.debug(ps.toString());
        return ps.toString();
    }

    public String buildIndividualTemporalQuery(OWLNamedIndividual... individual) {
        final ParameterizedSparqlString ps = buildBaseString();

        final String individualValues = Arrays.stream(individual)
                .map(this::getFullIRIString)
                .map(ind -> String.format("<%s>", ind))
                .collect(Collectors.joining(" "));

        ps.setCommandText(String.format("SELECT DISTINCT ?individual ?temporal ?property ?object" +
                " WHERE" +
                " { ?individual trestle:exists_time ?temporal ." +
                " OPTIONAL{?temporal trestle:exists_at ?tAt} ." +
                " OPTIONAL{?temporal trestle:exists_from ?tStart} ." +
                " OPTIONAL{?temporal trestle:exists_to ?tEnd} ." +
                " ?temporal ?property ?object" +
                " VALUES ?individual { %s } ." +
                " FILTER(!isURI(?object) && !isBlank(?object)) .}", individualValues));
        logger.debug(ps.toString());
        return ps.toString();
    }

    public String buildSpatialIntersection(OWLClass datasetClass, String wktValue, double buffer, UNITS unit) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m trestle:has_fact ?f ." +
                "?f ogc:asWKT ?wkt ");
        ps.setIri("type", getFullIRIString(datasetClass));
        buildDatabaseSString(ps, wktValue, buffer);

        logger.debug(ps.toString());
        return ps.toString();
    }

//    FIXME(nrobison): This needs to account for exists and valid times.
    public String buildTemporalSpatialIntersection(OWLClass datasetClass, String wktValue, double buffer, UNITS unit, OffsetDateTime atTime) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m ?tStart ?tEnd" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m trestle:has_fact ?f ." +
                "?f ogc:asWKT ?wkt ." +
                "?f trestle:valid_time ?t ." +
                "{?t trestle:start_temporal ?tStart} ." +
                "OPTIONAL{?t trestle:end_temporal ?tEnd} .");
        buildDatabaseTSString(ps, wktValue, buffer, atTime);
        ps.setIri("type", getFullIRIString(datasetClass));
//        We need to simplify the WKT to get under the 4000 character SQL limit.
//        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL query to find the concepts that intersect a given space/time pair
     * @param wktValue - WKT value
     * @param buffer - buffer value (in meters)
     * @param atTime - Temporal to select appropriate, valid fact
     * @return - String of SPARQL query
     * @throws UnsupportedFeatureException
     */
    public String buildTemporalSpatialConceptIntersection(String wktValue, double buffer, @Nullable OffsetDateTime atTime) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE { " +
                "?m rdf:type trestle:Trestle_Concept ." +
                "?m trestle:related_by ?r ." +
                "?r trestle:relation_of ?object ." +
//                "?m trestle:concept_of ?object . " +
                "?object trestle:has_fact ?f ." +
                "?f trestle:valid_time ?ft ." +
//                "?ft trestle:valid_from ?tStart ." +
//                "OPTIONAL{ ?ft trestle:valid_to ?tEnd }." +
                "?f ogc:asWKT ?wkt .");

        if (atTime != null) {
            this.buildDatabaseTSString(ps, wktValue, buffer, atTime);
        } else {
            this.buildDatabaseSString(ps, wktValue, buffer);
        }

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL Query to retrieve all given members of a Trestle_Concept that are subclassed from the given OWLClass
     * @param datasetClass OWLClass of individuals to return
     * @param conceptID - IRI of Trestle_Concept to query
     * @return - SPARQL Query String
     */
//    TODO(nrobison): Implement spatio-temporal intersection
    public String buildConceptObjectRetrieval(OWLClass datasetClass, IRI conceptID) {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("SELECT DISTINCT ?m " +
                "WHERE { " +
                "?m rdf:type ?type . " +
                "?m trestle:has_relation ?r ." +
                "?r trestle:related_to ?concept ." +
//                "?m trestle:has_concept ?concept . " +
                "VALUES ?concept { <%s> }}", getFullIRI(conceptID).toString()));
        ps.setIri("type", getFullIRIString(datasetClass));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Common method to build the spatio-temporal intersection component of the SPARQL query
     * @param ps - ParamaterizedSparqlString to build on
     * @param wktValue - ParamaterizedSparqlString to build on
     * @param buffer - double buffer (in meters) around the intersection
     * @param atTime - OffsetDateTime to set intersection time to
     * @throws UnsupportedFeatureException
     */
    private void buildDatabaseTSString(ParameterizedSparqlString ps, String wktValue, double buffer, OffsetDateTime atTime) throws UnsupportedFeatureException {
        switch (this.dialect) {
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
            } case SESAME: {
//                We need to remove this, otherwise GraphDB substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
                ps.append("FILTER((?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }

        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Common method for build the Spatial intersection component of the SPARQL query
     * @param ps - ParamaterizedSparqlString to build on
     * @param wktValue - ParamaterizedSparqlString to build on
     * @param buffer - double buffer (in meters) around the intersection
     * @throws UnsupportedFeatureException
     */
    private void buildDatabaseSString(ParameterizedSparqlString ps, String wktValue, double buffer) throws UnsupportedFeatureException {
        switch (this.dialect) {
            case ORACLE: {
//                We need to remove this, otherwise Oracle substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
                ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
                ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }
            case VIRTUOSO: {
                logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
                ps.append("FILTER(bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
                ps.setLiteral("distance", buffer);
                break;
            } case SESAME: {
                ps.removeNsPrefix("geosparql");
                ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }

//        We need to simplify the WKT to get under the 4000 character SQL limit.
        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
    }

    /**
     * Return individuals with IRIs that match the given search string
     *
     * @param individual - String to search for matching individual
     * @param owlClass   - Optional OWLClass to limit search results
     * @param limit      - Optional limit of query results
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
            ps.setIri("type", IRI.create(TRESTLE_PREFIX, "Dataset").toString());
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

    /**
     * Initialize the base SPARQL String, with the correct prefixes
     * @return - Base SPARQL string
     */
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

    /**
     * Recursively simplify the WKT string.
     * On first pass, reduce the decimal precision, before attempting simplification
     * @param wkt - WKT string to simplify
     * @param factor - Initial starting simplification factor (If 0, reduce precision first)
     * @param buffer - Buffer to add to WKT
     * @return - String of simplified WKT
     */
    private static String simplifyWkt(String wkt, double factor, double buffer) {

        final Geometry geom;
        try {
            geom = reader.read(wkt);
        } catch (ParseException e) {
            logger.error("Cannot read wkt into geom", e);
            return wkt;
        }

        if (!geom.isValid()) {
            logger.error("Invalid geometry at simplification level {}: {}", factor, geom.toString());
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
