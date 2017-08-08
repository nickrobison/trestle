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
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 8/11/16.
 */
public class QueryBuilder {

    private static final double OFFSET = 0.01;
    private static final int SCALE = 100000;

    public enum Dialect {
        ORACLE,
        VIRTUOSO,
        STARDOG,
        JENA,
        SESAME
    }

    public enum Units {
        KM,
        MILE,
        METER,
        CM,
    }

    private static final Logger logger = LoggerFactory.getLogger(QueryBuilder.class);
    private final Dialect dialect;
    private final String prefixes;
    private final DefaultPrefixManager pm;
    private final String baseURI;
    private final Map<String, String> trimmedPrefixMap;
    private static final WKTReader reader = new WKTReader();
    private static final WKTWriter writer = new WKTWriter();

    public QueryBuilder(Dialect dialect, DefaultPrefixManager pm) {
        this.dialect = dialect;
        trimmedPrefixMap = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        final Set<Map.Entry<@KeyFor("pm.getPrefixName2PrefixMap()") String, String>> entries = pm.getPrefixName2PrefixMap().entrySet();
//        From the given prefix manager, extract the prefixes and build the prefix String
        for (Map.Entry<@KeyFor("pm.getPrefixName2PrefixMap()") String, String> entry : entries) {
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

    /**
     * Returns the {@link Dialect} currently configured
     *
     * @return - {@link Dialect}
     */
    public Dialect getDialect() {
        return this.dialect;
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
     *
     * @param individual           - OWLNamedIndividual
     * @param conceptFilter        - Nullable OWLNamedIndividual of Trestle_Concept to filter on
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
        if (dialect == Dialect.VIRTUOSO) {
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

    /**
     * Retrieve fact axioms for a given set of {@link OWLNamedIndividual}.
     * Optionally, can provide a list of {@link OWLDataProperty} to return, as a subset of all available facts
     *
     * @param validTemporal          - {@link OffsetDateTime} of valid time, to filter results on
     * @param databaseTemporal       - {@link OffsetDateTime} of database time, to filter results on
     * @param filterTemporals        - filter temporal assertions from the resultset?
     * @param filteredFactProperties - Optional filtered list of {@link OWLDataProperty} facts to return
     * @param individual             - {@link OWLNamedIndividual} to retrieve results for  @return - SPARQL query string
     */
    public String buildObjectFactRetrievalQuery(OffsetDateTime validTemporal, OffsetDateTime databaseTemporal, boolean filterTemporals, @Nullable List<OWLDataProperty> filteredFactProperties, OWLNamedIndividual... individual) {
        final ParameterizedSparqlString ps = buildBaseString();

        final String individualValues = Arrays.stream(individual)
                .map(this::getFullIRIString)
                .map(ind -> String.format("<%s>", ind))
                .collect(Collectors.joining(" "));

//        Jena won't expand URIs in the FILTER operator, so we need to give it the fully expanded value.
//        But we can't do it through the normal routes, because then it'll insert superfluous '"' values. Because, of course.
//        If the start temporal is null, pull the currently valid property
        ps.setCommandText(String.format("SELECT DISTINCT ?individual ?fact ?property ?object ?df ?dt ?vf ?vt ?va" +
                " WHERE" +
                " { ?individual trestle:has_fact ?fact ." +
//                "?fact trestle:database_time ?d ." +
                "{?fact trestle:database_from ?df} ." +
                "OPTIONAL{?fact trestle:database_to ?dt} ." +
//                "?fact trestle:valid_time ?v ." +
                "OPTIONAL{?fact trestle:valid_from ?vf} ." +
                "OPTIONAL{?fact trestle:valid_to ?vt} ." +
                "OPTIONAL{?fact trestle:valid_at ?va} ." +
                "?fact ?property ?object ." +
                "VALUES ?individual { %s } ." +
//                Oracle doesn't support isLiteral() on CLOB types, so we have to do this gross inverse filter.
                "FILTER(!isURI(?object) && !isBlank(?object)) ." +
                "FILTER(!bound(?tEnd)) .", individualValues));
        if (filteredFactProperties != null && !filteredFactProperties.isEmpty()) {
            final String factValues = filteredFactProperties
                    .stream()
                    .map(fact -> fact.getIRI().toString())
                    .map(iriSTring -> String.format("<%s>", iriSTring))
                    .collect(Collectors.joining(" "));
            ps.append(String.format("VALUES ?property { %s } .", factValues));
        }
//        If the temporals are specified, constrain with them. Otherwise, find the facts with the unbound ending temporals
        ps.append("FILTER((!bound(?vf) || ?vf <= ?validVariable^^xsd:dateTime) && (!bound(?vt) || ?vt > ?validVariable^^xsd:dateTime)) .");
        ps.append("FILTER(!bound(?va) || ?va = ?validVariable^^xsd:dateTime) .");
        ps.setLiteral("validVariable", validTemporal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.append("FILTER((!bound(?df) || ?df <= ?databaseVariable^^xsd:dateTime) && (!bound(?dt) || ?dt > ?databaseVariable^^xsd:dateTime)) .");
        ps.setLiteral("databaseVariable", databaseTemporal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        if (filterTemporals) {
            ps.append(" FILTER NOT EXISTS {?property rdfs:subPropertyOf trestle:Temporal_Property}");
        } else {
            ps.append(" FILTER EXISTS {?property rdfs:subPropertyOf trestle:Temporal_Property}");
        }
        ps.append("}");
        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL query to return all temporal/spatial relations for a given individual
     *
     * @param individual - {@link OWLNamedIndividual} to retrieve relations for
     * @return - SPARQL query string (?m - Individual, ?o - Object, ?p Property)
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
                "} " +
                "UNION { ?m ?o ?p . ?o rdfs:subPropertyOf trestle:Event_Relation ." +
                " ?p rdf:type trestle:Trestle_Object} . " +
                "VALUES ?m {<%s>}}", getFullIRIString(individual)));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL query to return all TrestleEvents (with their corresponding properties) for a given individual
     *
     * @param individual - {@link OWLNamedIndividual} to query
     * @return - SPARQL query string (?r - Event Individual, ?type - Event Type (IRI), ?t - at Temporal)
     */
    public String buildIndividualEventQuery(OWLNamedIndividual individual) {
        final ParameterizedSparqlString ps = buildBaseString();

        ps.setCommandText(String.format("SELECT DISTINCT ?r ?type ?t" +
                " WHERE { ?m rdf:type trestle:Trestle_Object ." +
                "?m trestle:Event_Relation ?r ." +
                "?r rdf:type ?type ." +
                "?type rdfs:subClassOf trestle:Trestle_Event ." +
                "?r trestle:exists_at ?t ." +
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

        ps.setCommandText(String.format("SELECT DISTINCT ?individual ?property ?object" +
                " WHERE {" +
//                " { ?individual trestle:exists_time ?temporal ." +
                " OPTIONAL{?individual trestle:exists_at ?tAt} ." +
                " OPTIONAL{?individual trestle:exists_from ?tStart} ." +
                " OPTIONAL{?individual trestle:exists_to ?tEnd} ." +
                " ?individual ?property ?object" +
                " VALUES ?individual { %s } ." +
                " FILTER(!isURI(?object) && !isBlank(?object)) .}", individualValues));
        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Retrieve all Fact values for a given individual
     * If a temporal range is provided, the returned values will be valid within that range
     * If a database temporal value is provided, only db versions beyond that point will be returned
     *
     * @param individual - OWLNamedIndividual to get facts from
     * @param property   - OWLDataProperty values to retrieve
     * @param validStart - Optional start of fact value, temporal filter
     * @param validEnd   - Optional end of fact value, temporal filter
     * @param dbTemporal - Optional database temporal filter
     * @return - SPARQL Query string
     */
    public String buildFactHistoryQuery(OWLNamedIndividual individual, OWLDataProperty property, @Nullable OffsetDateTime validStart, @Nullable OffsetDateTime validEnd, @Nullable OffsetDateTime dbTemporal) {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("SELECT ?value " +
                "WHERE { " +
                "?m trestle:has_fact ?f ." +
                "{?f trestle:database_from ?df} ." +
                "OPTIONAL{?f trestle:database_to ?dt} ." +
                "OPTIONAL{?f trestle:valid_from ?vf } ." +
                "OPTIONAL{?f trestle:valid_at ?va } ." +
                "OPTIONAL{?f trestle:valid_to ?vt }. " +
                "?f <%s> ?value ." +
                "VALUES ?m {<%s>} .", getFullIRIString(property), getFullIRIString(individual)));
        if (validStart != null) {
            if (validEnd != null) {
//                If we have both a start and end valid interval, find all the values that are valid between the points
                ps.append("FILTER ((!bound(?vf) || " +
                        "(?vf >= ?validStart^^xsd:dateTime && ?vf < ?validEnd^^xsd:dateTime) && " +
                        "(!bound(?vt) || " +
                        "?vt > ?validEnd^^xsd:dateTime)) && " +
                        "(!bound(?va) || " +
                        "(?va >= ?validStart^^xsd:dateTime && " +
                        "?va < ?validEnd^^xsd:dateTime))) .");
                ps.setLiteral("validStart", validStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                ps.setLiteral("validEnd", validEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else {
                ps.append("FILTER ((!bound(?vf) || " +
                        "(?vf >= ?validStart^^xsd:dateTime && ?vf ?validEnd^^xsd:dateTime)) && " +
                        "(!bound(?va) || " +
                        "(?va >= ?validStart^^xsd:dateTime && " +
                        "?va < ?validEnd^^xsd:dateTime))) .");
                ps.setLiteral("validStart", validStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        }

        if (dbTemporal != null) {
            ps.append("FILTER(?df >= ?dbAt^^xsd:dateTime) .");
            ps.setLiteral("dbAt", dbTemporal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        ps.append("}");
        logger.debug(ps.toString());
        return ps.toString();
    }

    @Deprecated
    public String buildSpatialIntersection(OWLClass datasetClass, String wktValue, double buffer, Units unit) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m trestle:has_fact ?f ." +
                "?f ogc:asWKT ?wkt ");
        ps.setIri("type", getFullIRIString(datasetClass));
        buildDatabaseSString(ps, wktValue, buffer, OffsetDateTime.now());

        logger.debug(ps.toString());
        return ps.toString();
    }

    //    FIXME(nrobison): This needs to account for exists and valid times.
    public String buildTemporalSpatialIntersection(OWLClass datasetClass, String wktValue, double buffer, Units unit, OffsetDateTime atTime, OffsetDateTime dbAtTime) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m ?tStart ?tEnd" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m trestle:has_fact ?f ." +
                "?f ogc:asWKT ?wkt ." +
//                "?f trestle:valid_time ?t ." +
                "OPTIONAL{?f trestle:valid_from ?tStart} ." +
                "OPTIONAL{?f trestle:valid_to ?tEnd} ." +
                "OPTIONAL{?f trestle:valid_at ?tAt} ." +
                "?f trestle:database_from ?df ." +
                "OPTIONAL{?f trestle:database_to ?dt} .");
        buildDatabaseTSString(ps, wktValue, buffer, atTime, dbAtTime);
        ps.setIri("type", getFullIRIString(datasetClass));
//        We need to simplify the WKT to get under the 4000 character SQL limit.
//        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL query to find the concepts that intersect a given space/time pair
     * If a validAt temporal is given, intersect at that point in time, otherwise, find anything that intersects, ever
     *
     * @param wktValue - WKT value
     * @param buffer   - buffer value (in meters)
     * @param atTime   - Temporal to select appropriate, valid fact
     * @param dbAtTime - Temporal to select currently valid version of the fact
     * @return - String of SPARQL query
     * @throws UnsupportedFeatureException - Throws if we're running on a database that doesn't support all the features
     */
    // TODO(nrobison): Why does this throw? It'll never need to be caught
    public String buildTemporalSpatialConceptIntersection(String wktValue, double buffer, @Nullable OffsetDateTime atTime, OffsetDateTime dbAtTime) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText("SELECT DISTINCT ?m" +
                " WHERE { " +
                "?m rdf:type trestle:Trestle_Concept ." +
                "?m trestle:related_by ?r ." +
                "?r trestle:relation_of ?object ." +
                "?object trestle:has_fact ?f ." +
                "OPTIONAL {?f trestle:valid_from ?tStart }." +
                "OPTIONAL {?f trestle:valid_to ?tEnd }." +
                "OPTIONAL {?f trestle:valid_at ?tAt }." +
                "?f trestle:database_from ?df ." +
                "OPTIONAL {?f trestle:database_to ?dt }." +
                "?f ogc:asWKT ?wkt .");

        if (atTime == null) {
            this.buildDatabaseSString(ps, wktValue, buffer, dbAtTime);
        } else {
            this.buildDatabaseTSString(ps, wktValue, buffer, atTime, dbAtTime);
        }

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Build SPARQL Query to retrieve all given members of a Trestle_Concept that are subclassed from the given OWLClass
     *
     * @param datasetClass OWLClass of individuals to return
     * @param conceptID    - IRI of Trestle_Concept to query
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
     *
     * @param ps       - ParamaterizedSparqlString to build on
     * @param wktValue - ParamaterizedSparqlString to build on
     * @param buffer   - double buffer (in meters) around the intersection
     * @param atTime   - OffsetDateTime to set intersection time to
     * @param dbAtTime
     * @throws UnsupportedFeatureException
     */
    private void buildDatabaseTSString(ParameterizedSparqlString ps, String wktValue, double buffer, OffsetDateTime atTime, OffsetDateTime dbAtTime) throws UnsupportedFeatureException {
//        Add DB intersection
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");
        switch (this.dialect) {
            case ORACLE: {
//                We need to remove this, otherwise Oracle substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
                ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
                ps.append("FILTER(((!bound(?tStart) || ?tStart <= ?startVariable^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > ?endVariable^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
//                ps.append("FILTER((?tStart < ?startVariable^^xsd:dateTime && ?tEnd >= ?endVariable^^xsd:dateTime) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }
            case VIRTUOSO: {
                logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
                ps.append("FILTER(((!bound(?tStart) || ?tStart <= ?startVariable^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > ?endVariable^^xsd:dateTime)) && bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
                ps.setLiteral("distance", buffer);
                break;
            }
            case SESAME: {
//                We need to remove this, otherwise GraphDB substitutes geosparql for ogc
                ps.removeNsPrefix("geosparql");
                ps.append("FILTER(((!bound(?tStart) || ?tStart <= ?startVariable^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > ?endVariable^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }

        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("dbAt", dbAtTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Common method for build the Spatial intersection component of the SPARQL query
     *
     * @param ps       - ParamaterizedSparqlString to build on
     * @param wktValue - ParamaterizedSparqlString to build on
     * @param buffer   - double buffer (in meters) around the intersection
     * @throws UnsupportedFeatureException
     */
    private void buildDatabaseSString(ParameterizedSparqlString ps, String wktValue, double buffer, OffsetDateTime dbAt) throws UnsupportedFeatureException {
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");
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
            }
            case SESAME: {
                ps.removeNsPrefix("geosparql");
                ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                break;
            }

            default:
                throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }
        ps.setLiteral("dbAt", dbAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

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
     * Update the ending value of a database interval, provided the interval is continuing
     *
     * @param temporal   - {@link OffsetDateTime} of value to use
     * @param individual - {@link OWLNamedIndividual} of Interval_Object to update
     * @return - SPARQL Query String
     */
    public String buildUpdateUnboundedTemporal(OffsetDateTime temporal, OWLNamedIndividual... individual) {
        final String individualValues = Arrays.stream(individual)
                .map(this::getFullIRIString)
                .map(ind -> String.format("<%s>", ind))
                .collect(Collectors.joining(" "));

        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("INSERT {" +
                "?m trestle:database_to ?newValue^^xsd:dateTime} " +
                " WHERE { " +
                "VALUES ?m {%s} . " +
                "OPTIONAL{?m trestle:database_to ?dt} . " +
                "?m rdf:type trestle:Interval_Object ." +
                "FILTER(!bound(?dt))}", individualValues));

        ps.setLiteral("newValue", temporal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Update fact ending temporal to new value
     * Provided the currently valid fact has a continuing interval
     *
     * @param individual - {@link OWLNamedIndividual} of Interval_Object to update
     * @param property   - {@link OWLNamedIndividual} of Fact
     * @param temporal   - {@link OffsetDateTime} of value to use
     * @return - SPARQL Query String
     */
    public String updateUnboundedFact(OWLNamedIndividual individual, OWLDataProperty property, OffsetDateTime temporal) {
        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("INSERT {" +
                "?f trestle:valid_to ?newValue^^xsd:dateTime} " +
                "WHERE { " +
                "?m trestle:has_fact ?f ." +
                "OPTIONAL{?f trestle:valid_to ?vt }. " +
                "?f rdf:type trestle:Interval_Object ." +
                "?f <%s> ?value ." +
                "FILTER(?m = <%s> && !bound(?vt)) }", getFullIRIString(property), getFullIRIString(individual)));

        ps.setLiteral("newValue", temporal.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Takes a list of {@link OWLDataPropertyAssertionAxiom}s and REPLACES any existing values
     *
     * @param axioms - Axioms to add to object
     * @param typeRestriction - Optional {@link IRI} which specifies an
     * @return - SPARQL query string
     */
    public String updateObjectProperties(List<OWLDataPropertyAssertionAxiom> axioms, @Nullable IRI typeRestriction) {

//        Restrict query to specific object class?
        final IRI restrictionIRI;
        if (typeRestriction == null) {
            restrictionIRI = OWLRDFVocabulary.OWL_NAMED_INDIVIDUAL.getIRI();
        } else {
            restrictionIRI = typeRestriction;
        }

//        Find the data properties to delete all values of

        final String deleteAxioms = axioms
                .stream()
                .map(axiom -> axiom.getProperty().asOWLDataProperty())
                .map(property -> String.format("?m <%s> ?o", property.getIRI().toString()))
                .collect(Collectors.joining(". "));

        final String updateAxioms = axioms
                .stream()
                .map(axiom -> String.format("%s %s %s", axiom.getSubject().toString(), axiom.getProperty().asOWLDataProperty().toString(), axiom.getObject().toString()))
                .collect(Collectors.joining(". "));

        final String filterAxiom = axioms
                .stream()
                .map(axiom -> axiom.getSubject().toString())
                .collect(Collectors.joining(", "));


        final ParameterizedSparqlString ps = buildBaseString();
        ps.setCommandText(String.format("DELETE {" +
                "%s }" +
                "INSERT {" +
                "%s }" +
                "WHERE {" +
                "?m rdf:type <%s> ." +
                "VALUES ?m {%s} ." +
                "?m ?p ?o" +
                "}", deleteAxioms, updateAxioms, restrictionIRI.getIRIString(), filterAxiom));

        logger.debug(ps.toString());
        return ps.toString();
    }

    /**
     * Initialize the base SPARQL String, with the correct prefixes
     *
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
     *
     * @param wkt    - WKT string to simplify
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
