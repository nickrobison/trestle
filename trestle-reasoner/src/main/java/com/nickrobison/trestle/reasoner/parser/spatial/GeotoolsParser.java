package com.nickrobison.trestle.reasoner.parser.spatial;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Created by nrobison on 8/30/16.
 */
//TODO(nrobison): Implement
public class GeotoolsParser {
    private static final WKTReader reader = new WKTReader();
    private static final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;

    public static <T extends Geometry> String parseGeotoolsToWKT(T spatialObject) {
        return spatialObject.toString();
    }

    public static Object wktToGeotoolsObject(String wkt, Class<?> geotoolsClass) throws ParseException, TransformException {
        final com.vividsolutions.jts.geom.Geometry read = reader.read(wkt);
        return geotoolsClass.cast(JTS.toGeographic(read, crs));
    }
}
