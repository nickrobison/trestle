package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.exceptions.UnsupportedFeatureException;
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
//        ps.setIri("i", getFullIRIString(individual));
//        ps.setLiteral("i", String.format("<%s>", getFullIRIString(individual)));
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

    public String buildSpatialIntersection(DIALECT dialect, OWLClass datasetClass, String wktValue, double buffer, UNITS unit) throws UnsupportedFeatureException {
        final ParameterizedSparqlString ps = buildBaseString();
//        ps.setCommandText("SELECT ?m ?wkt" +
        ps.setCommandText("SELECT ?m" +
                " WHERE { " +
                "?m rdf:type ?type ." +
                "?m ogc:asWKT ?wkt ");
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

    private ParameterizedSparqlString buildBaseString() {
        final ParameterizedSparqlString ps = new ParameterizedSparqlString();
        ps.setBaseUri(baseURI);
        ps.setNsPrefixes(this.trimmedPrefixMap);
        return ps;
    }

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
            logger.error("Cannot simplify wkt", e);
            return null;
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
