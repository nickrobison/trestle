package com.nickrobison.trestle.reasoner.engines.object;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.merge.MergeScript;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.relations.RelationTracker;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.*;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Supplier;
import org.apache.commons.lang3.ClassUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.iri.IRIVersion.V1;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Created by nickrobison on 2/13/18.
 */
public class TrestleObjectWriter implements ITrestleObjectWriter {

    private static final Logger logger = LoggerFactory.getLogger(TrestleObjectWriter.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final OWLClass DATASET_CLASS = df.getOWLClass(StaticIRI.datasetClassIRI);

    private final TrestleEventEngine eventEngine;
    private final TrestleExecutorService objectWriterThreadPool;
    private final Metrician metrician;
    private final ObjectEngineUtils engineUtils;
    private final IClassParser classParser;
    private final IClassBuilder classBuilder;
    private final ITypeConverter typeConverter;
    private final TemporalParser temporalParser;
    private final TrestleMergeEngine mergeEngine;
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final TrestleCache trestleCache;
    private final RelationTracker relationTracker;
    private final String reasonerPrefix;


    @Inject
    public TrestleObjectWriter(@ReasonerPrefix String reasonerPrefix,
                               TrestleEventEngine eventEngine,
                               Metrician metrician,
                               ObjectEngineUtils engineUtils,
                               TrestleParser trestleParser,
                               TrestleMergeEngine mergeEngine,
                               ITrestleOntology ontology,
                               QueryBuilder queryBuilder,
                               TrestleCache trestleCache,
                               RelationTracker relationTracker,
                               TrestleExecutorFactory factory) {
        this.eventEngine = eventEngine;
        this.metrician = metrician;
        this.engineUtils = engineUtils;
        this.classParser = trestleParser.classParser;
        this.classBuilder = trestleParser.classBuilder;
        this.temporalParser = trestleParser.temporalParser;
        this.typeConverter = trestleParser.typeConverter;
        this.mergeEngine = mergeEngine;
        this.ontology = ontology;
        this.qb = queryBuilder;
        this.trestleCache = trestleCache;
        this.relationTracker = relationTracker;
        this.reasonerPrefix = reasonerPrefix;

        this.objectWriterThreadPool = factory.create("object-writer-pool");
    }

    @Override
    public Completable writeTrestleObject(Object inputObject) {
//        return this.ontology.
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return writeTrestleObjectImpl(inputObject, null)
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }

    @Override
    public Completable writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).to(endTemporal).build();
        }
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return writeTrestleObjectImpl(inputObject, databaseTemporal)
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }

    @Override
    public Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return this.addFactToTrestleObjectImpl(clazz, individual, factName, value, validAt, null, null, databaseFrom)
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(err -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    @Override
    public Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return addFactToTrestleObjectImpl(clazz, individual, factName, value, null, validFrom, validTo, databaseFrom)
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(err -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    @Override
    public <T extends @NonNull Object> Completable addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength) {
        if (!(type == TrestleEventType.SPLIT) && !(type == TrestleEventType.MERGED)) {
            return Completable.error(new IllegalArgumentException("Only MERGED and SPLIT types are valid"));
        }

        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
//        Build the event name
        final OWLNamedIndividual eventIndividual = TrestleEventEngine.buildEventName(df, this.reasonerPrefix, subjectIndividual, type);
//        Get the event temporal to use, and just grab the first one
        final Optional<List<TemporalObject>> temporalObjects = this.temporalParser.getTemporalObjects(subject);
        if (temporalObjects.isEmpty()) {
            return Completable.error(new IllegalStateException("Cannot get temporals for individual"));
        }
        final TemporalObject subjectTemporal = temporalObjects.get().get(0);
        final Temporal eventTemporal;
        if (type == TrestleEventType.SPLIT) {
//            If it's a split, grab the ending temporal
            if (subjectTemporal.isPoint()) {
                eventTemporal = subjectTemporal.getIdTemporal();
            } else if (subjectTemporal.isInterval() && !subjectTemporal.isContinuing()) {
                eventTemporal = (Temporal) subjectTemporal.asInterval().getToTime().get();
            } else {
                return Completable.error(new IllegalArgumentException(String.format("Cannot add event to continuing object %s", subjectIndividual.toStringID())));
            }
        } else {
//            We know it's a merge, so get the starting temporal
            eventTemporal = subjectTemporal.getIdTemporal();
        }
        final Set<OWLNamedIndividual> objectIndividuals = objects
                .stream()
                .map(this.classParser::getIndividual)
                .collect(Collectors.toSet());

//        Write everyone
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return this.writeTrestleObject(subject)
                .andThen(Completable.defer(() -> Observable.fromIterable(objects)
                        .flatMapCompletable(this::writeTrestleObject)))
                //            Add the event
                .andThen(Completable.defer(() -> this.eventEngine.addSplitMergeEvent(type, subjectIndividual, objectIndividuals, eventTemporal)))
                // Write the strength
                .andThen(Completable.defer(() -> this.ontology.writeIndividualDataProperty(eventIndividual, df.getOWLDataProperty(relationStrengthIRI), df.getOWLLiteral(strength))))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }

    @Override
    public Completable writeObjectRelationship(Object subject, Object object, ObjectRelation relation, @Nullable TrestleTransaction transaction) {
        return this.writeObjectProperty(subject, object, df.getOWLObjectProperty(relation.getIRI()), transaction);
    }

    @Override
    public Completable writeSpatialOverlap(Object subject, Object object, String wkt) {
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(this.reasonerPrefix,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
        return this.ontology.createIndividual(overlapClassAssertion)
                .andThen(Completable.defer(() -> {
                    //        Write the overlap intersection
                    final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(sOverlapIRI), overlapIndividual, df.getOWLLiteral(wkt, df.getOWLDatatype(WKTDatatypeIRI)));
                    return this.ontology.writeIndividualDataProperty(sOverlapAssertion);
                }))
                .andThen(Completable.defer(() -> {
                    //        Write the subject relation
                    final OWLObjectProperty overlapProperty = df.getOWLObjectProperty(overlapOfIRI);
                    //        Write the object relation
                    return this.writeIndirectObjectProperty(overlapIndividual, subject, overlapProperty)
                            .andThen(Completable.defer(() -> this.writeIndirectObjectProperty(overlapIndividual, object, overlapProperty)));
                }));


    }

    //    TODO(nrobison): Correctly implement this
    @Override
    public Completable writeTemporalOverlap(Object subject, Object object, String temporalOverlap) {
        logger.warn("Temporal overlaps not implemented yet, overlap value has no meaning");
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(this.reasonerPrefix,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
//        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);

        return this.ontology.createIndividual(overlapClassAssertion)
                .andThen(Completable.defer(() -> {
                    //        Write the overlap intersection
                    final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(tOverlapIRI), overlapIndividual, df.getOWLLiteral(temporalOverlap));
                    return this.ontology.writeIndividualDataProperty(sOverlapAssertion);
                }))
                .andThen(Completable.defer(() -> {
                    //        Write the subject relation
                    final OWLObjectProperty overlapProperty = df.getOWLObjectProperty(overlapOfIRI);
                    return this.writeIndirectObjectProperty(overlapIndividual, subject, overlapProperty)
                            .andThen(Completable.defer(() -> this.writeIndirectObjectProperty(overlapIndividual, object, overlapProperty)));
                }));
//                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction))
//                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }

    /**
     * Writes an object into the ontology using the object's temporal scope
     * If a temporal is provided it uses that for the database time interval
     *
     * @param inputObject      - Object to write to the ontology
     * @param databaseTemporal - Optional TemporalObject to manually set database time
     * @return {@link Completable} when finished
     */
    @Timed
    @Metered(name = "trestle-object-write", absolute = true)
    Completable writeTrestleObjectImpl(Object inputObject, @Nullable TemporalObject databaseTemporal) {
        final Class<?> aClass = inputObject.getClass();
        if (!this.engineUtils.checkRegisteredClass(aClass)) {
            return Completable.error(new UnregisteredClassException(aClass));
        }

        final OWLNamedIndividual owlNamedIndividual = this.classParser.getIndividual(inputObject);

//            Create the database time object, set to UTC, of course
        final TemporalObject dTemporal;
        if (databaseTemporal == null) {
            dTemporal = TemporalObjectBuilder.database().from(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)).build();
        } else {
            dTemporal = databaseTemporal;
        }

        //        Get the temporal
        final Optional<List<TemporalObject>> temporalObjects = this.temporalParser.getTemporalObjects(inputObject);
        if (temporalObjects.isEmpty()) {
            return Completable.error(new RuntimeException(String.format("Cannot parse temporals for %s", owlNamedIndividual)));
        }
        TemporalObject objectTemporal = temporalObjects.get().get(0);
        TemporalObject factTemporal = objectTemporal.castTo(TemporalScope.VALID);

        return this.engineUtils.checkExists(owlNamedIndividual.getIRI())
                .concatMapCompletable(exists -> {
                    // Write and merge
                    if (this.mergeEngine.mergeOnLoad() && exists) {
                        return doObjectWriteAndMerge(inputObject, aClass, owlNamedIndividual, dTemporal, factTemporal);
                    } else { // Do the pure write, no merge
                        return doSimpleObjectWrite(inputObject, aClass, owlNamedIndividual, dTemporal, objectTemporal, factTemporal);
                    }
                })
                .andThen(Completable.defer(() -> {
                    final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, owlNamedIndividual.toStringID(), null,
                            parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC),
                            parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC));
                    logger.debug("Purging {} from the cache", individualIRI);
                    trestleCache.deleteTrestleObject(individualIRI);
                    this.relationTracker.removeComputedRelations(owlNamedIndividual);
                    return Completable.complete();
                }));
    }

    /**
     * Write a Trestle Object while also performing the merge operations for new and existing facts.
     * Merges occur as per the set {@link com.nickrobison.trestle.reasoner.engines.merge.MergeStrategy}
     *
     * @param inputObject        - {@link Object} to write
     * @param aClass             - {@link Class} Java class of object
     * @param owlNamedIndividual - {@link OWLNamedIndividual} object name
     * @param dTemporal          - {@link TemporalObject} database temporal to write
     * @param factTemporal       - {@link TemporalObject} fact temporal to write
     * @return - {@link Completable} when finished
     */
    private Completable doObjectWriteAndMerge(Object inputObject, Class<?> aClass, OWLNamedIndividual owlNamedIndividual, TemporalObject dTemporal, TemporalObject factTemporal) {
        final Timer.Context mergeTimer = this.metrician.registerTimer("trestle-merge-timer").time();
        final Optional<List<OWLDataPropertyAssertionAxiom>> individualFactsOptional = this.classParser.getFacts(inputObject);
//            Get all the currently valid facts
        if (individualFactsOptional.isPresent()) {
            final List<OWLDataPropertyAssertionAxiom> individualFacts = individualFactsOptional.get();
//                Extract OWLDataProperties from the list of new facts to merge
            final List<OWLDataProperty> filteredFactProperties = individualFacts
                    .stream()
                    .map(fact -> fact.getProperty().asOWLDataProperty())
                    .collect(Collectors.toList());

            // get the facts
            final String individualFactquery = this.qb.buildObjectFactRetrievalQuery(parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC), true, filteredFactProperties, owlNamedIndividual);
            final Single<List<TrestleResult>> currentFactsCompletable = this.ontology.executeSPARQLResults(individualFactquery).toList();

//                    Get object existence information
            final Single<Optional<TemporalObject>> existsTemporalCompletable = readObjectExistence(owlNamedIndividual, !this.mergeEngine.existenceEnabled());

//                Get all the currently valid facts, compare them with the ones present on the object, and update the different ones.
            final Timer.Context compareTimer = this.metrician.registerTimer("trestle-merge-comparison-timer").time();
            final Flowable<MergeScript> mergeScriptPublisher = Single.zip(currentFactsCompletable, existsTemporalCompletable, (currentFacts, existsTemporal) -> this.mergeEngine.mergeFacts(owlNamedIndividual, factTemporal, individualFacts, currentFacts, factTemporal.getIdTemporal(), dTemporal.getIdTemporal(), existsTemporal))
                    .doOnSuccess(success -> compareTimer.stop())
                    .toFlowable().publish().autoConnect(3);

//                Update all the unbounded DB temporals for the diverging facts
            final Completable dbTemporalsCompletable = mergeScriptPublisher
                    .flatMapCompletable(mergeScript -> {
                        logger.trace("Setting DBTo: {} for {}", dTemporal.getIdTemporal(), mergeScript.getFactsToVersion());
                        final String temporalUpdateQuery = this.qb.buildUpdateUnboundedTemporal(parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC), mergeScript.getFactsToVersionAsArray());
                        return this.ontology.executeUpdateSPARQL(temporalUpdateQuery);
                    });


