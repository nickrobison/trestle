package com.nickrobison.trestle.querybuilder;

import com.nickrobison.trestle.exceptions.UnsupportedFeatureException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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
        ps.setCommandText("SELECT ?f" +
                " WHERE" +
                " { ?m rdf:type ?t ." +
                "?m :has_relation ?r ." +
                "?r rdf:type :Concept_Relation ." +
                "?r :Relation_Strength ?s ." +
                "?r :has_relation ?f ." +
                "?f rdf:type ?t " +
                "FILTER(?m = ?i && ?s >= ?st)}");
//        Replace the variables
        ps.setIri("i", getFullIRIString(individual));
        if (datasetClass != null ) {
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
        ps.setCommandText("SELECT ?m ?wkt" +
                " WHERE" +
                " { ?m rdf:type ?type ." +
                "?m ogc:asWKT ?wkt ");
        switch (dialect) {
            case ORACLE: {
                if (buffer > 0) {
                    ps.append("FILTER(ogcf:sfIntersects(ogcf:buffer(?wkt, ?distance, ?unitIRI), ?wktString^^ogc:wktLiteral)) }");
                    ps.setLiteral("distance", buffer);
                    ps.setIri("unitIRI", String.format("http://xmlns.oracle.com/rdf/geo/uom/%s", unit.toString()));
                } else {
                    ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");
                }
                break;
            }
            case VIRTUOSO: {
                logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
                ps.append("FILTER(bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
                ps.setLiteral("distance", buffer);
                break;
            }

            default: throw new UnsupportedFeatureException(String.format("Trestle doesn't yet support spatial queries on %s", dialect));
        }
        ps.setIri("type", getFullIRIString(datasetClass));
        ps.setLiteral("wktString", wktValue);

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

    private String getFullIRIString (OWLNamedObject object) {
        return getFullIRI(object.getIRI()).toString();
    }
}
