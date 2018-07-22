package com.nickrobison.trestle.reasoner.engines.object;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.parser.*;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TrestleFact;
import com.nickrobison.trestle.types.TrestleObjectResult;
import com.nickrobison.trestle.types.TrestleObjectState;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.iri.IRIVersion.V1;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Created by nickrobison on 2/13/18.
 */
public class TrestleObjectReader implements ITrestleObjectReader {

    private static final Logger logger = LoggerFactory.getLogger(TrestleObjectReader.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final String MISSING_INDIVIDUAL = "Unable to get individual";
    private static final OffsetDateTime TEMPORAL_MAX_VALUE = LocalDate.of(3000, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final String MISSING_FACT_ERROR = "Fact %s does not exist on dataset %s";

    private final TrestleExecutorService objectReaderThreadPool;
    private final ObjectEngineUtils engineUtils;
    private final IClassParser classParser;
    private final IClassRegister classRegister;
    private final IClassBuilder classBuilder;
    private final ITypeConverter typeConverter;
    private final FactFactory factFactory;
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final TrestleCache trestleCache;
    private final String reasonerPrefix;

    @Inject
    public TrestleObjectReader(@ReasonerPrefix String reasonerPrefix,
                               ObjectEngineUtils engineUtils,
                               TrestleParser trestleParser,
                               FactFactory factFactory,
                               ITrestleOntology ontology,
                               QueryBuilder qb,
                               TrestleCache trestleCache,
                               TrestleExecutorFactory factory) {
        this.engineUtils = engineUtils;
        this.classParser = trestleParser.classParser;
        this.classRegister = trestleParser.classRegistry;
        this.classBuilder = trestleParser.classBuilder;
        this.typeConverter = trestleParser.typeConverter;
        this.factFactory = factFactory;
        this.ontology = ontology;
        this.qb = qb;
        this.trestleCache = trestleCache;
        this.reasonerPrefix = reasonerPrefix;

        this.objectReaderThreadPool = factory.create("object-reader-pool");
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException {
        return readTrestleObject(datasetClassID, objectID, null, null);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException {
//        Lookup class
        @SuppressWarnings("unchecked") final Class<T> aClass = (Class<T>) this.engineUtils.getRegisteredClass(datasetClassID);
        return readTrestleObject(aClass, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity {
        return readTrestleObject(clazz, objectID, null, null);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity {
        final IRI individualIRI = parseStringToIRI(this.reasonerPrefix, objectID);
        return readTrestleObject(clazz, individualIRI, false, validTemporal, databaseTemporal);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache) {
        return readTrestleObject(clazz, individualIRI, bypassCache, null, null);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt) {
        logger.debug("Reading {}", individualIRI);

        //        Contains class?
        if (!this.engineUtils.checkRegisteredClass(clazz)) {
            logger.error("Class {} is not registered", clazz.getName());
            throw new IllegalArgumentException(String.format("Class %s is not registered", clazz.getName()));
        }

        final PointTemporal validTemporal;
        final PointTemporal databaseTemporal;
        if (validAt != null) {
            validTemporal = TemporalObjectBuilder.valid().at(validAt).build();
        } else {
            validTemporal = TemporalObjectBuilder.valid().at(OffsetDateTime.now()).build();
        }
        if (databaseAt != null) {
            databaseTemporal = TemporalObjectBuilder.database().at(databaseAt).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().at(OffsetDateTime.now()).build();
        }

//        Build the TrestleIRI
        final TrestleIRI trestleIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, individualIRI.getIRIString(), null,
                parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

//        Try from cache first
        final boolean isCacheable = this.classRegister.isCacheable(clazz);
        @Nullable T individual = null;
        if (isCacheable) {
            individual = this.trestleCache.getTrestleObject(clazz, trestleIRI);
        }
        if (individual != null) {
            return individual;
        }
        logger.debug("Individual is not in cache, continuing");

        final Optional<TrestleObjectResult<T>> constructedObject = readTrestleObjectImpl(clazz, individualIRI, validTemporal, databaseTemporal);
        if (constructedObject.isPresent()) {
            logger.debug("Finished reading {}", individualIRI);
//            Write back to index
            final TrestleObjectResult<T> value = constructedObject.get();
            if (isCacheable) {
                try {
                    this.trestleCache.writeTrestleObject(trestleIRI, value.getValidFrom().toInstant().atOffset(ZoneOffset.UTC), value.getValidTo().toInstant().atOffset(ZoneOffset.UTC), value.getObject());
                } catch (Exception e) {
                    logger.error("Unable to write Trestle Object {} to cache", individualIRI, e);
                }
            }
            return value.getObject();
        } else {
            throw new NoValidStateException(individualIRI, validTemporal.getIdTemporal(), databaseTemporal.getIdTemporal());
        }
    }


    /**
     * Read object implementation, going to straight to the database, completely bypassing the cache
     * Returns the object state for the given valid/database {@link PointTemporal}
     *
     * @param clazz            - Java class of type T to return
     * @param individualIRI    - IRI of individual
     * @param databaseTemporal - Database temporal to filter results with
     * @param <T>              - Java class to return
     * @return - Java object of type T
     */
    @Timed
    @Metered(name = "read-trestle-object", absolute = true)
    private <T extends @NonNull Object> Optional<TrestleObjectResult<T>> readTrestleObjectImpl(Class<T> clazz, IRI individualIRI, PointTemporal<?> validTemporal, PointTemporal<?> databaseTemporal) {
        logger.trace("Reading individual {} at {}/{}", individualIRI, validTemporal, databaseTemporal);

//        Do some things before opening a transaction
        final Optional<List<OWLDataProperty>> dataProperties = this.classBuilder.getPropertyMembers(clazz);

//        If no temporals are provided, perform the intersection at query time.
        final OffsetDateTime dbAtTemporal;
        final OffsetDateTime validAtTemporal;
        dbAtTemporal = parseTemporalToOntologyDateTime(databaseTemporal.getPointTime(), ZoneOffset.UTC);
        validAtTemporal = parseTemporalToOntologyDateTime(validTemporal.getPointTime(), ZoneOffset.UTC);

//            Get the temporal objects to figure out the correct return type
        final Class<? extends Temporal> baseTemporalType = TemporalParser.getTemporalType(clazz);

//        Build the fact query
        final String factQuery = qb.buildObjectFactRetrievalQuery(validAtTemporal, dbAtTemporal, true, null, df.getOWLNamedIndividual(individualIRI));

//        Open a new read transaction
        final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(false);

        //        Figure out its name
        if (!this.engineUtils.checkExists(individualIRI)) {
            logger.error("Missing individual {}", individualIRI);
            ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.empty();
        }

        if (dataProperties.isPresent()) {
            try {
//            Facts
                final CompletableFuture<List<TrestleFact>> factsFuture = CompletableFuture.supplyAsync(() -> {
                    final Instant individualRetrievalStart = Instant.now();
                    final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
                    TrestleResultSet resultSet = ontology.executeSPARQLResults(factQuery);
                    ontology.returnAndCommitTransaction(tt);
                    if (resultSet.getResults().isEmpty()) {
                        throw new NoValidStateException(individualIRI, validAtTemporal, dbAtTemporal);
                    }
                    final Instant individualRetrievalEnd = Instant.now();
                    logger.debug("Retrieving {} facts took {} ms", resultSet.getResults().size(), Duration.between(individualRetrievalStart, individualRetrievalEnd).toMillis());
                    return resultSet;
                }, this.objectReaderThreadPool)
                        .thenApply(resultSet -> {
//                        From the resultSet, build the Facts
                            return resultSet.getResults()
                                    .stream()
                                    .map(result -> {
                                        final OWLDataPropertyAssertionAxiom assertion = df.getOWLDataPropertyAssertionAxiom(
                                                df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)).toStringID()),
                                                result.getIndividual("individual").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)),
                                                result.getLiteral("object").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)));
//                                    Get valid temporal
                                        final Optional<TemporalObject> factValidTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.VALID, result.getLiteral("va"), result.getLiteral("vf"), result.getLiteral("vt"));
//                                    Get database temporal
                                        final Optional<TemporalObject> factDatabaseTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.DATABASE, Optional.empty(), result.getLiteral("df"), result.getLiteral("dt"));
                                        //noinspection unchecked
                                        return this.factFactory.createFact(
                                                clazz,
                                                assertion,
                                                factValidTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact valid temporal")),
                                                factDatabaseTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact database temporal")));
                                    })
                                    .collect(Collectors.toList());
                        });
//            Get the temporals
                final CompletableFuture<Optional<TemporalObject>> temporalFuture = CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
//                final Set<OWLDataPropertyAssertionAxiom> properties = ontology.getFactsForIndividual(df.getOWLNamedIndividual(individualIRI), validAtTemporal, dbAtTemporal, false);
                    final Set<OWLDataPropertyAssertionAxiom> properties = ontology.getTemporalsForIndividual(df.getOWLNamedIndividual(individualIRI));
                    ontology.returnAndCommitTransaction(tt);
                    return properties;
                }, this.objectReaderThreadPool)
                        .thenApply(temporalProperties -> TemporalObjectBuilder.buildTemporalFromProperties(temporalProperties, baseTemporalType, clazz));
//            Constructor arguments
                final CompletableFuture<TrestleObjectState> argumentsFuture = factsFuture.thenCombineAsync(temporalFuture, (facts, temporals) -> {
                    logger.debug("In the arguments future");
                    final ConstructorArguments constructorArguments = new ConstructorArguments();
                    facts.forEach(fact -> constructorArguments.addArgument(
                            this.classParser.matchWithClassMember(clazz, fact.getName(), fact.getLanguage()),
                            fact.getJavaClass(),
                            fact.getValue()));
                    if (!temporals.isPresent()) {
                        throw new IllegalStateException(String.format("Cannot restore temporal from ontology for %s", individualIRI));
                    }
//            Add the temporal to the constructor args
                    final TemporalObject temporal = temporals.get();
                    if (temporal.isInterval()) {
                        final IntervalTemporal intervalTemporal = temporal.asInterval();
                        constructorArguments.addArgument(
                                this.classParser.matchWithClassMember(clazz, intervalTemporal.getStartName()),
                                intervalTemporal.getBaseTemporalType(),
                                intervalTemporal.getFromTime());
                        if (!intervalTemporal.isDefault() && intervalTemporal.getToTime().isPresent()) {
                            constructorArguments.addArgument(
                                    this.classParser.matchWithClassMember(clazz, intervalTemporal.getEndName()),
                                    intervalTemporal.getBaseTemporalType(),
                                    intervalTemporal.getToTime().get());
                        }
                    } else {
                        constructorArguments.addArgument(
                                this.classParser.matchWithClassMember(clazz, temporal.asPoint().getParameterName()),
                                temporal.asPoint().getBaseTemporalType(),
                                temporal.asPoint().getPointTime());
                    }
//                Get the temporal ranges
//                Valid first

                    final Optional<Temporal> validStart = facts
                            .stream()
                            .map(TrestleFact::getValidTemporal)
                            .map(TemporalObject::getIdTemporal)
                            .max(TemporalUtils::compareTemporals);


                    final Optional<Temporal> validEnd = facts
                            .stream()
                            .map(TrestleFact::getValidTemporal)
                            .map(valid -> {
                                if (valid.isPoint()) {
                                    return valid.asPoint().getPointTime();
                                } else {
                                    return (Temporal) valid.asInterval().getToTime().orElse(TEMPORAL_MAX_VALUE);
                                }
                            })
                            .min(TemporalUtils::compareTemporals)
                            .map(Temporal.class::cast);
//                Database temporal, next
                    final Optional<Temporal> dbStart = facts
                            .stream()
                            .map(TrestleFact::getDatabaseTemporal)
                            .map(TemporalObject::getIdTemporal)
                            .max(TemporalUtils::compareTemporals)
                            .map(Temporal.class::cast);

                    final Optional<Temporal> dbEnd = facts
                            .stream()
                            .map(TrestleFact::getDatabaseTemporal)
                            .map(db -> {
                                if (db.isPoint()) {
                                    return db.asPoint().getPointTime();
                                } else {
                                    return (Temporal) db.asInterval().getToTime().orElse(TEMPORAL_MAX_VALUE);
                                }
                            })
                            .min(TemporalUtils::compareTemporals)
                            .map(Temporal.class::cast);

                    return new TrestleObjectState(constructorArguments, validStart.get(), validEnd.get(), dbStart.get(), dbEnd.get());
                }, this.objectReaderThreadPool);
                final TrestleObjectState objectState = argumentsFuture.get();
                if (objectState == null) {
                    logger.error("Object state is null, error must have occurred");
                    this.ontology.returnAndAbortTransaction(trestleTransaction);
                    return Optional.empty();
                }
                final T constructedObject = this.classBuilder.constructObject(clazz, objectState.getArguments());
                return Optional.of(new TrestleObjectResult<>(individualIRI, constructedObject, objectState.getMinValidFrom(), objectState.getMinValidTo(), objectState.getMinDatabaseFrom(), objectState.getMinDatabaseTo()));
            } catch (InterruptedException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Read object {} interrupted", individualIRI, e.getCause());
                Thread.currentThread().interrupt();
                return Optional.empty();
            } catch (ExecutionException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Execution exception when reading object {}", individualIRI, e.getCause());
                return Optional.empty();
            } catch (MissingConstructorException e) {
                logger.error("Problem with constructor", e);
                ontology.returnAndAbortTransaction(trestleTransaction);
                return Optional.empty();
            } finally {
                ontology.returnAndCommitTransaction(trestleTransaction);
            }
        } else {
            ontology.returnAndAbortTransaction(trestleTransaction);
            throw new IllegalStateException("No data properties, not even trying");
        }
    }

    @Override
    public List<Object> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
