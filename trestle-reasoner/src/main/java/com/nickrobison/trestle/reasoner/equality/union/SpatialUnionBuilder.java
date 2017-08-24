package com.nickrobison.trestle.reasoner.equality.union;

import com.esri.core.geometry.*;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.spatial.ESRIParser;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.TemporalUtils.compareTemporals;

public class SpatialUnionBuilder {
    private static final String TEMPORAL_OPTIONAL_ERROR = "Cannot get temporal for comparison object";

    private static final OperatorFactoryLocal instance = OperatorFactoryLocal.getInstance();
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) instance.getOperator(Operator.Type.Intersection);
    private static final OperatorUnion operatorUnion = (OperatorUnion) instance.getOperator(Operator.Type.Union);
    private final TrestleParser tp;

    @Inject
    public SpatialUnionBuilder(TrestleParser tp) {
        this.tp = tp;
    }

    @SuppressWarnings({"ConstantConditions", "squid:S3655"})
    public <T> Optional<UnionEqualityResult<T>> getApproximateEqualUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        final TemporallyDividedObjects<T> dividedObjects = divideObjects(inputObjects);

//        Extract the ESRI polygons for each objects
        final Set<Polygon> earlyPolygons = dividedObjects
                .getEarlyObjects()
                .stream()
                .map(SpatialParser::getSpatialValue)
                .map(SpatialUnionBuilder::parseESRIPolygon)
                .collect(Collectors.toSet());

        //        Extract the ESRI polygons for each objects
        final Set<Polygon> latePolygons = dividedObjects
                .getLateObjects()
                .stream()
                .map(SpatialParser::getSpatialValue)
                .map(SpatialUnionBuilder::parseESRIPolygon)
                .collect(Collectors.toSet());

        @Nullable final PolygonMatchSet polygonMatchSet = getApproxEqualUnion(earlyPolygons, latePolygons, inputSR, matchThreshold);
        if (polygonMatchSet == null) {
            return Optional.empty();
        }

