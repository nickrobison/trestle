package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.apache.jena.query.ParameterizedSparqlString;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class VirtuosoQueryBuilder extends QueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(VirtuosoQueryBuilder.class);

    public VirtuosoQueryBuilder(DefaultPrefixManager pm) {
        super(Dialect.VIRTUOSO, pm);
    }

    @Override
    protected void buildDatabaseTSString(ParameterizedSparqlString ps, String wktValue, double buffer, OffsetDateTime atTime, OffsetDateTime dbAtTime) throws UnsupportedFeatureException {
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");

        logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
        ps.append("FILTER(((!bound(?tStart) || ?tStart <= ?startVariable^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > ?endVariable^^xsd:dateTime)) && bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
        ps.setLiteral("distance", buffer);

        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("dbAt", dbAtTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Override
    protected void buildDatabaseSString(ParameterizedSparqlString ps, String wktValue, double buffer, OffsetDateTime dbAt) throws UnsupportedFeatureException {
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");

        logger.warn("Unit conversion not implemented yet, assuming meters as base distance");
        ps.append("FILTER(bif:st_intersects(?wkt, ?wktString^^ogc:wktLiteral, ?distance)) }");
        ps.setLiteral("distance", buffer);


        ps.setLiteral("dbAt", dbAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

//        We need to simplify the WKT to get under the 4000 character SQL limit.
        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00, buffer));

    }
}