//        Parse String to Fact IRI
        final IRI factIRI = this.classParser.getFactIRI(clazz, factName)
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, this.classParser.getObjectClass(clazz))));

        return getFactValues(clazz,
                df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, individual)),
                df.getOWLDataProperty(factIRI), validStart, validEnd, databaseTemporal);
    }

    @Override
    public List<Object> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {

        final Class<?> datatype = this.classParser.getFactDatatype(clazz, factName.getIRI().toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, this.classParser.getObjectClass(clazz))));

//        Parse the temporal to OffsetDateTime, if they're not null
        final OffsetDateTime start, end, db;
        if (validStart != null) {
            start = parseTemporalToOntologyDateTime(validStart, ZoneOffset.UTC);
        } else {
            start = null;
        }
        if (validEnd != null) {
            end = parseTemporalToOntologyDateTime(validEnd, ZoneOffset.UTC);
        } else {
            end = null;
        }
        if (databaseTemporal != null) {
            db = parseTemporalToOntologyDateTime(databaseTemporal, ZoneOffset.UTC);
        } else {
            db = null;
        }

        final String historyQuery = this.qb.buildFactHistoryQuery(individual, factName, start, end, db);
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(historyQuery);
//        Optional::isPresent works fine, Checker is wrong
        return resultSet.getResults()
                .stream()
                .map(result -> result.unwrapLiteral("value"))
                .map(literal -> this.handleLiteral(clazz, datatype, literal))
                .collect(Collectors.toList());
    }

    @Override
    public List<Object> sampleFactValues(Class<?> clazz, String factName, long sampleLimit) {
        final IRI factIRI = this.classParser.getFactIRI(clazz, factName)
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, this.classParser.getObjectClass(clazz))));

        return sampleFactValues(clazz, df.getOWLDataProperty(factIRI), sampleLimit);
    }

    @Override
    public List<Object> sampleFactValues(Class<?> clazz, OWLDataProperty factName, long sampleLimit) {

        final OWLClass datasetClass = this.classParser.getObjectClass(clazz);
        final Class<?> datatype = this.classParser.getFactDatatype(clazz, factName.getIRI().toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, datasetClass)));

        final String factValueQuery = this.qb.buildDatasetFactValueQuery(datasetClass, factName, sampleLimit);
        final CompletableFuture<List<Object>> factValuesFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(false);
            try {
                return this.ontology.executeSPARQLResults(factValueQuery);
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, this.objectReaderThreadPool)
                .thenApply(results -> results
                        .getResults()
                        .stream()
                        .map((result) -> result.unwrapLiteral("o"))
                        .map((literal) -> this.handleLiteral(clazz, datatype, literal))
                        .collect(Collectors.toList()));

        try {
            return factValuesFuture.get();
        } catch (InterruptedException e) {
            logger.error("Interrupted while getting values for fact {} on dataset {}", factName, datasetClass, e);
            Thread.currentThread().interrupt();
            return ExceptionUtils.rethrow(e.getCause());
        } catch (ExecutionException e) {
            logger.error("Cannot get values for fact {} on dataset {}", factName, datasetClass, e);
            return ExceptionUtils.rethrow(e.getCause());
        }
    }

    @Override
    public <T> Optional<List<T>> getRelatedObjects(Class<T> clazz, String identifier, ObjectRelation relation, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        final IRI individualIRI = parseStringToIRI(identifier);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

//        Be careful, there's an inference issue here. Java things the List is of type ?, not T.
//        So we have to do the manual type assertion
        final CompletableFuture<Optional<List<T>>> relatedFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            try {
                final Optional<List<OWLObjectPropertyAssertionAxiom>> objectProperties = this.ontology.getIndividualObjectProperty(individualIRI, relation.getIRI());
                if (objectProperties.isPresent()) {
                    final List<T> relatedObjects = objectProperties
                            .get()
                            .stream()
                            .map(objectRelation -> {
                                final OWLNamedIndividual objectIndividual = objectRelation.getObject().asOWLNamedIndividual();
                                try {
                                    return this.readTrestleObject(clazz, objectIndividual.getIRI(), false, validAt, dbAt);
                                } catch (NoValidStateException e) {
                                    logger.debug("Cannot read {} at {} and {}", objectIndividual, validAt, dbAt);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return Optional.of(relatedObjects);
                } else {
                    return Optional.empty();
                }
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, this.objectReaderThreadPool);

        try {
            return relatedFuture.get();
        } catch (InterruptedException e) {
            logger.error("Interrupted while get related objects for {}", individualIRI, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            logger.error("Error when getting related objects for {}", individualIRI, rootCause);
            return Optional.empty();
        }
    }


    /**
     * Handle extracting and reprojecting a given {@link OWLLiteral}
     *
     * @param datasetClass - {@link Class} of {@link OWLClass}
     * @param datatype     - Java {@link Class} of given {@link OWLLiteral}
     * @param literal      - {@link OWLLiteral} to process
     * @return - {@link Object} of the given datatype
     */
    private Object handleLiteral(Class<?> datasetClass, Class<?> datatype, OWLLiteral literal) {
        return this.typeConverter.reprojectSpatial(
                this.typeConverter.extractOWLLiteral(datatype, literal),
                this.classParser.getClassProjection(datasetClass));
    }
}
