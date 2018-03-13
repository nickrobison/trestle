package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.Polygon;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.TrestlePair;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.inject.Inject;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.TemporalUtils.compareTemporals;
import static com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils.buildObjectGeometry;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Metriced
public class SpatialUnionBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SpatialUnionBuilder.class);
    private static final String TEMPORAL_OPTIONAL_ERROR = "Cannot get temporal for comparison object";
    private static final Comparator<PolygonMatchSet> strengthComparator = Comparator.comparingDouble(PolygonMatchSet::getStrength).reversed();

    private final TrestleParser tp;
    private final Histogram unionSetSize;
    private final Cache<Integer, Geometry> geometryCache;

    @Inject
    public SpatialUnionBuilder(TrestleParser tp, Metrician metrician, Cache<Integer, Geometry> cache) {
        this.tp = tp;
        unionSetSize = metrician.registerHistogram("union-set-size");
        this.geometryCache = cache;
    }

    public <T extends @NonNull Object> UnionContributionResult calculateContribution(UnionEqualityResult<T> equalityResult, int inputSRID) {
        logger.debug("Calculating union contribution for input set");
        //        Setup the JTS components

//        Determine the class of the object
        final T unionObject = equalityResult.getUnionObject();
        final Geometry geometry = SpatialEngineUtils.getGeomFromCache(unionObject, inputSRID, this.geometryCache);

//        Build the contribution object
        final UnionContributionResult result = new UnionContributionResult(this.tp.classParser.getIndividual(unionObject),
                geometry.getArea());

//        Add all the others
        final Set<UnionContributionResult.UnionContributionPart> contributionParts = equalityResult
                .getUnionOf()
                .stream()
//                Build geometry object
                .map(object -> new TrestlePair<>(this.tp.classParser.getIndividual(object),
                        SpatialEngineUtils.getGeomFromCache(object,
                                inputSRID, this.geometryCache)))
                .map(pair -> {
//                    Calculate the proportion contribution
                    final double percentage = calculateEqualityPercentage(geometry, pair.getRight());
                    return new UnionContributionResult.UnionContributionPart(pair.getLeft(), percentage);
                })
                .collect(Collectors.toSet());

        result.addAllContributions(contributionParts);

        return result;
    }

    @SuppressWarnings({"ConstantConditions", "squid:S3655"})
    @Timed(name = "union-equality-timer", absolute = true)
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> getApproximateEqualUnion(List<T> inputObjects, int inputSRID, double matchThreshold) {
//        Setup the JTS components
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSRID);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final TemporallyDividedObjects<T> dividedObjects = divideObjects(inputObjects);

        //        If we don't have early or late polygons, then there can't be a Union, so just return
        final Set<T> earlyObjects = dividedObjects.getEarlyObjects();
        final Set<T> lateObjects = dividedObjects.getLateObjects();
        if (earlyObjects.isEmpty()
                || lateObjects.isEmpty()) {
            return Optional.empty();
        }

//        Extract the JTS polygons for each early object
        final Map<Geometry, T> earlyObjectMap = new HashMap<>(earlyObjects.size());
        final Set<Geometry> earlyPolygons = new HashSet<>(earlyObjects.size());
        for (T earlyObject : earlyObjects) {
            final Geometry earlyGeom = SpatialEngineUtils.getGeomFromCache(earlyObject, inputSRID, this.geometryCache);
            earlyObjectMap.put(earlyGeom, earlyObject);
            earlyPolygons.add(earlyGeom);
        }

        //        Extract the JTS polygons for each late object
        final Map<Geometry, T> lateObjectMap = new HashMap<>(lateObjects.size());
        final Set<Geometry> latePolygons = new HashSet<>(lateObjects.size());
        for (T lateObject : lateObjects) {
            final Geometry lateGeom = SpatialEngineUtils.getGeomFromCache(lateObject, inputSRID, this.geometryCache);
            lateObjectMap.put(lateGeom, lateObject);
            latePolygons.add(lateGeom);
        }