//                Write new versions of all the previously valid facts
            // TODO(nickrobison): This is really bad and inefficient, we should batch up our writes into a single SPARQL execution
            final Completable newVersionCompletable = mergeScriptPublisher
                    .flatMapCompletable(mergeScript -> {
                        return Flowable.fromIterable(mergeScript.getNewFactVersions())
                                .flatMapCompletable(fact -> writeObjectFacts(aClass, owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), dTemporal))
                                //                Write the new valid facts
                                .andThen(Completable.defer(() -> writeObjectFacts(aClass, owlNamedIndividual, mergeScript.getNewFacts(), factTemporal, dTemporal)));
                    });

            final Completable existenceCompletable = mergeScriptPublisher
                    .map(MergeScript::getIndividualExistenceAxioms)
                    .filter(axioms -> !axioms.isEmpty())
                    .flatMapCompletable(axioms -> {
                        final String updateExistenceQuery = this.qb.updateObjectProperties(axioms, trestleObjectIRI);
                        return this.ontology.executeUpdateSPARQL(updateExistenceQuery)
                                //                                Update object events
                                .andThen(Completable.defer(() -> this.eventEngine.adjustObjectEvents(axioms)));
                    });

            return Completable.mergeArray(dbTemporalsCompletable, newVersionCompletable, existenceCompletable)
                    .doOnComplete(mergeTimer::stop);
        }
        mergeTimer.stop();
        return Completable.complete();
    }

    /**
     * Write a Trestle Object without doing any fact merging
     *
     * @param inputObject        - {@link Object} to write
     * @param aClass             - {@link Class} Java class of object
     * @param owlNamedIndividual - {@link OWLNamedIndividual} object name
     * @param dTemporal          - {@link TemporalObject} database temporal to write
     * @param objectTemporal     - {@link TemporalObject} object temporal to write
     * @param factTemporal       - {@link TemporalObject} fact temporal to write
     * @return - {@link Completable} when finished
     */
    private Completable doSimpleObjectWrite(Object inputObject, Class<?> aClass, OWLNamedIndividual owlNamedIndividual, TemporalObject dTemporal, TemporalObject objectTemporal, TemporalObject factTemporal) {
        //        Write the class
        final OWLClass owlClass = this.classParser.getObjectClass(inputObject);
        return ontology.associateOWLClass(owlClass, DATASET_CLASS)
                .andThen(Completable.defer(() -> ontology.createIndividual(owlNamedIndividual, owlClass)))
                .andThen(Completable.defer(() -> {
                    return writeTemporal(objectTemporal, owlNamedIndividual);
                }))
                .andThen(Completable.defer(() -> {
                    final Optional<List<OWLDataPropertyAssertionAxiom>> individualFacts = this.classParser.getFacts(inputObject);
                    return individualFacts.map(owlDataPropertyAssertionAxioms -> writeObjectFacts(aClass, owlNamedIndividual, owlDataPropertyAssertionAxioms, factTemporal, dTemporal)).orElseGet(Completable::complete);
                }))
                .andThen(Completable.defer(() -> this.eventEngine.addEvent(TrestleEventType.CREATED, owlNamedIndividual, objectTemporal.getIdTemporal())))
                .andThen(Completable.defer(() -> {
                    if (!objectTemporal.isContinuing()) {
                        if (objectTemporal.isInterval()) {
                            return this.eventEngine.addEvent(TrestleEventType.DESTROYED, owlNamedIndividual, (Temporal) objectTemporal.asInterval().getToTime().get());
                        } else {
                            return this.eventEngine.addEvent(TrestleEventType.DESTROYED, owlNamedIndividual, objectTemporal.getIdTemporal());
                        }
                    }
                    return Completable.complete();
                }));
    }

    /**
     * Manually add a Fact to a TrestleObject, along with a specific validity period
     * Either a validAt, or the validFrom parameter must be specified
     *
     * @param clazz        - Java class to parse
     * @param individual   - Individual ID
     * @param factName     - Fact name
     * @param value        - Fact value
     * @param validAt      - Optional validAt Temporal
     * @param validFrom    - Optional validFrom Temporal
     * @param validTo      - Optional validTo Temporal
     * @param databaseFrom - Optional databaseFrom Temporal
     * @return {@link Completable} when finished
     */
    @SuppressWarnings({"argument.type.incompatible"})
    private Completable addFactToTrestleObjectImpl(Class<?> clazz, String individual, String factName, Object value, @Nullable Temporal validAt, @Nullable Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        final OWLNamedIndividual owlNamedIndividual = df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, individual));
