package com.nickrobison.trestle.exporter;

/**
 * Borrowed from <a href="https://github.com/metteo/jts/blob/master/jts-jvm/src/main/java/com/vividsolutions/jts/io/geojson/GeoJsonConstants.java">here</a>
 */
public class GeoJsonConstants {

    private GeoJsonConstants() {
//        Not used
    }

    public static final String NAME_GEOMETRIES = "geometries";
    public static final String NAME_CRS = "crs";
    public static final String NAME_PROPERTIES = "properties";
    public static final String NAME_NAME = "name";
    public static final String NAME_TYPE = "type";
    public static final String NAME_FEATURE = "Feature";
    public static final String NAME_COLLECTION = "FeatureCollection";
    public static final String NAME_POINT = "Point";
    public static final String NAME_LINESTRING = "LineString";
    public static final String NAME_POLYGON = "Polygon";
    public static final String NAME_COORDINATES = "coordinates";
    public static final String NAME_GEOMETRYCOLLECTION = "GeometryCollection";
    public static final String NAME_MULTIPOLYGON = "MultiPolygon";
    public static final String NAME_MULTILINESTRING = "MultiLineString";
    public static final String NAME_MULTIPOINT = "MultiPoint";
}
