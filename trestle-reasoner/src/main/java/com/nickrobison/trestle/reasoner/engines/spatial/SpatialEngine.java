package com.nickrobison.trestle.reasoner.engines.spatial;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.engines.IndividualEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ObjectEngineUtils;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.uom.SI;

import javax.cache.Cache;
import javax.inject.Inject;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

@Metriced
public class SpatialEngine implements ITrestleSpatialEngine {

    private static final Logger logger = LoggerFactory.getLogger(SpatialEngine.class);
    private final TrestleParser tp;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final ITrestleObjectReader objectReader;
    private final ObjectEngineUtils objectEngineUtils;
    private final IndividualEngine individualEngine;
    private final EqualityEngine equalityEngine;
    private final ContainmentEngine containmentEngine;
    private final TrestleExecutorService spatialPool;
    private final Cache<Integer, Geometry> geometryCache;


    @Inject
    public SpatialEngine(TrestleParser trestleParser,
                         QueryBuilder qb,
                         ITrestleOntology ontology,
                         ITrestleObjectReader objectReader,
                         ObjectEngineUtils objectEngineUtils,
                         IndividualEngine individualEngine,
                         EqualityEngine equalityEngine,
                         ContainmentEngine containmentEngine,
                         TrestleExecutorFactory factory,
                         Cache<Integer, Geometry> cache) {
        this.tp = trestleParser;
        this.qb = qb;
        this.ontology = ontology;
        this.objectReader = objectReader;
        this.objectEngineUtils = objectEngineUtils;
        this.individualEngine = individualEngine;
        this.equalityEngine = equalityEngine;
        this.containmentEngine = containmentEngine;
        this.spatialPool = factory.create("spatial-pool");

//        Setup object caches
        geometryCache = cache;
    }


    /**
     * INTERSECTIONS
     */

    @Override
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer) {
        return this.spatialIntersectIndividuals(datasetClassID, wkt, buffer, SI.METRE, null, null);
    }

    @Override
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, Unit<Length> bufferUnit) {
        return this.spatialIntersectIndividuals(datasetClassID, wkt, buffer, bufferUnit, null, null);
    }

    @Override
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        final Class<?> registeredClass = this.objectEngineUtils.getRegisteredClass(datasetClassID);
        return this.spatialIntersectIndividuals(registeredClass, wkt, buffer, SI.METRE, atTemporal, dbTemporal);
    }

    @Override
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, Unit<Length> bufferUnit, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        final Class<?> registeredClass = this.objectEngineUtils.getRegisteredClass(datasetClassID);
        return this.spatialIntersectIndividuals(registeredClass, wkt, buffer, bufferUnit, atTemporal, dbTemporal);
    }

    @Override
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        return this.spatialIntersectIndividuals(clazz, wkt, buffer, SI.METRE, validAt, dbAt);
    }

    @Override
    @Timed(name = "spatial-intersect-timer")
    @Metered(name = "spatial-intersect-meter")
    public Flowable<TrestleIndividual> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, Unit<Length> bufferUnit, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        final OWLClass owlClass = this.tp.classParser.getObjectClass(clazz);

        final OffsetDateTime atTemporal;
        final OffsetDateTime dbTemporal;

        if (validAt == null) {
            atTemporal = OffsetDateTime.now();
        } else {
            atTemporal = parseTemporalToOntologyDateTime(validAt, ZoneOffset.UTC);
        }

        if (dbAt == null) {
            dbTemporal = OffsetDateTime.now();
        } else {
            dbTemporal = parseTemporalToOntologyDateTime(dbAt, ZoneOffset.UTC);
        }

//        Buffer?
        final String wktBuffer = SpatialEngineUtils.addWKTBuffer(wkt, buffer, bufferUnit);

        final String intersectQuery;
//        If the atTemporal is null, do a spatial intersection
        if (validAt == null) {
            intersectQuery = this.qb.buildSpatialIntersection(owlClass, wktBuffer, dbTemporal);
        } else {
            intersectQuery = this.qb.buildTemporalSpatialIntersection(owlClass, wktBuffer, atTemporal, dbTemporal);
        }

