package com.nickrobison.trestle.reasoner.spatial;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.individual.IndividualEngine;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SpatialEngineImpl implements SpatialEngine {

    private static final Logger logger = LoggerFactory.getLogger(SpatialEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private final TrestleParser tp;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final IndividualEngine individualEngine;
    private final TrestleExecutorService spatialPool;
    private WKTReader reader;
    private WKTWriter writer;


    @Inject
    public SpatialEngineImpl(TrestleParser trestleParser,
                             QueryBuilder qb,
                             ITrestleOntology ontology,
                             IndividualEngine individualEngine,
                             Metrician metrician) {
        final Config trestleConfig = ConfigFactory.load().getConfig("trestle");
        this.tp = trestleParser;
        this.qb = qb;
        this.ontology = ontology;
        this.individualEngine = individualEngine;
        this.spatialPool = TrestleExecutorService.executorFactory("spatial-pool", trestleConfig.getInt("threading.spatial-pool.size"), metrician);

//        Setup the WKT stuff
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.reader = new WKTReader(geometryFactory);
        this.writer = new WKTWriter();
    }

    @Override
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

//        Do the intersection
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
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

        try {
            return Optional.of(individualList.get());
        } catch (InterruptedException e) {
            logger.error("Spatial intersection interrupted", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Execution exception while intersecting", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }

    }
}
