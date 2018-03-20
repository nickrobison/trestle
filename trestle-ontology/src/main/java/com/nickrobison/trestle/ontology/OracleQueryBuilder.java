package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.apache.jena.query.ParameterizedSparqlString;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OracleQueryBuilder extends QueryBuilder {

    public OracleQueryBuilder(DefaultPrefixManager pm) {
        super(Dialect.ORACLE, pm);
    }

    @Override
    protected void buildDatabaseTSString(ParameterizedSparqlString ps, String wktValue, OffsetDateTime atTime, OffsetDateTime dbAtTime) {
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");

        //                We need to remove this, otherwise Oracle substitutes geosparql for ogc
        ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
        ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
        ps.append("FILTER(((!bound(?tStart) || ?tStart <= ?startVariable^^xsd:dateTime) && (!bound(?tEnd) || ?tEnd > ?endVariable^^xsd:dateTime)) && ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");

        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00));
        ps.setLiteral("startVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("endVariable", atTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        ps.setLiteral("dbAt", dbAtTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Override
    protected void buildDatabaseSString(ParameterizedSparqlString ps, String wktValue, OffsetDateTime dbAt) {
        ps.append("FILTER(?df <= ?dbAt^^xsd:dateTime && (!bound(?dt) || ?dt > ?dbAt^^xsd:dateTime)) .");

        //                We need to remove this, otherwise Oracle substitutes geosparql for ogc
        ps.removeNsPrefix("geosparql");
//                Add this hint to the query planner
        ps.setNsPrefix("ORACLE_SEM_HT_NS", "http://oracle.com/semtech#leading(?wkt)");
        ps.append("FILTER(ogcf:sfIntersects(?wkt, ?wktString^^ogc:wktLiteral)) }");

        ps.setLiteral("dbAt", dbAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

//        We need to simplify the WKT to get under the 4000 character SQL limit.
        ps.setLiteral("wktString", simplifyWkt(wktValue, 0.00));
    }
}