//        Parse String to Fact IRI
        final Optional<IRI> factIRI = this.classParser.getFactIRI(clazz, factName);
        if (factIRI.isEmpty()) {
            logger.error("Cannot parse {} for individual {}", factName, individual);
            return Completable.error(new TrestleMissingFactException(owlNamedIndividual, parseStringToIRI(this.reasonerPrefix, factName)));
        }
        final OWLDataProperty owlDataProperty = df.getOWLDataProperty(factIRI.get());

//        Validate that we have the correct type
        final Optional<Class<?>> factDatatypeOptional = this.classParser.getFactDatatype(clazz, factName);
        if (factDatatypeOptional.isEmpty()) {
            logger.error("Individual {} does not have fact {}", owlNamedIndividual, owlDataProperty);
            return Completable.error(new TrestleMissingFactException(owlNamedIndividual, factIRI.get()));
        }

//        If the fact datatype is a primitive, then we need to determine if the value class is a primitive, because it'll be boxed by the JVM
        final Class<?> factDatatype = factDatatypeOptional.get();
        Class<?> valueClass = value.getClass();
        if (factDatatype.isPrimitive()) {
            final Class<?> primitiveClass = ClassUtils.wrapperToPrimitive(valueClass);
            if (primitiveClass != null) {
                valueClass = primitiveClass;
            }
        }
        if (!factDatatype.isAssignableFrom(valueClass)) {
            logger.error("Mismatched type. Fact {} has type {}, not {}", factIRI.get(), factDatatype, valueClass);
            return Completable.error(new TrestleMissingFactException(owlNamedIndividual, factIRI.get(), factDatatype, valueClass));
        }