//        Do the intersection on the main thread, to try and avoid other weirdness
        logger.debug("Beginning spatial intersection, should not have any transactions");
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        return this.ontology.executeSPARQLResults(intersectQuery)
                .map(result -> result.unwrapIndividual("m"))
                .flatMap(individual -> {
//                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    return Flowable.just(this.individualEngine.getTrestleIndividual(individual.asOWLNamedIndividual(), null));
//                            .doOnError(error -> this.ontology.returnAndAbortTransaction(tt))
//                            .doOnComplete(() -> this.ontology.returnAndCommitTransaction(tt));
                })
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//        try {
//            final CompletableFuture<List<TrestleIndividual>> individualList = CompletableFuture.supplyAsync(() -> {
//                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                try {
//                    return this.ontology.executeSPARQLResults(intersectQuery).toList().blockingGet();
//                } finally {
//                    this.ontology.returnAndCommitTransaction(tt);
//                }
//            }, this.spatialPool)
////                From the results, get all the individuals
//                    .thenApply(results -> results
//                            .stream()
//                            .map(result -> result.unwrapIndividual("m"))
//                            .collect(Collectors.toSet()))
//                    .thenApply(intersectedIndividuals -> intersectedIndividuals
//                            .stream()
//                            .map(individual -> CompletableFuture.supplyAsync(() -> {
//                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                                try {
//                                    return this.individualEngine.getTrestleIndividual(individual.asOWLNamedIndividual(), tt);
//                                } finally {
//                                    this.ontology.returnAndCommitTransaction(tt);
//                                }
//                            }))
//                            .collect(Collectors.toList()))
//                    .thenCompose(LambdaUtils::sequenceCompletableFutures);
//
//
//            return Optional.of(individualList.get());
//        } catch (InterruptedException e) {
//            logger.error("Spatial intersection interrupted", e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            Thread.currentThread().interrupt();
//            return Optional.empty();
//        } catch (ExecutionException e) {
//            logger.error("Execution exception while intersecting", e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } catch (QueryEvaluationException e) {
//            logger.error("You broke it, Nick!", e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }

    }


    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersectObject(T inputObject, double buffer) {
        return spatialIntersectObject(inputObject, buffer, SI.METRE, null, null);
    }

    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersectObject(T inputObject, double buffer, Unit<Length> bufferUnit) {
        return spatialIntersectObject(inputObject, buffer, bufferUnit, null, null);
    }

    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersectObject(T inputObject, double buffer, @Nullable Temporal temporalAt, @Nullable Temporal dbAt) {
        return spatialIntersectObject(inputObject, buffer, SI.METRE, temporalAt, null);
    }

    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersectObject(T inputObject, double buffer, Unit<Length> bufferUnit, @Nullable Temporal temporalAt, @Nullable Temporal dbAt) {
        // Reproject the input object into WGS 84, which is what the underlying ontologies use
        final Geometry projectedGeom = SpatialEngineUtils.reprojectObject(inputObject, this.tp.classParser.getClassProjection(inputObject.getClass()), 4326, this.geometryCache);
        final WKTWriter writer = new WKTWriter();
        final String wkt = writer.write(projectedGeom);

//        final Optional<String> wktString = SpatialParser.getSpatialValueAsString(inputObject);

//        if (wktString.isPresent()) {
//
//        }

        return spatialIntersect((Class<T>) inputObject.getClass(), wkt, buffer, bufferUnit, temporalAt, null);
//        logger.info("{} doesn't have a spatial component", owlNamedIndividual);
//        return Optional.empty();
    }

    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersect(Class<T> clazz, String wkt, double buffer) {
        return spatialIntersect(clazz, wkt, buffer, SI.METRE, null, null);
    }


    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersect(Class<T> clazz, String wkt, double buffer, Unit<Length> bufferUnit) {
        return spatialIntersect(clazz, wkt, buffer, bufferUnit, null, null);
    }

    @Override
    public <T extends @NonNull Object> Flowable<T> spatialIntersect(Class<T> clazz, String wkt, double buffer, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        return spatialIntersect(clazz, wkt, buffer, SI.METRE, validAt, null);
    }

    @Override
    @Timed(name = "spatial-intersect-timer")
    @Metered(name = "spatial-intersect-meter")
    @SuppressWarnings({"override.return.invalid"})
    public <T extends @NonNull Object> Flowable<T> spatialIntersect(Class<T> clazz, String wkt, double buffer, Unit<Length> bufferUnit, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        final OWLClass owlClass = this.tp.classParser.getObjectClass(clazz);

        final OffsetDateTime atTemporal;
        final OffsetDateTime dbTemporal;

        if (validAt == null) {
            atTemporal = OffsetDateTime.now();
        } else {
            atTemporal = parseTemporalToOntologyDateTime(validAt, ZoneOffset.UTC);
        }

        final String wktBuffer = SpatialEngineUtils.addWKTBuffer(wkt, buffer, bufferUnit);


        if (dbAt == null) {
            dbTemporal = OffsetDateTime.now();
        } else {
            dbTemporal = parseTemporalToOntologyDateTime(dbAt, ZoneOffset.UTC);
        }

//        String spatialIntersection;
        logger.debug("Running spatial intersection at time {}", atTemporal);
        final String spatialIntersection = qb.buildTemporalSpatialIntersection(owlClass, wktBuffer, atTemporal, dbTemporal);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        return this.ontology.executeSPARQLResults(spatialIntersection)
                .map(result -> IRI.create(result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()))
                .flatMapSingle(iri -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    return this.objectReader.readTrestleObject(clazz, iri, false, atTemporal, dbTemporal)
                            .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                            .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
                })
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//
//        try {
////            final String finalSpatialIntersection = spatialIntersection;
//            final CompletableFuture<List<T>> objectsFuture = CompletableFuture.supplyAsync(() -> {
//                logger.debug("Executing async spatial query");
//                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                logger.debug("Transaction opened");
//                try {
//                    return this.ontology.executeSPARQLResults(spatialIntersection).toList().blockingGet();
//                } finally {
//                    this.ontology.returnAndCommitTransaction(tt);
//                }
//            }, this.spatialPool)
//                    .thenApply(results -> results
//                            .stream()
//                            .map(result -> IRI.create(result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()))
//                            .collect(Collectors.toSet()))
//                    .thenApply(intersectedIRIs -> intersectedIRIs
//                            .stream()
//                            .map(iri -> CompletableFuture.supplyAsync(() -> {
//                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                                try {
//                                    return this.objectReader.readTrestleObject(clazz, iri, false, atTemporal, dbTemporal).blockingGet();
//                                } finally {
//                                    this.ontology.returnAndCommitTransaction(tt);
//                                }
//                            }, this.spatialPool))
//                            .collect(Collectors.toList()))
//                    .thenCompose(LambdaUtils::sequenceCompletableFutures);
//
//            return Optional.of(objectsFuture.get());
//        } catch (InterruptedException e) {
//            logger.error("Spatial intersection interrupted", e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            Thread.currentThread().interrupt();
//            return Optional.empty();
//        } catch (ExecutionException e) {
//            logger.error("Spatial intersection execution exception", e.getCause());
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
    }


    @Override
    @Timed
    public <A extends @NonNull Object, B extends @NonNull Object> SpatialComparisonReport compareTrestleObjects(A objectA, B objectB, double matchThreshold) {

        final OWLNamedIndividual objectAID = this.tp.classParser.getIndividual(objectA);
        final Integer aSRID = this.tp.classParser.getClassProjection(objectA.getClass());
        final OWLNamedIndividual objectBID = this.tp.classParser.getIndividual(objectB);
        final Integer bSRID = this.tp.classParser.getClassProjection(objectB.getClass());
        logger.debug("Beginning comparison of {} with {}",
                objectAID,
                objectBID);

        final SpatialComparisonReport spatialComparisonReport = new SpatialComparisonReport(objectAID, objectBID);

        //        Build the geometries
        final Geometry aPolygon = SpatialEngineUtils.getGeomFromCache(objectA, aSRID, this.geometryCache);
        final Geometry bPolygon = SpatialEngineUtils.getGeomFromCache(objectB, bSRID, this.geometryCache);

//        Reproject to coordinate system of Geometry A
        logger.debug("Potentially reprojecting {} from {} to {}", objectBID, bSRID, aSRID);
        final Geometry transformedB = SpatialEngineUtils.reprojectGeometry(bPolygon, bSRID, aSRID, this.geometryCache, objectB.hashCode());


        //        If they're disjoint, return
        if (aPolygon.disjoint(transformedB)) {
            return spatialComparisonReport;
        }

//        Are they equal?
        final double equality = this.equalityEngine.calculateSpatialEquals(objectA, objectB);
        if (equality >= matchThreshold) {
            logger.debug("Found {} equality between {} and {}", equality, objectA, objectB);
            spatialComparisonReport.addApproximateEquality(equality);
        }

//        Meets
        if (aPolygon.touches(transformedB)) {
            logger.debug("{} touches {}", objectA, objectB);
            spatialComparisonReport.addRelation(ObjectRelation.SPATIAL_MEETS);
//            Contains means totally inside, without any touching of the perimeter
//        } else if (aPolygon.contains(bPolygon)) {
//            logger.debug("{} contains {}", objectA, objectB);
//            spatialComparisonReport.addRelation(ObjectRelation.CONTAINS);
////            Also add an overlap, since the overlap is total
//            spatialComparisonReport.addSpatialOverlap(SpatialParser.parseWKTFromGeom(bPolygon)
//                            .orElseThrow(() -> new IllegalStateException("Can't parse Polygon")),
//                    calculateOverlapPercentage(aPolygon, bPolygon));
//            //            Covers catches all contains relationships that also allow for touching the perimeter
        } else if (aPolygon.covers(transformedB)) {
            logger.debug("{} covers {}", objectA, objectB);
            spatialComparisonReport.addRelation(ObjectRelation.COVERS);
//            Also add an overlap, since the overlap is total
            spatialComparisonReport.addSpatialOverlap(SpatialParser.parseWKTFromGeom(bPolygon)
                            .orElseThrow(() -> new IllegalStateException("Can't parse Polygon")),
                    calculateOverlapPercentage(aPolygon, bPolygon));
        } else if (aPolygon.intersects(transformedB)) { // Overlaps
            logger.debug("Found overlap between {} and {}", objectA, objectB);
            final Geometry intersection = aPolygon.intersection(bPolygon);
            spatialComparisonReport.addSpatialOverlap(SpatialParser.parseWKTFromGeom(intersection)
                            .orElseThrow(() -> new IllegalStateException("Can't parse Polygon")),
                    calculateOverlapPercentage(aPolygon, intersection));
        }

        return spatialComparisonReport;
    }

    private static double calculateOverlapPercentage(Geometry objectGeom, Geometry intersectionGeometry) {
        return intersectionGeometry.getArea() / objectGeom.getArea();
    }

    /**
     * EQUALITY
     */

    @Override
    @Timed
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, int inputSRID, double matchThreshold) {
        return this.equalityEngine.calculateSpatialUnion(inputObjects, inputSRID, matchThreshold);
    }

    @Override
    public <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, int inputSRID) {
        return this.equalityEngine.calculateUnionContribution(result, inputSRID);
    }

    @Override
    public Maybe<UnionContributionResult> calculateSpatialUnionWithContribution(String datasetClassID, List<String> individualIRIs, int inputSRID, double matchThreshold) {

        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        return Flowable.fromIterable(individualIRIs)
                .flatMapSingle(individual -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    return this.objectEngineUtils.getAdjustedQueryTemporal(individual, atTemporal, trestleTransaction)
                            .flatMap(temporal -> this.objectReader.readTrestleObject(datasetClassID, individual, temporal, null))
                            .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                            .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
                })
                .toList()
                .map(individuals -> this.calculateSpatialUnion(individuals, inputSRID, matchThreshold))
                .flatMapMaybe(result -> result.map(objectUnionEqualityResult -> Maybe.just(this.calculateUnionContribution(objectUnionEqualityResult, inputSRID))).orElseGet(Maybe::empty));
    }

    @Override
    @Timed
    public Flowable<SpatialComparisonReport> compareTrestleObjects(String datasetID, String objectAID, List<String> comparisonObjectIDs, int inputSR, double matchThreshold) {
        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        //        First, read object A
        return this.objectEngineUtils.getAdjustedQueryTemporal(objectAID, atTemporal, trestleTransaction)
                .flatMap(temporal -> this.getAdjustedIndividual(datasetID, objectAID, temporal, trestleTransaction))
                .flatMapPublisher(objectA -> {
                    return Flowable.fromIterable(comparisonObjectIDs)
                            .flatMapSingle(id -> {
                                return this.objectEngineUtils.getAdjustedQueryTemporal(id, atTemporal, trestleTransaction)
                                        .flatMap(temporal -> this.getAdjustedIndividual(datasetID, id, temporal, trestleTransaction));
                            })
                    .map(objectB -> this.compareTrestleObjects(objectA, objectB, matchThreshold));
                })
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//
////        final CompletableFuture<Object> objectAFuture = CompletableFuture.supplyAsync(() -> this.objectEngineUtils.getAdjustedQueryTemporal(objectAID, atTemporal, trestleTransaction).blockingGet(), this.spatialPool)
////                .thenCompose((temporal) -> this.getAdjustedIndividual(datasetID, objectAID, temporal, trestleTransaction));
//
////        Read all the other objects
//        final List<CompletableFuture<Object>> objectFutures = comparisonObjectIDs
//                .stream()
//                .map(id -> CompletableFuture.supplyAsync(() -> this.objectEngineUtils.getAdjustedQueryTemporal(id, atTemporal, trestleTransaction).blockingGet(),
//                        this.spatialPool)
//                        .thenCompose((temporal) -> this.getAdjustedIndividual(datasetID, id, temporal, trestleTransaction)))
//                .collect(Collectors.toList());
//        final CompletableFuture<List<Object>> sequencedFutures = LambdaUtils.sequenceCompletableFutures(objectFutures);
//
////        Run the spatial comparison
//        final CompletableFuture<List<SpatialComparisonReport>> comparisonFuture = objectAFuture
//                .thenCombineAsync(sequencedFutures,
//                        (objectA, comparisonObjects) -> comparisonObjects
//                                .stream()
//                                .map(objectB -> this.compareTrestleObjects(objectA, objectB, matchThreshold))
//                                .collect(Collectors.toList()), this.spatialPool);
//
//        try {
//            return Optional.of(comparisonFuture.get());
//        } catch (InterruptedException e) {
//            logger.error("Spatial comparison is interrupted", e.getCause());
//            Thread.currentThread().interrupt();
//            return Optional.empty();
//        } catch (ExecutionException e) {
//            logger.error("Spatial comparison was excepted", e.getCause());
//            return Optional.empty();
//        }
    }


    @Override
    @Timed
    public <A extends @NonNull Object, B extends @NonNull Object> boolean isApproximatelyEqual(A inputObject, B matchObject, double threshold) {
        return this.equalityEngine.isApproximatelyEqual(inputObject, matchObject, threshold);
    }

    @Override
    @Timed
    public <A extends @NonNull Object, B extends @NonNull Object> double calculateSpatialEquals(A inputObject, B matchObject) {
        return this.equalityEngine.calculateSpatialEquals(inputObject, matchObject);
    }

    @Override
    @Timed
    public <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, OWLNamedIndividual individual, Temporal queryTemporal) {
        return this.equalityEngine.getEquivalentIndividuals(clazz, individual, queryTemporal);
    }

    @Override
    @Timed
    public <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, List<OWLNamedIndividual> individual, Temporal queryTemporal) {
        return this.equalityEngine.getEquivalentIndividuals(clazz, individual, queryTemporal);
    }

    /**
     * CONTAINMENT
     */

    @Override
    @Timed
    public <A extends @NonNull Object, B extends @NonNull Object> ContainmentDirection getApproximateContainment(A objectA, B objectB, double threshold) {
        return this.containmentEngine.getApproximateContainment(objectA, objectB, threshold);
    }

    /*
     * HELPER FUNCTIONS
     */

    /**
     * Get an individual using an adjusted {@link Temporal} gathered from {@link ObjectEngineUtils#getAdjustedQueryTemporal(String, OffsetDateTime, TrestleTransaction)}
     *
     * @param datasetID   - {@link String} datasetID
     * @param id          - {@link String} individual ID
     * @param temporal    - {@link Temporal} adjusted temporal
     * @param transaction - {@link Nullable} {@link TrestleTransaction}
     * @return - {@link Single} of {@link Object}
     */
    private Single<Object> getAdjustedIndividual(String datasetID, String id, Temporal temporal, @Nullable TrestleTransaction transaction) {
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transaction);

        try {
            return this.objectReader.readTrestleObject(datasetID, id, temporal, null)
                    .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                    .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
        } catch (MissingOntologyEntity | TrestleClassException e) {
            this.ontology.returnAndAbortTransaction(tt);
            return Single.error(e);
        }

//        return CompletableFuture.supplyAsync(() -> {
//            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transaction);
//            try {
//                return this.objectReader.readTrestleObject(datasetID, id, temporal, null);
//            } catch (MissingOntologyEntity | TrestleClassException e) {
//                this.ontology.returnAndAbortTransaction(tt);
//                throw new CompletionException(e);
//            } finally {
//                this.ontology.returnAndCommitTransaction(tt);
//            }
//        });
    }
}
