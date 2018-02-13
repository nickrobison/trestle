package com.nickrobison.trestle.reasoner.engines.spatial;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.SpatialReference;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.engines.IndividualEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

@Metriced
public class SpatialEngine implements EqualityEngine, ContainmentEngine {

    private static final Logger logger = LoggerFactory.getLogger(SpatialEngine.class);
    private final TrestleParser tp;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final IndividualEngine individualEngine;
    private final EqualityEngine equalityEngine;
    private final ContainmentEngine containmentEngine;
    private final TrestleExecutorService spatialPool;
    private final Cache<Integer, Geometry> geometryCache;
    private WKTReader reader;
    private WKTWriter writer;


    @Inject
    public SpatialEngine(TrestleParser trestleParser,
                         QueryBuilder qb,
                         ITrestleOntology ontology,
                         IndividualEngine individualEngine,
                         EqualityEngine equalityEngine,
                         ContainmentEngine containmentEngine,
                         Metrician metrician,
                         Cache<Integer, Geometry> cache) {
        final Config trestleConfig = ConfigFactory.load().getConfig("trestle");
        this.tp = trestleParser;
        this.qb = qb;
        this.ontology = ontology;
        this.individualEngine = individualEngine;
        this.equalityEngine = equalityEngine;
        this.containmentEngine = containmentEngine;
        this.spatialPool = TrestleExecutorService.executorFactory("spatial-pool", trestleConfig.getInt("threading.spatial-pool.size"), metrician);

//        Setup the WKT stuff
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.reader = new WKTReader(geometryFactory);
        this.writer = new WKTWriter();

//        Setup object caches
        geometryCache = cache;
    }


    /**
     * INTERSECTIONS
     */

    /**
     * Performs a spatial intersection on a given dataset with a specified spatio-temporal restriction
     * Returns an optional list of {@link TrestleIndividual}s
     * If no valid temporal is specified, performs a spatial intersection with no temporal constraints
     * This method will return the individual represented by the input WKT, so it may need to be filtered out
     *
     * @param clazz   - {@link Class} of dataset {@link OWLClass}
     * @param wkt     - {@link String} WKT boundary
     * @param buffer  - {@link Double} buffer to extend around buffer. 0 is no buffer
     * @param validAt - {@link Temporal} valid at restriction
     * @param dbAt    - {@link Temporal} database at restriction
     * @return - {@link Optional} {@link List} of {@link TrestleIndividual}
     */
    @Timed
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
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
        final String wktBuffer;
        if (buffer > 0.0) {
            logger.debug("Adding {} buffer to WKT", buffer);
            try {
                wktBuffer = this.writer.write(this.reader.read(wkt).buffer(buffer));
            } catch (ParseException e) {
                logger.error("Unable to parse wkt");
                throw new TrestleInvalidDataException("Unable to parse WKT", wkt);
            }
        } else {
            wktBuffer = wkt;
        }

        final String intersectQuery;
//        If the atTemporal is null, do a spatial intersection
        if (validAt == null) {
            intersectQuery = this.qb.buildSpatialIntersection(owlClass, wktBuffer, buffer, QueryBuilder.Units.METER, dbTemporal);
        } else {
            intersectQuery = this.qb.buildTemporalSpatialIntersection(owlClass, wktBuffer, buffer, QueryBuilder.Units.METER, atTemporal, dbTemporal);
        }

//        Do the intersection on the main thread, to try and avoid other weirdness
        logger.debug("Beginning spatial intersection, should not have any transactions");
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
//            CompletableFuture.supplyAsync(() -> {
//                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                try {
//                    return this.ontology.executeSPARQLResults(intersectQuery);
//                } finally {
//                    this.ontology.returnAndCommitTransaction(tt`);
//                }
//            }, this.spatialPool)
//                    .thenCompose((results) -> {
//                        return results
//                                .getResults()
//                                .stream()
//                                .map(result -> result.unwrapIndividual("m"))
//                                .map(owlIndividual -> CompletableFuture.supplyAsync(() -> {
//                                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                                    try {
//                                        return this.individualEngine.getTrestleIndividual(owlIndividual.asOWLNamedIndividual(), tt);
//                                    } finally {
//                                        this.ontology.returnAndCommitTransaction(tt);
//                                    }
//                                }))
//                                .collect(Collectors.toList())
//                    })
////            final List<CompletableFuture<TrestleIndividual>> individualFutures = trestleResultSet
////                    ;
//
//            final CompletableFuture<List<TrestleIndividual>> sequencedFuture = LambdaUtils.sequenceCompletableFutures(individualFutures);

//
            final CompletableFuture<List<TrestleIndividual>> individualList = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                try {
                    return this.ontology.executeSPARQLResults(intersectQuery);
                } finally {
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, this.spatialPool)
//                From the results, get all the individuals
                    .thenApply(trestleResultSet -> trestleResultSet
                            .getResults()
                            .stream()
                            .map(result -> result.unwrapIndividual("m"))
                            .collect(Collectors.toSet()))
                    .thenApply(intersectedIndividuals -> intersectedIndividuals
                            .stream()
                            .map(individual -> CompletableFuture.supplyAsync(() -> {
                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                                try {
                                    return this.individualEngine.getTrestleIndividual(individual.asOWLNamedIndividual(), tt);
                                } finally {
                                    this.ontology.returnAndCommitTransaction(tt);
                                }
                            }))
                            .collect(Collectors.toList()))
                    .thenCompose(LambdaUtils::sequenceCompletableFutures);


