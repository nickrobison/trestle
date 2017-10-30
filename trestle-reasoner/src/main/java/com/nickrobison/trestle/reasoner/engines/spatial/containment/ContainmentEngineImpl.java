package com.nickrobison.trestle.reasoner.engines.spatial.containment;

import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
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

/**
 * Created by detwiler on 8/31/17.
 */
public class ContainmentEngineImpl implements ContainmentEngine {
    private static final OperatorExportToWkb operatorExport = OperatorExportToWkb.local();

    @Override
    public <T> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final Geometry polygonA = parseJTSGeometry(SpatialParser.getSpatialValue(objectA), wktReader, wkbReader);
        final Geometry polygonB = parseJTSGeometry(SpatialParser.getSpatialValue(objectB), wktReader, wkbReader);

        final double areaA = polygonA.getArea();
        final double areaB = polygonB.getArea();
        double smallerArea;
        ContainmentDirection containmentDir;
        if (areaA <= areaB) {
            smallerArea = areaA;
            containmentDir = ContainmentDirection.WITHIN;
        } else {
            smallerArea = areaB;
            containmentDir = ContainmentDirection.CONTAINS;
        }

        final double intersectionArea = polygonA.intersection(polygonB).getArea();

        if (intersectionArea / smallerArea >= threshold) {
            // found containment above threshold
            return containmentDir;
        } else {
            return ContainmentDirection.NONE;
        }
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
//    TODO(nrobison): Unify this with the same implementation from the Equality Engine
    private static Geometry parseJTSGeometry(Optional<Object> spatialValue, WKTReader wktReader, WKBReader wkbReader) {
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
