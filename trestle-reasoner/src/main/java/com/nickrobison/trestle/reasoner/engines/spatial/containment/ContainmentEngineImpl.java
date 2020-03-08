package com.nickrobison.trestle.reasoner.engines.spatial.containment;

import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.cache.Cache;
import javax.inject.Inject;

/**
 * Created by detwiler on 8/31/17.
 */
public class ContainmentEngineImpl implements ContainmentEngine {

    private final Cache<Integer, Geometry> geometryCache;
    private final IClassParser parser;

    @Inject
    ContainmentEngineImpl(Cache<Integer, Geometry> geometryCache, TrestleParser trestleParser) {
        this.geometryCache = geometryCache;
        this.parser = trestleParser.classParser;
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> ContainmentDirection getApproximateContainment(A objectA, B objectB, double threshold) {
        final Integer aSRID = this.parser.getClassProjection(objectA.getClass());
        final Integer bSRID = this.parser.getClassProjection(objectB.getClass());
        final Geometry polygonA = SpatialEngineUtils.getGeomFromCache(objectA, aSRID, this.geometryCache);
        final Geometry polygonB = SpatialEngineUtils.reprojectObject(objectB, bSRID, aSRID, this.geometryCache);

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
