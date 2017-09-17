package com.nickrobison.trestle.reasoner.containment;

import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
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
@Metriced
public class ContainmentEngineImpl implements ContainmentEngine {
    private static final OperatorExportToWkb operatorExport = OperatorExportToWkb.local();

    /**
     * Compares objectA and objectB to determine if one is contained, approximately, within the other
     * A threshold value is used to specify how close the approximation must be.
     *
     * @param objectA   - first input object
     * @param objectB   - Second input object
     * @param inputSR   - {@link SpatialReference} of objects
     * @param threshold - threshold value which determines 'approximately contained in'
     * @return - ContainmentDirection.CONTAINS if objectA contains a percentage of objectB greater than or equal to the
     * threshold. ContainmentDirection.WITHIN if objectB contains a percentage of objectA greater than or
     * equal to the threshold. ContainmentDirection.NONE if neither is true.
     */
    @Override
    @Timed
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

//        final Geometry intersectionGeom = operatorIntersection.execute(polygonA, polygonB, inputSR, new ContainmentProgressTracker("Match intersection"));
        final double intersectionArea = polygonA.intersection(polygonB).getArea();
//        final double intersectionArea = intersectionGeom.calculateArea2D();

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
     * @return - {@link Polygon}
     * @throws IllegalArgumentException is the {@link Object} is not a subclass of {@link Polygon} or {@link String}
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
