package com.nickrobison.trestle.parser.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Created by nrobison on 8/30/16.
 */
public class JTSParser {
    //    TODO(nrobison): Implement better JTS handling
    public static String parseJTSToWKT(Object jtsObject) {
        final Geometry jtsGeom = Geometry.class.cast(jtsObject);
        return new WKTWriter().write(jtsGeom);
    }

    //    TODO(nrobison): Implement better JTS handling
    public static Object wktToJTSObject(String wkt, Class<?> jtsClass) throws ParseException {
        return new WKTReader().read(wkt);
    }
}