//        Build the temporals
        final TemporalObject validTemporal;
        final TemporalObject databaseTemporal;
        if (validFrom == null && validAt == null) {
            return Completable.error(new IllegalArgumentException("Both validFrom and ValidAt cannot null at the same time"));
        }
        if (validAt != null) {
            validTemporal = TemporalObjectBuilder.valid().at(validAt).build();
        } else if (validTo != null) {
            validTemporal = TemporalObjectBuilder.valid().from(validFrom).to(validTo).build();
        } else {
            validTemporal = TemporalObjectBuilder.valid().from(validFrom).build();
        }
        if (databaseFrom != null) {
            databaseTemporal = TemporalObjectBuilder.database().from(databaseFrom).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().from(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)).build();
        }

//        Ensure we handle spatial properties correctly
        final OWLDatatype datatypeFromJavaClass;
        final OWLLiteral parsedLiteral;
        if (owlDataProperty.getIRI().toString().contains(GEOSPARQLPREFIX)) {
            parsedLiteral = this.classBuilder.getProjectedWKT(clazz, value, null);
        } else {
            datatypeFromJavaClass = this.typeConverter.getDatatypeFromJavaClass(valueClass);
            parsedLiteral = df.getOWLLiteral(value.toString(), datatypeFromJavaClass);
        }
        final OWLDataPropertyAssertionAxiom newFactAxiom = df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, parsedLiteral);

