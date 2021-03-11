package com.nickrobison.trestle.reasoner.engines.object;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.reasoner.parser.*;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.*;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.*;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.iri.IRIVersion.V1;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;

/**
 * Created by nickrobison on 2/13/18.
 */
public class TrestleObjectReader implements ITrestleObjectReader {

    private static final Logger logger = LoggerFactory.getLogger(TrestleObjectReader.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final String MISSING_INDIVIDUAL = "Unable to get individual";
    private static final OffsetDateTime TEMPORAL_MAX_VALUE = LocalDate.of(3000, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final String MISSING_FACT_ERROR = "Fact %s does not exist on dataset %s";

    private final Scheduler objectReaderScheduler;
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

        this.objectReaderScheduler = Schedulers.from(factory.create("object-reader-pool"));
    }

    @Override
    public <T extends @NonNull Object> Single<@NonNull Object> readTrestleObject(String datasetClassID, String objectID) {
        return readTrestleObject(datasetClassID, objectID, null, null);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) {
//        Lookup class
        @SuppressWarnings("unchecked") final Class<T> aClass = (Class<T>) this.engineUtils.getRegisteredClass(datasetClassID);
        return readTrestleObject(aClass, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, String objectID) {
        return readTrestleObject(clazz, objectID, null, null);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) {
        final IRI individualIRI = parseStringToIRI(this.reasonerPrefix, objectID);
        return readTrestleObject(clazz, individualIRI, false, validTemporal, databaseTemporal, null);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable TrestleTransaction transaction) {
        return readTrestleObject(clazz, individualIRI, bypassCache, null, null, transaction);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt, @Nullable TrestleTransaction transaction) {
        logger.debug("Reading {}", individualIRI);
        //        Contains class?
        if (!this.engineUtils.checkRegisteredClass(clazz)) {
            logger.error("Class {} is not registered", clazz.getName());
            throw new IllegalArgumentException(String.format("Class %s is not registered", clazz.getName()));
        }

        final PointTemporal<?> validTemporal;
        final PointTemporal<?> databaseTemporal;
        validTemporal = TemporalObjectBuilder.valid().at(Objects.requireNonNullElseGet(validAt, OffsetDateTime::now)).build();
        databaseTemporal = TemporalObjectBuilder.database().at(Objects.requireNonNullElseGet(databaseAt, OffsetDateTime::now)).build();

//        Build the TrestleIRI
        final TrestleIRI trestleIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, individualIRI.getIRIString(), null,
                parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

//        Try from cache first, unless we've manually set the bypass
        final boolean isCacheable = this.classRegister.isCacheable(clazz) && !bypassCache;
        @Nullable T individual = null;
        if (isCacheable) {
            individual = this.trestleCache.getTrestleObject(clazz, trestleIRI);
        }
        if (individual != null) {
            return Single.just(individual);
        }
        logger.debug("Individual is not in cache, continuing");

        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transaction);
        return readTrestleObjectImpl(clazz, individualIRI, validTemporal, databaseTemporal, tt)
                .map(value -> {
                    if (isCacheable) {
                        try {
                            this.trestleCache.writeTrestleObject(trestleIRI, value.getValidFrom().toInstant().atOffset(ZoneOffset.UTC), value.getValidTo().toInstant().atOffset(ZoneOffset.UTC), value.getObject());
                        } catch (Exception e) {
                            logger.error("Unable to write Trestle Object {} to cache", individualIRI, e);
                        }
                    }
                    return value.getObject();
                })
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
    }


    /**
     * Read object implementation, going to straight to the database, completely bypassing the cache
     * Returns the object state for the given valid/database {@link PointTemporal}
     *
     * @param <T>              - Java class to return
     * @param clazz            - Java class of type T to return
     * @param individualIRI    - IRI of individual
     * @param databaseTemporal - Database temporal to filter results with
     * @param transaction      - Optional {@link TrestleTransaction} to continue with
     * @return - {@link Single} of {@link TrestleObjectResult} {@link T} Object result of type T
     */
    @Timed
    @Metered(name = "read-trestle-object", absolute = true)
    private <T extends @NonNull Object> Single<TrestleObjectResult<T>> readTrestleObjectImpl(Class<T> clazz, IRI individualIRI, PointTemporal<?> validTemporal, PointTemporal<?> databaseTemporal, @Nullable TrestleTransaction transaction) {
        logger.trace("Reading individual {} at {}/{}", individualIRI, validTemporal, databaseTemporal);
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(individualIRI);
//        If no temporals are provided, perform the intersection at query time.
        final OffsetDateTime dbAtTemporal;
        final OffsetDateTime validAtTemporal;
        dbAtTemporal = parseTemporalToOntologyDateTime(databaseTemporal.getPointTime(), ZoneOffset.UTC);
        validAtTemporal = parseTemporalToOntologyDateTime(validTemporal.getPointTime(), ZoneOffset.UTC);

//            Get the temporal objects to figure out the correct return type
        final Class<? extends Temporal> baseTemporalType = TemporalParser.getTemporalType(clazz);

//        Build the fact query
        final String factQuery = qb.buildObjectFactRetrievalQuery(validAtTemporal, dbAtTemporal, true, null, individual);

        // Build the actual query and execution
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transaction);
        final Single<List<TrestleFact<@NonNull Object>>> factsFlowable = this.ontology.executeSPARQLResults(factQuery)
                .map(result -> {
                    final OWLDataPropertyAssertionAxiom assertion = df.getOWLDataPropertyAssertionAxiom(
                            df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)).toStringID()),
                            result.getIndividual("individual").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)),
                            result.getLiteral("object").orElseThrow(() -> new IllegalStateException(MISSING_INDIVIDUAL)));
