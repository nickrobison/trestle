package com.nickrobison.trestle.exporter;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Assert;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON writer borrowed from:
 * <a href="https://github.com/metteo/jts/blob/master/jts-jvm/src/main/java/com/vividsolutions/jts/io/geojson/GeoJsonWriter.java">here</a>
 */
public class GeoJsonWriter {

    private static final String EPSG_PREFIX = "EPSG:";
    private static final int TEN_VALUE = 10;

    private double scale;
    private boolean isEncodeCRS = true;

    /**
     * Constructs a GeoJsonWriter instance.
     */
    public GeoJsonWriter() {
        this(8);
    }

    /**
     * * Constructs a GeoJsonWriter instance specifying the number of decimals to
     * use when encoding floating point numbers.
     * @param decimals - {@link Integer} number of decimals to encode
     */
    public GeoJsonWriter(int decimals) {
        this.scale = Math.pow(TEN_VALUE, decimals);
    }

    public void setEncodeCRS(boolean isEncodeCRS) {
        this.isEncodeCRS = isEncodeCRS;
    }

    /**
     * Writes a {@link Geometry} in GeoJson format to a String.
     *
     * @param geometry - {@link Geometry} to write
     * @return String GeoJson Encoded Geometry
     */
    public String write(Geometry geometry) {

        final StringWriter writer = new StringWriter();
        try {
            write(geometry, writer);
        } catch (IOException ex) {
            Assert.shouldNeverReachHere();
        }

        return writer.toString();
    }

    /**
     * Writes a {@link Geometry} in GeoJson format into a {@link Writer}.
     *
     * @param geometry Geometry to encode
     * @param writer   Stream to encode to.
     * @throws IOException throws an IOException when unable to write the JSON string
     */
    public void write(Geometry geometry, Writer writer) throws IOException {
        final Map<String, Object> map = create(geometry, isEncodeCRS);
        JSONObject.writeJSONString(map, writer);
        writer.flush();
    }

    private Map<String, Object> create(Geometry geometry, boolean encodeCRS) {

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(GeoJsonConstants.NAME_TYPE, geometry.getGeometryType());

        if (geometry instanceof Point) {
            Point point = (Point) geometry;

            final String jsonString = getJsonString(point.getCoordinateSequence());

            result.put(GeoJsonConstants.NAME_COORDINATES, (JSONAware) () -> jsonString);

        } else if (geometry instanceof LineString) {
            LineString lineString = (LineString) geometry;

            final String jsonString = getJsonString(lineString
                    .getCoordinateSequence());

            result.put(GeoJsonConstants.NAME_COORDINATES, (JSONAware) () -> jsonString);

        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;

            result.put(GeoJsonConstants.NAME_COORDINATES, makeJsonAware(polygon));

        } else if (geometry instanceof MultiPoint) {
            MultiPoint multiPoint = (MultiPoint) geometry;

            result.put(GeoJsonConstants.NAME_COORDINATES, makeJsonAware(multiPoint));

        } else if (geometry instanceof MultiLineString) {
            MultiLineString multiLineString = (MultiLineString) geometry;

            result.put(GeoJsonConstants.NAME_COORDINATES, makeJsonAware(multiLineString));

        } else if (geometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon) geometry;

            result.put(GeoJsonConstants.NAME_COORDINATES, makeJsonAware(multiPolygon));

        } else if (geometry instanceof GeometryCollection) {
            GeometryCollection geometryCollection = (GeometryCollection) geometry;

            ArrayList<Map<String, Object>> geometries = new ArrayList<>(
                    geometryCollection.getNumGeometries());

            for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
                geometries.add(create(geometryCollection.getGeometryN(i), false));
            }

            result.put(GeoJsonConstants.NAME_GEOMETRIES, geometries);

        } else {
            throw new IllegalArgumentException("Unable to encode geometry " + geometry.getGeometryType());
        }

        if (encodeCRS) {
            result.put(GeoJsonConstants.NAME_CRS, createCRS(geometry.getSRID()));
        }

        return result;
    }

    private Map<String, Object> createCRS(int srid) {

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(GeoJsonConstants.NAME_TYPE, GeoJsonConstants.NAME_NAME);

        final Map<String, Object> props = new LinkedHashMap<>();
        props.put(GeoJsonConstants.NAME_NAME, EPSG_PREFIX + srid);

        result.put(GeoJsonConstants.NAME_PROPERTIES, props);

        return result;
    }

    private List<JSONAware> makeJsonAware(Polygon poly) {
        final ArrayList<JSONAware> result = new ArrayList<>();

        {
            final String jsonString = getJsonString(poly.getExteriorRing()
                    .getCoordinateSequence());
            result.add(() -> jsonString);
        }
        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
            final String jsonString = getJsonString(poly.getInteriorRingN(i)
                    .getCoordinateSequence());
            result.add(() -> jsonString);
        }

        return result;
    }

    private List<Object> makeJsonAware(GeometryCollection geometryCollection) {

        final ArrayList<Object> list = new ArrayList<>(
                geometryCollection.getNumGeometries());
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            final Geometry geometry = geometryCollection.getGeometryN(i);

            if (geometry instanceof Polygon) {
                final Polygon polygon = (Polygon) geometry;
                list.add(makeJsonAware(polygon));
            } else if (geometry instanceof LineString) {
                final LineString lineString = (LineString) geometry;
                final String jsonString = getJsonString(lineString
                        .getCoordinateSequence());
                list.add((JSONAware) () -> jsonString);
            } else if (geometry instanceof Point) {
                final Point point = (Point) geometry;
                final String jsonString = getJsonString(point.getCoordinateSequence());
                list.add((JSONAware) () -> jsonString);
            }
        }

        return list;
    }

    private String getJsonString(CoordinateSequence coordinateSequence) {
        final StringBuilder result = new StringBuilder();

        if (coordinateSequence.size() > 1) {
            result.append('[');
        }
        for (int i = 0; i < coordinateSequence.size(); i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append('[');
            result.append(formatOrdinate(coordinateSequence.getOrdinate(i, CoordinateSequence.X)));
            result.append(',');
            result.append(formatOrdinate(coordinateSequence.getOrdinate(i, CoordinateSequence.Y)));

            if (coordinateSequence.getDimension() > 2) {
                final double z = coordinateSequence.getOrdinate(i, CoordinateSequence.Z);
                if (!Double.isNaN(z)) {
                    result.append(',');
                    result.append(formatOrdinate(z));
                }
            }

            result.append(']');

        }

        if (coordinateSequence.size() > 1) {
            result.append(']');
        }

        return result.toString();
    }

    private String formatOrdinate(double x) {
        String result;

        if (Math.abs(x) >= Math.pow(TEN_VALUE, -3) && x < Math.pow(TEN_VALUE, 7)) {
            x = Math.floor(x * scale + 0.5) / scale;
            final long lx = (long) x;
            if (lx == x) {
                result = Long.toString(lx);
            } else {
                result = Double.toString(x);
            }
        } else {
            result = Double.toString(x);
        }

        return result;
    }
}