            return Optional.of(individualList.get());
        } catch (InterruptedException e) {
            logger.error("Spatial intersection interrupted", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Execution exception while intersecting", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (QueryEvaluationException e) {
            logger.error("You broke it, Nick!", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }

    }


    /**
     * Perform spatial comparison between two input objects
     * Object relations unidirectional are A -&gt; B. e.g. contains(A,B)
     *
     * @param objectA        - {@link Object} to compare against
     * @param objectB        - {@link Object} to compare with
     * @param inputSR        - {@link SpatialReference} input spatial reference
     * @param matchThreshold - {@link Double} cutoff for all fuzzy matches
     * @param <T extends @NonNull Object>            - Type parameter
     * @return - {@link SpatialComparisonReport}
     */
    @Timed
    public <T extends Object> SpatialComparisonReport compareObjects(T objectA, T objectB, SpatialReference inputSR, double matchThreshold) {

        final OWLNamedIndividual objectAID = this.tp.classParser.getIndividual(objectA);
        final OWLNamedIndividual objectBID = this.tp.classParser.getIndividual(objectB);
        logger.debug("Beginning comparison of {} with {}",
                objectAID,
                objectBID);

        final SpatialComparisonReport spatialComparisonReport = new SpatialComparisonReport(objectAID, objectBID);

        //        Build the geometries
        final int srid = inputSR.getID();
//        final Geometry aPolygon = this.geometryCache.get(objectA.hashCode(), key -> computeGeometry(objectA, srid));
        final Geometry aPolygon = this.getGeomFromCache(objectA, srid);
//        final Geometry bPolygon = this.geometryCache.get(objectB.hashCode(), key -> computeGeometry(objectB, srid));
        final Geometry bPolygon = this.getGeomFromCache(objectB, srid);

        //        If they're disjoint, return
        if (aPolygon.disjoint(bPolygon)) {
            return spatialComparisonReport;
        }

//        Are they equal?
        final double equality = this.equalityEngine.calculateSpatialEquals(objectA, objectB, inputSR);
        if (equality >= matchThreshold) {
            logger.debug("Found {} equality between {} and {}", objectA, objectB);
            spatialComparisonReport.addApproximateEquality(equality);
        }

//        Meets
        if (aPolygon.touches(bPolygon)) {
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
        } else if (aPolygon.covers(bPolygon)) {
            logger.debug("{} covers {}", objectA, objectB);
            spatialComparisonReport.addRelation(ObjectRelation.COVERS);
//            Also add an overlap, since the overlap is total
            spatialComparisonReport.addSpatialOverlap(SpatialParser.parseWKTFromGeom(bPolygon)
                            .orElseThrow(() -> new IllegalStateException("Can't parse Polygon")),
                    calculateOverlapPercentage(aPolygon, bPolygon));
        } else if (aPolygon.intersects(bPolygon)) { // Overlaps
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
     * Get the object {@link Geometry} from the cache, computing if absent
     *
     * @param object - {@link Object inputObject}
     * @param srid   - {@link Integer} srid
     * @return - {@link Geometry}
     */
    private Geometry getGeomFromCache(Object object, int srid) {
        final int hashCode = object.hashCode();
        final Geometry value = this.geometryCache.get(hashCode);

        if (value == null) {
            final Geometry geometry = computeGeometry(object, srid);
            this.geometryCache.put(hashCode, geometry);
            return geometry;
        }
        return value;
    }

    /**
     * EQUALITY
     */

    @Override
    @Timed
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return this.equalityEngine.calculateSpatialUnion(inputObjects, inputSR, matchThreshold);
    }

    @Override
    public <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, SpatialReference inputSR) {
        return this.equalityEngine.calculateUnionContribution(result, inputSR);
    }

    @Override
    @Timed
    public <T extends @NonNull Object> boolean isApproximatelyEqual(T inputObject, T matchObject, SpatialReference inputSR, double threshold) {
        return this.equalityEngine.isApproximatelyEqual(inputObject, matchObject, inputSR, threshold);
    }

    @Override
    @Timed
    public <T extends @NonNull Object> double calculateSpatialEquals(T inputObject, T matchObject, SpatialReference inputSR) {
        return this.equalityEngine.calculateSpatialEquals(inputObject, matchObject, inputSR);
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
    public <T extends @NonNull Object> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold) {
        return this.containmentEngine.getApproximateContainment(objectA, objectB, inputSR, threshold);
    }
    /**
     * HELPER FUNCTIONS
     */

//    /**
//     * Get the current size of the geometry geometryCache
//     *
//     * @return - {@link Integer}
//     */
//    @Gauge(name = "geometry-geometryCache-size")
//    public long getSize() {
//        return this.geometryCache.get
//    }

    /**
     * Compute {@link Geometry} for the given {@link Object}
     *
     * @param object  - {@link Object} to get Geometry from
     * @param inputSR - {@link Integer} input spatial reference
     * @return - {@link Geometry}
     */
    @Metered(name = "geometry-calculation-meter")
    public static Geometry computeGeometry(Object object, int inputSR) {
        logger.debug("Cache miss for {}, computing", object);
        return SpatialUtils.buildObjectGeometry(object, inputSR);
    }
}
