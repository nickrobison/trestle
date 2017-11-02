package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.TrestlePair;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialUtils;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
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

import javax.inject.Inject;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.TemporalUtils.compareTemporals;
import static com.nickrobison.trestle.reasoner.engines.spatial.SpatialUtils.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Metriced
public class SpatialUnionBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SpatialUnionBuilder.class);
    private static final String TEMPORAL_OPTIONAL_ERROR = "Cannot get temporal for comparison object";

    private final TrestleParser tp;
    private final Histogram unionSetSize;

    @Inject
    public SpatialUnionBuilder(TrestleParser tp, Metrician metrician) {
        this.tp = tp;
        unionSetSize = metrician.registerHistogram("union-set-size");
    }

    public <T extends @NonNull Object> UnionContributionResult<T> calculateContribution(UnionEqualityResult<T> equalityResult, SpatialReference inputSR) {
        logger.debug("Calculating union contribution for input set");
        //        Setup the JTS components
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final WKTReader wktReader = new WKTReader(geometryFactory);

//        Determine the class of the object
        final T unionObject = equalityResult.getUnionObject();
        final Geometry geometry = buildObjectGeometry(unionObject, wktReader, wkbReader);

//        Build the contribution object
        final UnionContributionResult<T> result = new UnionContributionResult<>(unionObject, geometry.getArea());

//        Add all the others
        final Set<UnionContributionResult.UnionContributionPart<T>> contributionParts = equalityResult
                .getUnionOf()
                .stream()
//                Build geometry object
                .map(object -> new TrestlePair<>(object,
                        buildObjectGeometry(object,
                                wktReader, wkbReader)))
                .map(pair -> {
//                    Calculate the proportion contribution
                    final double percentage = calculateEqualityPercentage(geometry, pair.getRight());
                    return new UnionContributionResult.UnionContributionPart<>(pair.getLeft(), percentage);
                })
                .collect(Collectors.toSet());

        result.addAllContributions(contributionParts);

        return result;
    }

    @SuppressWarnings({"ConstantConditions", "squid:S3655"})
    @Timed(name = "union-equality-timer", absolute = true)
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> getApproximateEqualUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
//        Setup the JTS components
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final TemporallyDividedObjects<T> dividedObjects = divideObjects(inputObjects);

//        Extract the JTS polygons for each objects
        final Set<Geometry> earlyPolygons = dividedObjects
                .getEarlyObjects()
                .stream()
                .map(object -> buildObjectGeometry(object, wktReader, wkbReader))
                .collect(Collectors.toSet());

        //        Extract the JTS polygons for each objects
        final Set<Geometry> latePolygons = dividedObjects
                .getLateObjects()
                .stream()
                .map(object -> buildObjectGeometry(object, wktReader, wkbReader))
                .collect(Collectors.toSet());

        @Nullable final PolygonMatchSet polygonMatchSet = getApproxEqualUnion(geometryFactory, earlyPolygons, latePolygons, matchThreshold);
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
     *
     * @param inputObject - Input object
     * @param matchObject - Object to match against
     * @param inputSR     - {@link SpatialReference} spatial reference of input objects
     * @param <T>         - Generic type parameter
     * @return - {@link Double} percentage spatial match
     */
    public <T extends @NonNull Object> double calculateSpatialEquals(T inputObject, T matchObject, SpatialReference inputSR) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        final Geometry inputPolygon = SpatialUtils.buildObjectGeometry(inputObject, wktReader, wkbReader);
        final Geometry matchPolygon = SpatialUtils.buildObjectGeometry(inputObject, wktReader, wkbReader);

        return calculateEqualityPercentage(inputPolygon, matchPolygon);
    }


    private PolygonMatchSet getApproxEqualUnion(GeometryFactory geometryFactory, Set<Geometry> inputPolygons, Set<Geometry> matchPolygons, double matchThreshold) {
        final Set<Set<Geometry>> allInputSets = powerSet(inputPolygons);

        for (Set<Geometry> inputSet : allInputSets) {
            if (inputSet.isEmpty()) {
                continue;
            }

            PolygonMatchSet matchSet = executeUnionCalculation(geometryFactory, matchPolygons, inputSet, matchThreshold);
            if (matchSet != null) return matchSet;
        }
        return null;
    }

    @Timed(name = "union-calculation-timer", absolute = true)
    @Metered(name = "union-calculation-meter", absolute = true)
    private PolygonMatchSet executeUnionCalculation(GeometryFactory geometryFactory, Set<Geometry> matchPolygons, Set<Geometry> inputSet, double matchThreshold) {
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
            double matchStrength;
            if ((matchStrength = executeUnion(geometryFactory, matchThreshold, unionInputGeom, new ArrayList<>(matchSet))) > matchThreshold) {
                return new PolygonMatchSet(inputSet, matchSet, matchStrength);
            }
        }
        return null;
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
        Set<Set<Geometry>> sets = new HashSet<>();
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