//        Find existing facts
        try {
            final String validFactQuery = this.qb.buildObjectFactRetrievalQuery(parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), true, Collections.singletonList(owlDataProperty), owlNamedIndividual);
            final Single<List<TrestleResult>> trestleResultFlowable = this.ontology.executeSPARQLResults(validFactQuery).toList();

//                    Get object existence information
            Single<Optional<TemporalObject>> existsFlowable = readObjectExistence(owlNamedIndividual,
                    !this.mergeEngine.existenceEnabled());

            return Single.zip(trestleResultFlowable, existsFlowable, (validFactResultSet, existsTemporal) -> {
                final MergeScript newFactMergeScript = this.mergeEngine.mergeFacts(owlNamedIndividual, validTemporal, Collections.singletonList(newFactAxiom), validFactResultSet, validTemporal.getIdTemporal(), databaseTemporal.getIdTemporal(), existsTemporal);

                final String update = this.qb.buildUpdateUnboundedTemporal(parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), newFactMergeScript.getFactsToVersionAsArray());
                final Completable dbUpdateCompletable = this.ontology.executeUpdateSPARQL(update);

                //        Write the new versions
                final Completable newFactVerionCompletable = Flowable.fromIterable(newFactMergeScript.getNewFactVersions())
                        .flatMapCompletable(fact -> writeObjectFacts(clazz, owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), fact.getDbTemporal()));

                // Write new facts
                final Completable newFactsCompletable = writeObjectFacts(clazz, owlNamedIndividual, newFactMergeScript.getNewFacts(), validTemporal, databaseTemporal);

                final List<OWLDataPropertyAssertionAxiom> individualExistenceAxioms = newFactMergeScript.getIndividualExistenceAxioms();
                final String updateExistenceQuery = this.qb.updateObjectProperties(individualExistenceAxioms, trestleObjectIRI);
                final Completable existenceCompletable = this.ontology.executeUpdateSPARQL(updateExistenceQuery)
                        .andThen(Completable.defer(() -> this.eventEngine.adjustObjectEvents(individualExistenceAxioms)));

                return Completable.mergeArray(dbUpdateCompletable, newFactsCompletable, newFactVerionCompletable, existenceCompletable);
            }).flatMapCompletable(val -> val);
        } catch (RuntimeException e) {
            logger.error("Unable to add fact {} to object {}", factName, owlNamedIndividual, e);
            return Completable.error(e);
        }
    }

    /**
     * Writes a data property as an asserted fact for an individual TS_Object.
     *
     * @param rootIndividual   - OWLNamedIndividual of the TS_Object individual
     * @param properties       - List of OWLDataPropertyAssertionAxioms to write as Facts
     * @param validTemporal    - Temporal to associate with data property individual
     * @param databaseTemporal - Temporal representing database time
     * @return {@link Completable} when finished
     */
    @Timed
    private Completable writeObjectFacts(Class<?> clazz, OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        final OWLClass factClass = df.getOWLClass(factClassIRI);

        return Observable.fromIterable(properties)
                .flatMapCompletable(property -> {
                    final TrestleIRI factIdentifier = IRIBuilder.encodeIRI(V1,
                            this.reasonerPrefix,
                            rootIndividual.toStringID(),
                            property.getProperty().asOWLDataProperty().getIRI().toString(),
                            parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                            parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

                    final OWLNamedIndividual propertyIndividual = df.getOWLNamedIndividual(factIdentifier);
                    return ontology.createIndividual(propertyIndividual, factClass)
                            .andThen(Completable.defer(() -> {
                                logger.debug("Writing fact {} with value {} valid: {}, database: {}", factIdentifier, property.getObject(), validTemporal, databaseTemporal);
                                return ontology.writeIndividualDataProperty(propertyIndividual, property.getProperty().asOWLDataProperty(), property.getObject())
                                        .andThen(Completable.defer(() -> {
                                            //                Write the valid validTemporal
                                            return writeTemporal(validTemporal, propertyIndividual)
                                                    //                Write the relation back to the root individual
                                                    .andThen(Completable.defer(() -> ontology.writeIndividualObjectProperty(propertyIndividual, factOfIRI, rootIndividual)));
                                        }))
                                        .andThen(Completable.defer(() -> {
                                            //                Write the database time
                                            return writeTemporal(databaseTemporal, propertyIndividual);
                                        }))
                                        .andThen(Completable.defer(() -> {
                                            // Write any contributes_to relationships
                                            if (this.classParser.isFactRelated(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm())) {

                                                final String contributesToQuery = this.qb.buildContributesToQuery(rootIndividual, property);
                                                return this.ontology.executeUpdateSPARQL(contributesToQuery);
                                            }
                                            return Completable.complete();
                                        }));
                            }));
                });
    }

    private Completable writeTemporal(TemporalObject temporal, OWLNamedIndividual individual) {
//        Write the object
//        final IRI temporalIRI = IRI.create(this.reasonerPrefix, temporal.getID());
//        ontology.createIndividual(temporalIRI, temporalClassIRI);
        TemporalScope scope = temporal.getScope();
        TemporalType type = temporal.getType();

//        if (overrideTemporalScope != null) {
//            scope = overrideTemporalScope;
//        }

//        Write the properties using the scope and type variables set above
        if (type == TemporalType.INTERVAL) {
            if (scope == TemporalScope.VALID) {
//                Write from
                return ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        temporalValidFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI)
                        .andThen(Completable.defer(() -> {
                            //                Write to, if exists
                            final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                            if (toTime.isPresent()) {
                                return ontology.writeIndividualDataProperty(
                                        individual.getIRI(),
                                        temporalValidToIRI,
                                        parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                        dateTimeDatatypeIRI);
                            }
                            return Completable.complete();
                        }));
            } else if (scope == TemporalScope.DATABASE) {
                //                Write from
                return ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        temporalDatabaseFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI)
                        .andThen(Completable.defer(() -> {
                            //                Write to, if exists
                            final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                            if (toTime.isPresent()) {
                                return ontology.writeIndividualDataProperty(
                                        individual.getIRI(),
                                        temporalDatabaseToIRI,
                                        parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                        dateTimeDatatypeIRI);
                            }
                            return Completable.complete();
                        }));
            } else {
//                Write from
                return ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI)
                        .andThen(Completable.defer(() -> {
                            //                Write to, if exists
                            final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                            if (toTime.isPresent()) {
                                return ontology.writeIndividualDataProperty(
                                        individual.getIRI(),
                                        StaticIRI.temporalExistsToIRI,
                                        parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                        dateTimeDatatypeIRI);
                            }
                            return Completable.complete();
                        }));
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                return ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalValidAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            } else if (scope == TemporalScope.DATABASE) {
                logger.warn("Database time cannot be a point {}", individual);
            } else {
                return ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            }
        }

        return Completable.error(() -> new IllegalStateException("Unable to write temporal"));
    }

    /**
     * Read object existence {@link TemporalObject} for the given {@link OWLNamedIndividual}, unless we bypass it, then just return an empty optional
     *
     * @param individual - {@link OWLNamedIndividual} to read
     * @param bypass     - {@code true} bypass execution and return {@link Optional#empty()}
     * @return - {@link Maybe} of {@link TemporalObject} provided existence interval of the individual
     */
    private Single<Optional<TemporalObject>> readObjectExistence(OWLNamedIndividual individual, boolean bypass) {
        if (!bypass) {
            return this.ontology.getAllDataPropertiesForIndividual(individual).collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add)
                    .map(properties -> TemporalObjectBuilder.buildTemporalFromProperties(properties, OffsetDateTime.class, null, null));
        }
        return Single.just(Optional.empty());
    }

    /**
     * Write an indirect object property between a Java object and an intermediate OWL individual
     * The OWL individual must exist before calling this function, but the Java object is created if it doesn't exist.
     *
     * @param subject  - {@link OWLNamedIndividual} of intermediate OWL object
     * @param object   - Java {@link Object} to write as object of assertion
     * @param property - {@link OWLObjectProperty} to assert
     * @return {@link Completable} when finished
     */
    private Completable writeIndirectObjectProperty(OWLNamedIndividual subject, Object object, OWLObjectProperty property) {
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = df.getOWLObjectPropertyAssertionAxiom(property, subject, objectIndividual);
        return this.ontology.writeIndividualObjectProperty(owlObjectPropertyAssertionAxiom);
    }


    /**
     * Write an object property assertion between two objects, writing them into the database if they don't exist.
     *
     * @param subject     - Java {@link Object} to write as subject of assertion
     * @param object      - Java {@link Object} to write as object of assertion
     * @param property    - {@link OWLObjectProperty} to assert between the two objects
     * @param transaction - {@link TrestleTransaction} optional transaction to continue with
     * @return {@link Completable} - when finished
     */
    private Completable writeObjectProperty(Object subject, Object object, OWLObjectProperty property, @Nullable TrestleTransaction transaction) {
        logger.debug("Writing relationship {} between {} (subject) and {} (object)", property, subject, object);
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom objectRelationshipAssertion = df.getOWLObjectPropertyAssertionAxiom(property,
                subjectIndividual,
                objectIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(transaction, true);
        return this.ontology.writeIndividualObjectProperty(objectRelationshipAssertion)
                .doOnError(err -> this.ontology.returnAndAbortTransaction(trestleTransaction))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }
}
