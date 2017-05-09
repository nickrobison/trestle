package com.nickrobison.trestle.reasoner.parser.spatial;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 8/30/16.
 */
public class ESRIParser {

    private static final Map<String, Geometry.Type> geomTypes = buildTypeLookup();

    public static <T extends Geometry> String parseESRIToWKT(T esriObject) {
        return GeometryEngine.geometryToWkt(esriObject, 0);
    }

    public static Object wktToESRIObject(String wkt, Class<?> esriClass) {
        final Geometry.Type type = geomTypes.get(esriClass.getTypeName());
        final Geometry geometry;
        if (type == null) {
            geometry = GeometryEngine.geometryFromWkt(wkt, 0, Geometry.Type.Unknown);
        } else {
            geometry = GeometryEngine.geometryFromWkt(wkt, 0, type);
        }
        return esriClass.cast(geometry);
    }

    private static Map<String, Geometry.Type> buildTypeLookup() {
        Map<String, Geometry.Type> geomTypes = new HashMap<>();
        geomTypes.put("com.esri.core.geometry.Polygon", Geometry.Type.Polygon);
        geomTypes.put("com.esri.core.geometry.Polyline", Geometry.Type.Polyline);
        geomTypes.put("com.esri.core.geometry.Point", Geometry.Type.Point);
        geomTypes.put("com.esri.core.geometry.Multipoint", Geometry.Type.MultiPoint);
        geomTypes.put("com.esri.core.geometry.Line", Geometry.Type.Line);
        geomTypes.put("com.esri.core.geometry.Envelope", Geometry.Type.Envelope);

        return geomTypes;
    }
}