//        In some cases, we might have multiple objects in the early/late polygons.
//        If that happens, we need to figure out if we're a split/merge and then piecewise iterate through the possible matches
        final Queue<PolygonMatchSet> matchSetQueue;
        final MATCH_DIRECTION matchDirection = determineMatchDirection(dividedObjects);
        switch (matchDirection) {
//            If we're a merge, allocate the priority queue using the number of late polygons
            case MERGE: {
                matchSetQueue = new PriorityQueue<>(lateObjects.size(), strengthComparator);
                latePolygons
                        .forEach(latePoly -> {
                            final Optional<PolygonMatchSet> match = getApproxEqualUnion(geometryFactory, earlyPolygons, Collections.singleton(latePoly), matchThreshold);
                            match.ifPresent(matchSetQueue::add);
                        });
                break;
            }
            case SPLIT: {
                matchSetQueue = new PriorityQueue<>(earlyObjects.size(), strengthComparator);
                earlyPolygons
                        .forEach(earlyPoly -> {
                            final Optional<PolygonMatchSet> match = getApproxEqualUnion(geometryFactory, Collections.singleton(earlyPoly), latePolygons, matchThreshold);
                            match.ifPresent(matchSetQueue::add);
                        });
                break;
            }
//            If we don't know, we need to do it in both directions
            default: {
//                Merged first
                matchSetQueue = new PriorityQueue<>(lateObjects.size() + earlyObjects.size(), strengthComparator);
                latePolygons
                        .forEach(latePoly -> {
                            final Optional<PolygonMatchSet> match = getApproxEqualUnion(geometryFactory, earlyPolygons, Collections.singleton(latePoly), matchThreshold);
                            match.ifPresent(matchSetQueue::add);
                        });
//                Then split
                earlyPolygons
                        .forEach(earlyPoly -> {
                            final Optional<PolygonMatchSet> match = getApproxEqualUnion(geometryFactory, Collections.singleton(earlyPoly), latePolygons, matchThreshold);
                            match.ifPresent(matchSetQueue::add);
                        });
                break;
            }
        }

        @Nullable final PolygonMatchSet polygonMatchSet = matchSetQueue.peek();
        if (polygonMatchSet == null) {
            return Optional.empty();
        }

        switch (matchDirection) {
//            If we're a merge, get the late object, and its associated early objects
            case MERGE: {
                return Optional.of(new UnionEqualityResult<>(
                        getSetFirstFromMap(lateObjectMap, polygonMatchSet.latePolygons),
                        getMapValuesFromSet(earlyObjectMap, polygonMatchSet.earlyPolygons),
                        TrestleEventType.MERGED,
                        polygonMatchSet.getStrength()));
            }
            case SPLIT: {
                return Optional.of(new UnionEqualityResult<>(
                        getSetFirstFromMap(earlyObjectMap, polygonMatchSet.earlyPolygons),
                        getMapValuesFromSet(lateObjectMap, polygonMatchSet.latePolygons),
                        TrestleEventType.SPLIT,
                        polygonMatchSet.getStrength()));
            }
            default:
                throw new IllegalStateException("Can only have SPLIT/MERGE types");
        }
    }

    /**
     * Calculate the percentage spatial match between two object
     *
     * @param inputObject - Input object
     * @param matchObject - Object to match against
     * @param <A>         - Generic type parameter of input object
     * @param <B>         - Generic type parameter of match object
     * @return - {@link Double} percentage spatial match
     */
    public <A extends @NonNull Object, B extends @NonNull Object> double calculateSpatialEquals(A inputObject, B matchObject) {
        final Integer aSRID = this.tp.classParser.getClassProjection(inputObject.getClass());
        final Integer bSRID = this.tp.classParser.getClassProjection(matchObject.getClass());
        final Geometry inputPolygon = SpatialEngineUtils.getGeomFromCache(inputObject, aSRID, this.geometryCache);
//        Re-project match object to input object SRID, unless we already have one in the cache
        final Geometry matchPolygon = SpatialEngineUtils.reprojectObject(matchObject, bSRID, aSRID, this.geometryCache);


        return calculateEqualityPercentage(inputPolygon, matchPolygon);
    }


    private Optional<PolygonMatchSet> getApproxEqualUnion(GeometryFactory geometryFactory, Set<Geometry> inputPolygons, Set<Geometry> matchPolygons, double matchThreshold) {
        final Set<Set<Geometry>> allInputSets = powerSet(inputPolygons);

        for (Set<Geometry> inputSet : allInputSets) {
            if (inputSet.isEmpty()) {
                continue;
            }

            Optional<PolygonMatchSet> matchSet = executeUnionCalculation(geometryFactory, matchPolygons, inputSet, matchThreshold);
            if (matchSet.isPresent()) {
                return matchSet;
            }
        }
        return Optional.empty();
    }

    @Timed(name = "union-calculation-timer", absolute = true)
    @Metered(name = "union-calculation-meter", absolute = true)
    private Optional<PolygonMatchSet> executeUnionCalculation(GeometryFactory geometryFactory, Set<Geometry> matchPolygons, Set<Geometry> inputSet, double matchThreshold) {
        final GeometryCollection geometryCollection = new GeometryCollection(inputSet.toArray(new Geometry[inputSet.size()]), geometryFactory);
        logger.trace("Executing union operation for {}", inputSet);
        final Geometry unionInputGeom = geometryCollection.union();

        this.unionSetSize.update(inputSet.size());

//                Get all subsets of matchPolygons
        final Set<Set<Geometry>> allMatchSets = powerSet(matchPolygons);
        for (Set<Geometry> matchSet : allMatchSets) {
            if (matchSet.isEmpty()) {
                continue;
            }
            final double matchStrength;
            if ((matchStrength = executeUnion(geometryFactory, matchThreshold, unionInputGeom, new ArrayList<>(matchSet))) > matchThreshold) {
                return Optional.of(new PolygonMatchSet(inputSet, matchSet, matchStrength));
            }
        }
        return Optional.empty();
    }

    @Timed(name = "union-strength-timer", absolute = true)
    @Metered(name = "union-strength-meter", absolute = true)
    private double executeUnion(GeometryFactory geometryFactory, double matchThreshold, Geometry unionInputGeom, List<Geometry> matchGeomList) {
        final GeometryCollection geometryCollection = new GeometryCollection(matchGeomList.toArray(new Geometry[matchGeomList.size()]), geometryFactory);
        logger.trace("Executing union operation for {}", matchGeomList);
        this.unionSetSize.update(matchGeomList.size());
        final Geometry unionMatchGeom = geometryCollection.union();
        final double approxEqual = calculateEqualityPercentage(unionInputGeom, unionMatchGeom);
        if (approxEqual > matchThreshold) {
            return approxEqual;
        }
        return 0.0;
    }

    /**
     * Calculate the percent overlap of two {@link Polygon}
     *
     * @param inputPolygon - {@link Polygon} input polygon
     * @param matchPolygon - {@link Polygon} to match against input polygon
     * @return - {@link Double} value of percent overlap
     */
    private static double calculateEqualityPercentage(Geometry inputPolygon, Geometry matchPolygon) {
        final double inputArea = inputPolygon.getArea();
        final double matchArea = matchPolygon.getArea();
        double greaterArea = inputArea >= matchArea ? inputArea : matchArea;

        final double intersectionArea = inputPolygon.intersection(matchPolygon).getArea();

        return intersectionArea / greaterArea;
    }

    /**
     * Constructs a {@link Set} of {@link Set} representing all possible combinations of the input set of {@link Polygon}
     *
     * @param originalSet - {@link Set} of {@link Polygon} representing possible input combinations
     * @return - {@link Set} of {@link Set} of {@link Polygon} to determine if a union combination exists
     */
    private static Set<Set<Geometry>> powerSet(Set<Geometry> originalSet) {
//        No idea why we need to do this instead of ComparingInt.reversed(), but we do
        SortedSet<Set<Geometry>> sets = new TreeSet<>((o1, o2) -> Integer.compare(o2.size(), o1.size()));
//        Null safe way of handling an empty queue
        final Queue<Geometry> list = new ArrayDeque<>(originalSet);
        Geometry head = list.poll();
        if (head == null) {
            sets.add(new HashSet<>());
            return sets;
        }

        Set<Geometry> rest = new HashSet<>(list);
        for (Set<Geometry> set : powerSet(rest)) {
            Set<Geometry> newSet = new HashSet<>();
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
    private <T extends @NonNull Object> TemporallyDividedObjects<T> divideObjects(List<T> inputObjects) {
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

    private static MATCH_DIRECTION determineMatchDirection(TemporallyDividedObjects objects) {
        if (objects.getEarlyObjects().size() < objects.getLateObjects().size()) {
            return MATCH_DIRECTION.SPLIT;
        } else if (objects.getEarlyObjects().size() > objects.getLateObjects().size()) {
            return MATCH_DIRECTION.MERGE;
        }
        return MATCH_DIRECTION.UNKNOWN;
    }

    /**
     * From the provided object set, get its corresponding map entry
     *
     * @param objectMap  - {@link Map} of {@link Geometry} to input objects
     * @param polygonSet - {@link Set} of {@link Geometry} to get from
     * @param <T>        - {@link T} type parameter
     * @return - {@link T} object from map
     */
    private static <T extends @NonNull Object> T getSetFirstFromMap(Map<Geometry, T> objectMap, Set<Geometry> polygonSet) {
        final T first = objectMap.get(polygonSet.stream().findFirst().orElseThrow(() -> new IllegalStateException("Cannot get first polygon from set")));
        if (first == null) {
            throw new IllegalStateException("Cannot have empty map from Set");
        }
        return first;
    }

    /**
     * Get the matching objects in the given object {@link Map} from the {@link Set} of {@link Geometry}
     *
     * @param objectMap  - {@link Map} of {@link Geometry} to input objects
     * @param polygonSet - {@link Set} of {@link Geometry} to get from
     * @param <T>        - {@link T} type parameter
     * @return - {@link T} object fromm map
     */
    //        We know that this is non-null, so we can supress this
    @SuppressWarnings("methodref.return.invalid")
    private static <T extends @NonNull Object> Set<T> getMapValuesFromSet(Map<Geometry, T> objectMap, Set<Geometry> polygonSet) {
        return polygonSet
                .stream()
                .map(objectMap::get)
                .collect(Collectors.toSet());
    }

    private enum MATCH_DIRECTION {
        SPLIT,
        MERGE,
        UNKNOWN
    }

    private static class PolygonMatchSet {

        private final Set<Geometry> earlyPolygons;
        private final Set<Geometry> latePolygons;
        private final double strength;

        PolygonMatchSet(Set<Geometry> earlyPolygons, Set<Geometry> latePolygons, double strength) {
            this.earlyPolygons = earlyPolygons;
            this.latePolygons = latePolygons;
            this.strength = strength;
        }

        Set<Geometry> getEarlyPolygons() {
            return earlyPolygons;
        }

        Set<Geometry> getLatePolygons() {
            return latePolygons;
        }

        double getStrength() {
            return this.strength;
        }
    }
}