//        Are we a split or a merge
        if (polygonMatchSet.earlyPolygons.size() == 1) {
            final Optional<T> first = dividedObjects.getEarlyObjects().stream().findFirst();
            return Optional.of(new UnionEqualityResult<>(first.get(), dividedObjects.getLateObjects(), TrestleEventType.SPLIT, polygonMatchSet.getStrength()));
        } else {
            final Optional<T> first = dividedObjects.getLateObjects().stream().findFirst();
            //noinspection ConstantConditions
            return Optional.of(new UnionEqualityResult<>(first.get(), dividedObjects.getEarlyObjects(), TrestleEventType.MERGED, polygonMatchSet.getStrength()));
        }
    }

    /**
     * Calculate the percentage spatial match between two object
     * @param inputObject - Input object
     * @param matchObject - Object to match against
     * @param inputSR - {@link SpatialReference} spatial reference of input objects
     * @param <T> - Generic type parameter
     * @return - {@link Double} percentage spatial match
     */
    public <T> double calculateSpatialEquals(T inputObject, T matchObject, SpatialReference inputSR) {
        final Polygon inputPolygon = parseESRIPolygon(SpatialParser.getSpatialValue(inputObject));
        final Polygon matchPolygon = parseESRIPolygon(SpatialParser.getSpatialValue(matchObject));
        return isApproxEqual(inputPolygon, matchPolygon, inputSR);
    }


    private static @Nullable PolygonMatchSet getApproxEqualUnion(Set<Polygon> inputPolygons, Set<Polygon> matchPolygons, SpatialReference inputSR, double matchThreshold) {
        final Set<Set<Polygon>> allInputSets = powerSet(inputPolygons);

        for (Set<Polygon> inputSet : allInputSets) {
            if (inputSet.isEmpty()) {
                continue;
            }

            PolygonMatchSet matchSet = executeUnionCalculation(matchPolygons, inputSR, matchThreshold, inputSet);
            if (matchSet != null) return matchSet;
        }
        return null;
    }

    private static @Nullable PolygonMatchSet executeUnionCalculation(Set<Polygon> matchPolygons, SpatialReference inputSR, double matchThreshold, Set<Polygon> inputSet) {
        final SimpleGeometryCursor inputGeomsCursor = new SimpleGeometryCursor(new ArrayList<>(inputSet));
        final GeometryCursor unionInputGeoms = operatorUnion.execute(inputGeomsCursor, inputSR, new EqualityProgressTracker("Union calculation"));
        Geometry unionInputGeom;
        while ((unionInputGeom = unionInputGeoms.next()) != null) {

//                Get all subsets of matchPolygons
            final Set<Set<Polygon>> allMatchSets = powerSet(matchPolygons);
            for (Set<Polygon> matchSet : allMatchSets) {
                if (matchSet.isEmpty()) {
                    continue;
                }

                if (executeUnion(inputSR, matchThreshold, (Polygon) unionInputGeom, new ArrayList<>(matchSet)) > matchThreshold)
                    return new PolygonMatchSet(inputSet, matchSet, matchThreshold);
            }
        }
        return null;
    }

    private static double executeUnion(SpatialReference inputSR, double matchThreshold, Polygon unionInputGeom, List<Geometry> matchGeomList) {
        final SimpleGeometryCursor matchGeomCursor = new SimpleGeometryCursor(matchGeomList);
        final GeometryCursor unionMatchGeoms = operatorUnion.execute(matchGeomCursor, inputSR, new EqualityProgressTracker("Union calculation"));
        Geometry unionMatchGeom;
        while ((unionMatchGeom = unionMatchGeoms.next()) != null) {
            final double approxEqual = isApproxEqual(unionInputGeom, (Polygon) unionMatchGeom, inputSR);
            if (approxEqual > matchThreshold) {
                return approxEqual;
            }
        }
        return 0.0;
    }

    /**
     * Calculate the percent overlap of two {@link Polygon}
     *
     * @param inputPolygon - {@link Polygon} input polygon
     * @param matchPolygon - {@link Polygon} to match against input polygon
     * @param inputSR      - {@link SpatialReference} system to compare within
     * @return - {@link Double} value of percent overlap
     */
    private static double isApproxEqual(Polygon inputPolygon, Polygon matchPolygon, SpatialReference inputSR) {
        final double inputArea = inputPolygon.calculateArea2D();
        final double matchArea = matchPolygon.calculateArea2D();
        double greaterArea = inputArea >= matchArea ? inputArea : matchArea;

        final Geometry intersectionGeom = operatorIntersection.execute(inputPolygon, matchPolygon, inputSR, new EqualityProgressTracker("Match intersection"));
        final double intersectionArea = intersectionGeom.calculateArea2D();

        return intersectionArea / greaterArea;
    }

    /**
     * Constructs a {@link Set} of {@link Set} representing all possible combinations of the input set of {@link Polygon}
     *
     * @param originalSet - {@link Set} of {@link Polygon} representing possible input combinations
     * @return - {@link Set} of {@link Set} of {@link Polygon} to determine if a union combination exists
     */
    private static Set<Set<Polygon>> powerSet(Set<Polygon> originalSet) {
        Set<Set<Polygon>> sets = new HashSet<>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        final ArrayDeque<Polygon> list = new ArrayDeque<>(originalSet);
        Polygon head = list.getFirst();
        Set<Polygon> rest = new HashSet<>(list);
        for (Set<Polygon> set : powerSet(rest)) {
            Set<Polygon> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }


    /**
     * Divide objects into 2 temporal regions
     *
     * @param inputObjects - {@link List} if input Objects
     * @param <T>          - Generic type parameter
     * @return - {@link TemporallyDividedObjects} representing objects divided on end date
     */
    private <T> TemporallyDividedObjects<T> divideObjects(List<T> inputObjects) {
        final TemporalParser temporalParser = this.tp.temporalParser;
//        Get the temporal type of the input objects
//        Sort the input objects
        inputObjects.sort((o1, o2) -> {
            // return 1 if rhs should be before lhs
            // return -1 if lhs should be before rhs
            // return 0 otherwise
            final Optional<List<TemporalObject>> lhsTemporalOptional = temporalParser.getTemporalObjects(o1);
            final Optional<List<TemporalObject>> rhsTemporalOptional = temporalParser.getTemporalObjects(o2);
//                We can just grab the first one
            final TemporalObject lhsTemporal = lhsTemporalOptional.orElseThrow(() -> new IllegalStateException(TEMPORAL_OPTIONAL_ERROR)).get(0);
            final TemporalObject rhsTemporal = rhsTemporalOptional.orElseThrow(() -> new IllegalStateException(TEMPORAL_OPTIONAL_ERROR)).get(0);
            final boolean during = lhsTemporal.during(rhsTemporal);
            if (during) {
                return 0;
            } else {
                return lhsTemporal.compareTo(rhsTemporal.getIdTemporal());
            }
        });

        Set<T> earlyObjects = new HashSet<>();
        Set<T> lateObjects = new HashSet<>();

        Temporal currentEndDate = null;
//        TODO(nrobison): Merges do not support gaps, so we probably need a way to check that the start/end dates are within a single temporal unit
        for (T currentObject : inputObjects) {
            final Temporal objectEndDate = extractEndTemporal(temporalParser.getTemporalObjects(currentObject));
            final Temporal objectStartDate = extractStartTemporal(temporalParser.getTemporalObjects(currentObject));
            if (currentEndDate == null) {
                currentEndDate = objectEndDate;
            }
            if (compareTemporals(objectEndDate, currentEndDate) != 1) {
                earlyObjects.add(currentObject);
            } else if ((compareTemporals(objectStartDate, currentEndDate) == 1) || compareTemporals(objectStartDate, currentEndDate) == 0) {
                lateObjects.add(currentObject);
            }
        }

        return new TemporallyDividedObjects<>(earlyObjects, lateObjects);
    }

    private static Temporal extractStartTemporal(Optional<List<TemporalObject>> inputTemporals) {
        final TemporalObject temporalObject = extractTemporal(inputTemporals);
        return temporalObject.getIdTemporal();
    }

    @SuppressWarnings({"ConstantConditions", "squid:S3655"})
    private static Temporal extractEndTemporal(Optional<List<TemporalObject>> inputTemporals) {
        final TemporalObject temporalObject = extractTemporal(inputTemporals);
        if (temporalObject.isPoint()) {
            return temporalObject.getIdTemporal();
        }

        if (temporalObject.isContinuing()) {
            throw new IllegalStateException("Cannot extract end temporal for continuing object");
        }
        return (Temporal) temporalObject.asInterval().getToTime().get();
    }

    private static TemporalObject extractTemporal(Optional<List<TemporalObject>> inputTemporals) {
        final List<TemporalObject> unwrappedList = inputTemporals.orElseThrow(() -> new IllegalArgumentException(TEMPORAL_OPTIONAL_ERROR));
        if (unwrappedList.isEmpty()) {
            throw new IllegalStateException(TEMPORAL_OPTIONAL_ERROR);
        }
        return unwrappedList.get(0);
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

    private static class PolygonMatchSet {

        private final Set<Polygon> earlyPolygons;
        private final Set<Polygon> latePolygons;
        private final double strength;

        PolygonMatchSet(Set<Polygon> earlyPolygons, Set<Polygon> latePolygons, double strength) {
            this.earlyPolygons = earlyPolygons;
            this.latePolygons = latePolygons;
            this.strength = strength;
        }

        Set<Polygon> getEarlyPolygons() {
            return earlyPolygons;
        }

        Set<Polygon> getLatePolygons() {
            return latePolygons;
        }

        double getStrength() {
            return this.strength;
        }
    }

    private static class EqualityProgressTracker extends ProgressTracker {
        private static final Logger logger = LoggerFactory.getLogger(EqualityProgressTracker.class);

        private final String eventName;

        EqualityProgressTracker(String eventName) {
            this.eventName = eventName;
        }

        @Override
        public boolean progress(int step, int totalExpectedSteps) {
            logger.debug("{} on step {} of {}", eventName, step, totalExpectedSteps);
            return true;
        }
    }
}