//                                    Get valid temporal
                    final Optional<TemporalObject> factValidTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.VALID, result.getLiteral("va"), result.getLiteral("vf"), result.getLiteral("vt"));
//                                    Get database temporal
                    final Optional<TemporalObject> factDatabaseTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.DATABASE, Optional.empty(), result.getLiteral("df"), result.getLiteral("dt"));
                    return this.factFactory.createFact(
                            clazz,
                            assertion,
                            factValidTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact valid temporal")),
                            factDatabaseTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact database temporal")));
                })
                .toList();

//         Fetch related objects
        final Set<OWLObjectProperty> objectProperties = this.classBuilder.getObjectPropertyMembers(clazz);
        final Single<List<TrestleAssociatedObject<Object>>> associatedObjectsFlowable;
        // If we actually have object properties, then fetch everything in a single go and filter out what we don't need
        if (objectProperties.isEmpty()) {
            associatedObjectsFlowable = Single.just(Collections.emptyList());
        } else {
            associatedObjectsFlowable = this.ontology.getAllObjectPropertiesForIndividual(individual)
                    .filter(property -> objectProperties.contains(property.getProperty().asOWLObjectProperty()))
                    .flatMapSingle(assertion -> {
                        // We can't handle multiple objects yet, so we can only have a 1-1 mapping.
                        final Class<@NonNull ?> dType = this.classParser.getPropertyDatatype(clazz, assertion.getProperty().getNamedProperty().getIRI().getIRIString());
                        return this.readTrestleObjectImpl(dType, assertion.getObject().asOWLNamedIndividual().getIRI(), validTemporal, databaseTemporal, transaction)
                                .map(result -> new TrestleAssociatedObject<Object>(assertion.getProperty().getNamedProperty(), result.getObject()));
                    })
                    .toList();
        }

        final Single<TemporalObject> temporalFlowable = this.ontology.getTemporalsForIndividual(individual)
                .collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add)
                .map(properties -> TemporalObjectBuilder.buildTemporalFromProperties(properties, baseTemporalType, clazz))
                .map(temporal -> temporal.orElseThrow(() -> new IllegalStateException(String.format("Cannot restore temporal from ontology for %s", individualIRI))));

        final Single<TrestleObjectResult<T>> mergedFlow = Single.zip(factsFlowable, temporalFlowable, associatedObjectsFlowable,
                (facts, temporal, associatedObjects) -> this.buildTrestleObjectResult(clazz, individualIRI, facts, associatedObjects, temporal, validTemporal, databaseTemporal));

        return this.engineUtils.checkExists(individualIRI)
                .flatMap(exists -> {
                    if (!exists) {
                        return Single.error(new TrestleMissingIndividualException(individualIRI.toString()));
                    } else {
                        return mergedFlow;
                    }
                })
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
    }

    @Override
    public Maybe<TrestleObjectHeader> readObjectHeader(Class<?> clazz, String individual) {
        final OWLClass objectClass = this.classParser.getObjectClass(clazz);
        final IRI individualIRI = parseStringToIRI(this.reasonerPrefix, individual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        final String headerQuery = this.qb.buildObjectHeaderQuery(objectClass, df.getOWLNamedIndividual(individualIRI));
        return this.ontology.executeSPARQLResults(headerQuery)
                .map(result -> {
                    final Optional<OWLLiteral> existsToLiteral = result.getLiteral("et");
                    final @Nullable Temporal existsTo;
                    //noinspection OptionalIsPresent
                    if (existsToLiteral.isPresent()) {
                        existsTo = parseToTemporal(existsToLiteral.get(), OffsetDateTime.class);
                    } else {
                        existsTo = null;
                    }
                    return new TrestleObjectHeader(
                            result.unwrapIndividual("m").toStringID(),
                            parseToTemporal(result.unwrapLiteral("ef"), OffsetDateTime.class),
                            existsTo);
                })
                .firstElement()
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    @Override
    public Flowable<Object> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
//        Parse String to Fact IRI
        final IRI factIRI = this.classParser.getFactIRI(clazz, factName)
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, this.classParser.getObjectClass(clazz))));

        return getFactValues(clazz,
                df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, individual)),
                df.getOWLDataProperty(factIRI), validStart, validEnd, databaseTemporal);
    }

    @Override
    public Flowable<Object> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {

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

        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(false);
        final String historyQuery = this.qb.buildFactHistoryQuery(individual, factName, start, end, db);
        return this.ontology.executeSPARQLResults(historyQuery)
                .map(result -> result.unwrapLiteral("value"))
                .map(literal -> this.handleLiteral(clazz, datatype, literal))
                .doOnError(err -> this.ontology.returnAndAbortTransaction(tt))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(tt));
    }

    @Override
    public Flowable<Object> sampleFactValues(Class<?> clazz, String factName, long sampleLimit) {
        final IRI factIRI = this.classParser.getFactIRI(clazz, factName)
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, this.classParser.getObjectClass(clazz))));

        return sampleFactValues(clazz, df.getOWLDataProperty(factIRI), sampleLimit);
    }

    @Override
    public Flowable<Object> sampleFactValues(Class<?> clazz, OWLDataProperty factName, long sampleLimit) {

        final OWLClass datasetClass = this.classParser.getObjectClass(clazz);
        final Class<?> datatype = this.classParser.getFactDatatype(clazz, factName.getIRI().toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, factName, datasetClass)));

        final String factValueQuery = this.qb.buildDatasetFactValueQuery(datasetClass, factName, sampleLimit);
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(false);
        return this.ontology.executeSPARQLResults(factValueQuery)
                .map(result -> result.unwrapLiteral("o"))
                .map(literal -> this.handleLiteral(clazz, datatype, literal))
                .doOnError(err -> this.ontology.returnAndAbortTransaction(tt))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(tt));
    }

    @Override
    public <T> Flowable<T> getRelatedObjects(Class<T> clazz, String identifier, ObjectRelation relation, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        final IRI individualIRI = parseStringToIRI(this.reasonerPrefix, identifier);

        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(false);

        return this.ontology.getIndividualObjectProperty(individualIRI, relation.getIRI())
                .flatMap(objectRelation -> {
                    final OWLNamedIndividual objectIndividual = objectRelation.getObject().asOWLNamedIndividual();
                    return this.readTrestleObject(clazz, objectIndividual.getIRI(), false, validAt, dbAt, tt).toFlowable();
                })
                .filter(Objects::nonNull)
                .doOnError(error -> this.ontology.returnAndAbortTransaction(tt))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(tt));
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

    @SuppressWarnings("unchecked")
    private <T> TrestleObjectResult<T> buildTrestleObjectResult(Class<T> clazz, IRI individualIRI, List<TrestleFact<@NonNull Object>> facts, List<TrestleAssociatedObject<Object>> associatedObjects, TemporalObject temporal, PointTemporal<?> validTemporal, PointTemporal<?> databaseTemporal) {

        final ConstructorArguments constructorArguments = new ConstructorArguments();
        facts.forEach(fact -> constructorArguments.addArgument(
                this.classParser.matchWithClassMember(clazz, fact.getName(), fact.getLanguage()),
                fact.getJavaClass(),
                fact.getValue()));

        associatedObjects.forEach(object -> {
            final String propertyName = object.getProperty().getIRI().getRemainder().orElse("");
            final String member = this.classParser.matchWithClassMember(clazz, propertyName);
            constructorArguments.addArgument(
                    member,
                    object.getObject().getClass(),
                    object.getObject());
        });

//            Add the temporal to the constructor args
        if (temporal.isInterval()) {
            final IntervalTemporal<?> intervalTemporal = temporal.asInterval();
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

        //noinspection OptionalGetWithoutIsPresent
        final TrestleObjectState objectState = new TrestleObjectState(constructorArguments, validStart.get(), validEnd.get(), dbStart.get(), dbEnd.get());
        final T constructedObject;
        try {
            constructedObject = this.classBuilder.constructObject(clazz, objectState.getArguments());
        } catch (MissingConstructorException e) {
            logger.error("Cannot find matching constructor.", e);
            throw new NoValidStateException(individualIRI, validTemporal.getIdTemporal(), databaseTemporal.getIdTemporal());
        }
        return new TrestleObjectResult<>(individualIRI, constructedObject, objectState.getMinValidFrom(), objectState.getMinValidTo(), objectState.getMinDatabaseFrom(), objectState.getMinDatabaseTo());
    }
}
