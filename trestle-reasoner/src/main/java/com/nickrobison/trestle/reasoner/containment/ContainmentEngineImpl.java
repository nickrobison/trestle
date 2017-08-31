package com.nickrobison.trestle.reasoner.containment;

import com.esri.core.geometry.*;
import com.nickrobison.trestle.reasoner.equality.union.SpatialUnionBuilder;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.nickrobison.trestle.reasoner.parser.spatial.ESRIParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by detwiler on 8/31/17.
 */
public class ContainmentEngineImpl implements ContainmentEngine
{
    private static final OperatorFactoryLocal instance = OperatorFactoryLocal.getInstance();
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) instance.getOperator(Operator.Type.Intersection);

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
    public <T> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold)
    {
        final Polygon polygonA = parseESRIPolygon(SpatialParser.getSpatialValue(objectA));
        final Polygon polygonB = parseESRIPolygon(SpatialParser.getSpatialValue(objectB));
        final double areaA = polygonA.calculateArea2D();
        final double areaB = polygonB.calculateArea2D();
        double smallerArea;
        ContainmentDirection containmentDir;
        if(areaA <= areaB)
        {
            smallerArea = areaA;
            containmentDir = ContainmentDirection.WITHIN;
        }
        else
        {
            smallerArea = areaB;
            containmentDir = ContainmentDirection.CONTAINS;
        }

        final Geometry intersectionGeom = operatorIntersection.execute(polygonA, polygonB, inputSR, new ContainmentProgressTracker("Match intersection"));
        final double intersectionArea = intersectionGeom.calculateArea2D();

        if(intersectionArea / smallerArea >= threshold)
        {
            // found containment above threshold
            return containmentDir;
        }
        else
        {
            return ContainmentDirection.NONE;
        }
    }

    /**
     * Build a {@link Polygon} from a given {@link Object} representing the spatial value
     *
     * @param spatialValue - {@link Optional} {@link Object} representing a spatialValue
     * @return - {@link Polygon}
     * @throws IllegalArgumentException is the {@link Object} is not a subclass of {@link Polygon} or {@link String}
     */
    private static Polygon parseESRIPolygon(Optional<Object> spatialValue) {
        final Object spatial = spatialValue.orElseThrow(() -> new IllegalStateException("Cannot get spatial value for object"));
        if (spatial instanceof Polygon) {
            return (Polygon) spatial;
        } else if (spatial instanceof String) {
            return (Polygon) ESRIParser.wktToESRIObject((String) spatial, Polygon.class);
        }
        throw new IllegalArgumentException("Only ESRI Polygons are supported by the Equality Engine");
    }

    private static class ContainmentProgressTracker extends ProgressTracker {
        private static final Logger logger = LoggerFactory.getLogger(ContainmentProgressTracker.class);

        private final String eventName;

        ContainmentProgressTracker(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public boolean progress(int step, int totalExpectedSteps) {
            logger.debug("{} on step {} of {}", eventName, step, totalExpectedSteps);
            return true;
        }
    }
}
