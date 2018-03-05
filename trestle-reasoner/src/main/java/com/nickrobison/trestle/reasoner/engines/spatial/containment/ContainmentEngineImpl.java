package com.nickrobison.trestle.reasoner.engines.spatial.containment;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Created by detwiler on 8/31/17.
 */
public class ContainmentEngineImpl implements ContainmentEngine {

    ContainmentEngineImpl() {
    }

    @Override
    public <T extends @NonNull Object> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final Geometry polygonA = SpatialUtils.buildObjectGeometry(objectA, wktReader, wkbReader);
        final Geometry polygonB = SpatialUtils.buildObjectGeometry(objectB, wktReader, wkbReader);

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

}
