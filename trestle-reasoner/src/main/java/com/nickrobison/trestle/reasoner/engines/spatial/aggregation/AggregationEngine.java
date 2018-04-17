package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.parser.IClassBuilder;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.ITypeConverter;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.vividsolutions.jts.geom.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.reasoner.engines.object.TrestleObjectReader.MISSING_FACT_ERROR;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationEngine {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final ITrestleObjectReader reader;
    private final IClassParser parser;
    private final IClassBuilder builder;
    private final ITypeConverter typeConverter;
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
        this.builder = trestleParser.classBuilder;
        this.typeConverter = trestleParser.typeConverter;
        this.aggregationPool = factory.create("aggregation-pool");

    }

    public <T extends @NonNull Object> Optional<Geometry> aggregateDataset(Class<T> clazz, AggregationRestriction restriction, @Nullable AggregationOperation operation) {
        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final OffsetDateTime dbTemporal = OffsetDateTime.now();


        final OWLClass objectClass = this.parser.getObjectClass(clazz);
        final Integer classProjection = this.parser.getClassProjection(clazz);

//        Special handling of WKT values
        final IRI factIRI;
        if (restriction.getFact().equals("asWKT")) {
            factIRI = IRI.create(StaticIRI.GEOSPARQLPREFIX, "asWKT");
        } else {
            factIRI = this.parser.getFactIRI(clazz, restriction.getFact())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, restriction.getFact(), objectClass)));
        }

        final Class<?> factDatatype = this.parser.getFactDatatype(clazz, factIRI.toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, restriction.getFact(), objectClass)));


        final String intersectionQuery = buildAggregationQuery(clazz, objectClass, factIRI, factDatatype, restriction, operation, atTemporal, dbTemporal);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
//            First, do the TS intersection, to figure out a list of individuals to aggregate with
            final CompletableFuture<Geometry> unionGeomFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                final Instant start = Instant.now();
                try {
                    logger.debug("Performing aggregation restriction query");
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
                    .thenApply(individuals -> individuals
                            .stream()
                            .map(HasIRI::getIRI)
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

    private String buildAggregationQuery(Class<?> clazz, OWLClass datasetClass, IRI factIRI, Class<?> factDatatype, AggregationRestriction restriction, @Nullable AggregationOperation operation, OffsetDateTime atTemporal, OffsetDateTime dbTemporal) {

//        final String filterStatement;
//        if (operation != null) {
//            filterStatement = operation.buildFilterString();
//        } else {
//            filterStatement = "FILTER((?ef <= ?existsFrom^^xsd:dateTime) && " +
//                    "(!bound(?et) || (?et > ?existsTo^^xsd:dateTime)))";
//        }

        final OffsetDateTime existsFrom = LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        final OffsetDateTime existsTo = LocalDate.of(2015, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        final String restrictionQuery;
//        Are we a spatial value? If so, parse it out
        if (restriction.fact.equals("asWKT")) {
            final String wkt = (String) this.typeConverter.reprojectSpatial(restriction.getValue(), 4326);
            restrictionQuery = this.qb.buildSpatialRestrictionFragment(datasetClass, wkt, atTemporal, dbTemporal);
        } else {
            final OWLDatatype owlDatatype = this.typeConverter.getDatatypeFromJavaClass(factDatatype);
            restrictionQuery = this.qb.buildPropertyRestrictionFragment(datasetClass,
                    df.getOWLDataProperty(factIRI),
                    df.getOWLLiteral(restriction.getValue().toString(), owlDatatype),
                    existsFrom, existsTo,
                    atTemporal, dbTemporal);
        }
//        Add the aggregation operations
        final String fullQueryString;
        if (operation == null) {
            fullQueryString = this.qb.prefixizeQuery(restrictionQuery);
        } else {
//            Get the aggregation fact and data type
            final IRI opFactIRI = this.parser.getFactIRI(clazz, operation.getProperty())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, operation.getProperty(), clazz)));
            final Class<?> opFactDataType = this.parser.getFactDatatype(clazz, opFactIRI.toString())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, operation.getProperty(), clazz)));
            fullQueryString = this.qb.buildAggregationQuery(restrictionQuery,
                    df.getOWLDataProperty(opFactIRI),
                    df.getOWLLiteral(operation.getValue().toString(), this.typeConverter.getDatatypeFromJavaClass(opFactDataType)),
                    operation.getType().toString());
        }
        return fullQueryString;
    }


    public static class AggregationRestriction {

        private String fact;
        private Object value;

        public AggregationRestriction() {
//            Not used
        }

        public String getFact() {
            return fact;
        }

        public void setFact(String fact) {
            this.fact = fact;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
