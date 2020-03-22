package com.nickrobison.trestle.reasoner.parser.spatial;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

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
