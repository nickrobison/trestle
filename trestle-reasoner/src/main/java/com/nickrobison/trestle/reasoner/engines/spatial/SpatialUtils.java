package com.nickrobison.trestle.reasoner.engines.spatial;

import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;

import java.nio.ByteBuffer;
import java.util.Optional;

public class SpatialUtils {

    private static final OperatorExportToWkb operatorExport = OperatorExportToWkb.local();

    /**
     * Compute {@link Geometry} for the given {@link Object}
     *
     * @param object  - {@link Object} to get Geometry from
     * @param inputSR - {@link Integer} input spatial reference
     * @return - {@link Geometry}
     */
    public static Geometry buildObjectGeometry(Object object, int inputSR) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        return buildObjectGeometry(object, wktReader, wkbReader);
    }

    /**
     * Compute {@link Geometry} for the given {@link Object} using the provided Readers
     *
     * @param object    - {@link Object} to get Geometry from
     * @param wkbReader - {@link WKBReader} to use
     * @param wktReader - {@link WKTReader} to use
     * @return - {@link Geometry}
     */
    public static Geometry buildObjectGeometry(Object object, WKTReader wktReader, WKBReader wkbReader) {
        return parseJTSGeometry(SpatialParser.getSpatialValue(object), wktReader, wkbReader);
    }

    /**
     * Build a {@link Geometry} from a given {@link Object} representing the spatial value
     *
     * @param spatialValue - {@link Optional} {@link Object} representing a spatialValue
     * @param wktReader    - {@link WKTReader} for marshalling Strings to Geometries
     * @param wkbReader    - {@link WKBReader} for converting ESRI {@link Polygon} to JTS Geom
     * @return - {@link Polygon}
     * @throws IllegalArgumentException    if the {@link Object} is not a subclass of {@link Polygon} or {@link String}
     * @throws TrestleInvalidDataException if JTS is unable to Parse the spatial input
     */
    private static Geometry parseJTSGeometry(Optional<Object> spatialValue, WKTReader wktReader, WKBReader wkbReader) {
        final Object spatial = spatialValue.orElseThrow(() -> new IllegalStateException("Cannot get spatial value for object"));
        try {
            if (spatial instanceof Polygon) {
                final ByteBuffer wkbBuffer = operatorExport.execute(0, (Polygon) spatial, null);
                return wkbReader.read(wkbBuffer.array());
            } else if (spatial instanceof String) {
                return wktReader.read(String.class.cast(spatial));
            }
            throw new IllegalArgumentException("Only ESRI Polygons and WKT Strings are supported by the Equality Engine");
        } catch (ParseException e) {
            throw new TrestleInvalidDataException(e.getMessage(), spatial);
//            throw new TrestleInvalidDataException("Cannot parse input polygon", spatial);
        }
    }
}
