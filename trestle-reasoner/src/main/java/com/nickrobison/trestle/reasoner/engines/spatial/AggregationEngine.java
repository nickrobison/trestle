package com.nickrobison.trestle.reasoner.engines.spatial;

import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.vividsolutions.jts.geom.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.AsOWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationEngine {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    private final ITrestleObjectReader reader;
    private final IClassParser parser;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final TrestleExecutorService aggregationPool;

    @Inject
    public AggregationEngine(ITrestleObjectReader objectReader,
                             QueryBuilder queryBuilder,
                             ITrestleOntology ontology,
                             TrestleParser trestleParser,
                             TrestleExecutorFactory factory) {
        this.reader = objectReader;
        this.qb = queryBuilder;
        this.ontology = ontology;
        this.parser = trestleParser.classParser;
        this.aggregationPool = factory.create("aggregation-pool");

    }

//    public void aggregateDataset(String datasetClass, String wkt) {
//        final Class<?> registeredClass = this.objectEngineUtils.getRegisteredClass(datasetClass);
//        this.aggregateDataset(registeredClass, wkt);
//    }

    public <T extends @NonNull Object> Optional<Geometry> aggregateDataset(Class<T> clazz, String wkt) {
        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final OffsetDateTime dbTemporal = OffsetDateTime.now();


        final OWLClass objectClass = this.parser.getObjectClass(clazz);
        final Integer classProjection = this.parser.getClassProjection(clazz);

        final String intersectionQuery = this.qb.buildTemporalSpatialIntersection(objectClass, wkt, atTemporal, dbTemporal);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
//        We have to break this apart into 2 queries for a couple of reasons.
//        1. GraphDB doesn't seem to support using spatial queries as a predicate, despite what their documentation says, so we need to use FILTER statements
//        2. Using multiple FILTER statements is SLOW! We could probably do this using nested SPARQL queries, but this is good enough for now.
        try {
//            First, do the TS intersection, to figure out a list of individuals to aggregate with
            final CompletableFuture<Geometry> unionGeomFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                final Instant start = Instant.now();
                try {
                    logger.debug("Performing spatial intersection for aggregation");
                    return this.ontology.executeSPARQLResults(intersectionQuery);
                } finally {
                    logger.debug("Finished, took {} ms", Duration.between(start, Instant.now()).toMillis());
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, this.aggregationPool)
                    .thenApply(resultSet -> resultSet
                            .getResults()
                            .stream()
                            .map(result -> result.unwrapIndividual("m"))
                            .map(AsOWLNamedIndividual::asOWLNamedIndividual)
                            .collect(Collectors.toList()))
//                    Now, do the aggregation query
                    .thenApply(individuals -> {
                        final String aggregationQuery = this.qb.buildAggregationQuery(individuals,
                                LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
                                LocalDate.of(2015, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime());
                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                        try {
                            return this.ontology.executeSPARQLResults(aggregationQuery);
                        } finally {
                            this.ontology.returnAndCommitTransaction(tt);
                        }
                    })
                    .thenApply((resultSet) -> resultSet
                            .getResults()
                            .stream()
                            .map(result -> result.unwrapIndividual("m"))
                            .map(i -> i.asOWLNamedIndividual().getIRI())
                            .map(individual -> CompletableFuture.supplyAsync(() -> {
                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                                try {
                                    return this.reader.readTrestleObject(clazz, individual, false, atTemporal, dbTemporal);
                                } finally {
                                    this.ontology.returnAndCommitTransaction(tt);
                                }
                            }, this.aggregationPool))
                            .collect(Collectors.toList()))
                    .thenCompose(LambdaUtils::sequenceCompletableFutures)
                    .thenApply((objects) -> {
                        logger.debug("Intersection complete, performing union with {} objects", objects.size());
                        return objects
                                .stream()
                                .map((obj) -> SpatialEngineUtils.buildObjectGeometry(obj, classProjection))
                                .collect(Collectors.toList());
                    })
                    .thenApply((geoms) -> {
                        logger.debug("Performing actual spatial union.");
                        final List<Polygon> jtsExteriorRings = SpatialEngineUtils.getJTSExteriorRings(geoms, classProjection);
                        return new GeometryCollection(jtsExteriorRings.toArray(new Geometry[0]), new GeometryFactory(new PrecisionModel(), classProjection)).union();
                    });
//            Now we need to combine everything into a multi geometry


            final Geometry unionGeom = unionGeomFuture.get();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.of(unionGeom);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (ExecutionException e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
