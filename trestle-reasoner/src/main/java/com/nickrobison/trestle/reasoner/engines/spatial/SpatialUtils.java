package com.nickrobison.trestle.reasoner.engines.spatial;

import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;

import java.nio.ByteBuffer;
import java.util.Optional;

public class SpatialUtils {

    private static final OperatorExportToWkb operatorExport = OperatorExportToWkb.local();

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
//    TODO(nrobison): Unify this with the same implementation from the Equality Engine
    public static Geometry parseJTSGeometry(Optional<Object> spatialValue, WKTReader wktReader, WKBReader wkbReader) {
        final Object spatial = spatialValue.orElseThrow(() -> new IllegalStateException("Cannot get spatial value for object"));
        try {
            if (spatial instanceof Polygon) {
                final ByteBuffer wkbBuffer = operatorExport.execute(0, (Polygon) spatial, null);
                return wkbReader.read(wkbBuffer.array());
            } else if (spatial instanceof String) {
                return wktReader.read((String) spatial);
            }
            throw new IllegalArgumentException("Only ESRI Polygons are supported by the Equality Engine");
        } catch (ParseException e) {
            throw new TrestleInvalidDataException("Cannot parse input polygon", e);
        }
    }
}
