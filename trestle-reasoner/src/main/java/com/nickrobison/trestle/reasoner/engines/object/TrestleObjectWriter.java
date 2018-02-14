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
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventException;
import com.nickrobison.trestle.reasoner.engines.merge.MergeScript;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.TypeConverter;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.common.StaticIRI.factOfIRI;
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
    private final TemporalParser temporalParser;
    private final TrestleMergeEngine mergeEngine;
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final TrestleCache trestleCache;
    private final String reasonerPrefix;


    @Inject
    public TrestleObjectWriter(TrestleEventEngine eventEngine,
                               Metrician metrician,
                               ObjectEngineUtils engineUtils,
                               TrestleParser trestleParser,
                               TrestleMergeEngine mergeEngine,
                               ITrestleOntology ontology,
                               QueryBuilder queryBuilder,
                               TrestleCache trestleCache,
                               @Named("reasonerPrefix") String reasonerPrefix) {
        this.eventEngine = eventEngine;
        this.metrician = metrician;
        this.engineUtils = engineUtils;
        this.classParser = trestleParser.classParser;
        this.temporalParser = trestleParser.temporalParser;
        this.mergeEngine = mergeEngine;
        this.ontology = ontology;
        this.qb = queryBuilder;
        this.trestleCache = trestleCache;
        this.reasonerPrefix = reasonerPrefix;

        final Config config = ConfigFactory.load().getConfig("trestle");

        this.objectWriterThreadPool = TrestleExecutorService.executorFactory(
                "object-writer-pool",
                config.getInt("threading.object-pool.size"),
                this.metrician);
    }

    @Override
    public void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        writeTrestleObjectImpl(inputObject, null);
    }

    @Override
    public void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).to(endTemporal).build();
        }
        writeTrestleObjectImpl(inputObject, databaseTemporal);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        addFactToTrestleObjectImpl(clazz, individual, factName, value, validAt, null, null, databaseFrom);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        addFactToTrestleObjectImpl(clazz, individual, factName, value, null, validFrom, validTo, databaseFrom);
    }

    @Override
    public <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength) {
        if (!(type == TrestleEventType.SPLIT) && !(type == TrestleEventType.MERGED)) {
            throw new IllegalArgumentException("Only MERGED and SPLIT types are valid");
        }

        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
//        Build the event name
        final OWLNamedIndividual eventIndividual = TrestleEventEngine.buildEventName(df, this.reasonerPrefix, subjectIndividual, type);
//        Get the event temporal to use, and just grab the first one
        final Optional<List<TemporalObject>> temporalObjects = this.temporalParser.getTemporalObjects(subject);
        final TemporalObject subjectTemporal = temporalObjects.orElseThrow(() -> new IllegalStateException("Cannot get temporals for individual")).get(0);
        final Temporal eventTemporal;
        if (type == TrestleEventType.SPLIT) {
//            If it's a split, grab the ending temporal
            if (subjectTemporal.isPoint()) {
                eventTemporal = subjectTemporal.getIdTemporal();
            } else if (subjectTemporal.isInterval() && !subjectTemporal.isContinuing()) {
                eventTemporal = (Temporal) subjectTemporal.asInterval().getToTime().get();
            } else {
                throw new IllegalArgumentException(String.format("Cannot add event to continuing object %s", subjectIndividual.toStringID()));
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
        try {
            this.writeTrestleObject(subject);
            for (T object : objects) {
                writeTrestleObject(object);
            }
//            Add the event
            this.eventEngine.addSplitMergeEvent(type, subjectIndividual, objectIndividuals, eventTemporal);
//            Write the strength
            this.ontology.writeIndividualDataProperty(eventIndividual, df.getOWLDataProperty(relationStrengthIRI), df.getOWLLiteral(strength));
        } catch (TrestleClassException | MissingOntologyEntity e) {
            logger.error("Unable to add individuals", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } catch (TrestleEventException e) {
            logger.error("Unable add Event", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    /**
     * Writes an object into the ontology using the object's temporal scope
     * If a temporal is provided it uses that for the database time interval
     *
     * @param inputObject      - Object to write to the ontology
     * @param databaseTemporal - Optional TemporalObject to manually set database time
     */
    @Timed
    @Metered(name = "trestle-object-write", absolute = true)
    private void writeTrestleObjectImpl(Object inputObject, @Nullable TemporalObject databaseTemporal) throws UnregisteredClassException, MissingOntologyEntity {
        final Class aClass = inputObject.getClass();
        if (!this.engineUtils.checkRegisteredClass(aClass)) {
            throw new UnregisteredClassException(aClass);
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
        TemporalObject objectTemporal = temporalObjects.orElseThrow(() -> new RuntimeException(String.format("Cannot parse temporals for %s", owlNamedIndividual))).get(0);
        TemporalObject factTemporal = objectTemporal.castTo(TemporalScope.VALID);

//        Merge operation, if the object exists
        // temporal merging occurs by default but may be disabled in the configuration
        if (this.mergeEngine.mergeOnLoad() && this.engineUtils.checkExists(owlNamedIndividual.getIRI())) {
            final Timer.Context mergeTimer = this.metrician.registerTimer("trestle-merge-timer").time();
            final Optional<List<OWLDataPropertyAssertionAxiom>> individualFactsOptional = this.classParser.getFacts(inputObject);
//            Open transaction
            final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(true);
            try {

//            Get all the currently valid facts
                if (individualFactsOptional.isPresent()) {
                    final List<OWLDataPropertyAssertionAxiom> individualFacts = individualFactsOptional.get();
//                Extract OWLDataProperties from the list of new facts to merge
                    final List<OWLDataProperty> filteredFactProperties = individualFacts
                            .stream()
                            .map(fact -> fact.getProperty().asOWLDataProperty())
                            .collect(Collectors.toList());

                    final CompletableFuture<TrestleResultSet> factsFuture = CompletableFuture.supplyAsync(() -> {
                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                        try {
                            final String individualFactquery = this.qb.buildObjectFactRetrievalQuery(parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC), true, filteredFactProperties, owlNamedIndividual);
                            return this.ontology.executeSPARQLResults(individualFactquery);
                        } finally {
                            this.ontology.returnAndCommitTransaction(tt);
                        }
                    }, this.objectWriterThreadPool);

//                    Get object existence information
                    @SuppressWarnings("Duplicates") final CompletableFuture<Optional<TemporalObject>> existsFuture = readObjectExistence(owlNamedIndividual, trestleTransaction, this.objectWriterThreadPool, !this.mergeEngine.existenceEnabled());


                    final CompletableFuture<Void> mergeFuture = factsFuture.thenAcceptBothAsync(existsFuture, (resultSet, existsTemporal) -> {

                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);

                        try {
//                Get all the currently valid facts, compare them with the ones present on the object, and update the different ones.
                            final Timer.Context compareTimer = this.metrician.registerTimer("trestle-merge-comparison-timer").time();
                            final List<TrestleResult> currentFacts = resultSet.getResults();
                            final MergeScript mergeScript = this.mergeEngine.mergeFacts(owlNamedIndividual, factTemporal, individualFacts, currentFacts, factTemporal.getIdTemporal(), dTemporal.getIdTemporal(), existsTemporal);
                            compareTimer.stop();

//                Update all the unbounded DB temporals for the diverging facts
                            logger.trace("Setting DBTo: {} for {}", dTemporal.getIdTemporal(), mergeScript.getFactsToVersion());
                            final String temporalUpdateQuery = this.qb.buildUpdateUnboundedTemporal(TemporalParser.parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC), mergeScript.getFactsToVersionAsArray());
                            final Timer.Context temporalTimer = this.metrician.registerTimer("trestle-merge-temporal-timer").time();
                            this.ontology.executeUpdateSPARQL(temporalUpdateQuery);
                            temporalTimer.stop();
//                Write new versions of all the previously valid facts
                            mergeScript
                                    .getNewFactVersions()
                                    .forEach(fact -> writeObjectFacts(owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), dTemporal));
//                Write the new valid facts
                            final Timer.Context factsTimer = this.metrician.registerTimer("trestle-merge-facts-timer").time();
                            writeObjectFacts(owlNamedIndividual, mergeScript.getNewFacts(), factTemporal, dTemporal);
                            factsTimer.stop();

//                    Write new individual existence axioms, if they exist
                            if (!mergeScript.getIndividualExistenceAxioms().isEmpty()) {
                                final String updateExistenceQuery = this.qb.updateObjectProperties(mergeScript.getIndividualExistenceAxioms(), trestleObjectIRI);
                                this.ontology.executeUpdateSPARQL(updateExistenceQuery);

//                                Update object events
                                this.eventEngine.adjustObjectEvents(mergeScript.getIndividualExistenceAxioms());
                            }
                        } finally {
                            this.ontology.returnAndCommitTransaction(tt);
                        }
                    }, this.objectWriterThreadPool);
                    mergeFuture.join();

                }
            } catch (RuntimeException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Error while writing object {}", owlNamedIndividual, e);
//                recoverExceptionType(e, TrestleMergeConflict.class, TrestleMergeException.class);
                ExceptionUtils.rethrow(e.getCause());
                mergeTimer.stop();
            } finally {
                ontology.returnAndCommitTransaction(trestleTransaction);
                mergeTimer.stop();
            }
        } else {
//        If the object doesn't exist, continue with the simple write

//        Write the class
            final OWLClass owlClass = this.classParser.getObjectClass(inputObject);
//        Open the transaction
            final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(true);
            try {
                ontology.associateOWLClass(owlClass, DATASET_CLASS);
//        Write the individual
                ontology.createIndividual(owlNamedIndividual, owlClass);
                writeTemporal(objectTemporal, owlNamedIndividual);

//        Write the data facts
                final Optional<List<OWLDataPropertyAssertionAxiom>> individualFacts = this.classParser.getFacts(inputObject);
                individualFacts.ifPresent(owlDataPropertyAssertionAxioms -> writeObjectFacts(owlNamedIndividual, owlDataPropertyAssertionAxioms, factTemporal, dTemporal));

//            Add object events
                this.eventEngine.addEvent(TrestleEventType.CREATED, owlNamedIndividual, objectTemporal.getIdTemporal());
                if (!objectTemporal.isContinuing()) {
                    if (objectTemporal.isInterval()) {
                        this.eventEngine.addEvent(TrestleEventType.DESTROYED, owlNamedIndividual, (Temporal) objectTemporal.asInterval().getToTime().get());
                    } else {
                        this.eventEngine.addEvent(TrestleEventType.DESTROYED, owlNamedIndividual, objectTemporal.getIdTemporal());
                    }
                }
            } catch (RuntimeException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Error while writing object {}", owlNamedIndividual, e);
            } finally {
                ontology.returnAndCommitTransaction(trestleTransaction);
            }
        }

//        Invalidate the cache
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, owlNamedIndividual.toStringID(), null,
                parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC));
        logger.debug("Purging {} from the cache", individualIRI);
        trestleCache.deleteTrestleObject(individualIRI);
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
     */
    @SuppressWarnings({"argument.type.incompatible"})
    private void addFactToTrestleObjectImpl(Class<?> clazz, String individual, String factName, Object value, @Nullable Temporal validAt, @Nullable Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        final OWLNamedIndividual owlNamedIndividual = df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, individual));
//        Parse String to Fact IRI
        final Optional<IRI> factIRI = this.classParser.getFactIRI(clazz, factName);
        if (!factIRI.isPresent()) {
            logger.error("Cannot parse {} for individual {}", factName, individual);
            throw new TrestleMissingFactException(owlNamedIndividual, parseStringToIRI(this.reasonerPrefix, factName));
        }
        final OWLDataProperty owlDataProperty = df.getOWLDataProperty(factIRI.get());

//        Validate that we have the correct type
        final Optional<Class<?>> factDatatypeOptional = this.classParser.getFactDatatype(clazz, factName);
        if (!factDatatypeOptional.isPresent()) {
            logger.error("Individual {} does not have fact {}", owlNamedIndividual, owlDataProperty);
            throw new TrestleMissingFactException(owlNamedIndividual, factIRI.get());
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
            throw new TrestleMissingFactException(owlNamedIndividual, factIRI.get(), factDatatype, valueClass);
        }


//        Build the temporals
        final TemporalObject validTemporal;
        final TemporalObject databaseTemporal;
        if (validFrom == null && validAt == null) {
            throw new IllegalArgumentException("Both validFrom and ValidAt cannot null at the same time");
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
        if (owlDataProperty.getIRI().toString().contains(GEOSPARQLPREFIX)) {
            datatypeFromJavaClass = df.getOWLDatatype(WKTDatatypeIRI);
        } else {
            datatypeFromJavaClass = TypeConverter.getDatatypeFromJavaClass(valueClass);
        }
        final OWLDataPropertyAssertionAxiom newFactAxiom = df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, df.getOWLLiteral(value.toString(), datatypeFromJavaClass));

//        Find existing facts
//        final String validFactQuery = this.qb.buildCurrentlyValidFactQuery(owlNamedIndividual, owlDataProperty, parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));
        final String validFactQuery = this.qb.buildObjectFactRetrievalQuery(parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), true, Collections.singletonList(owlDataProperty), owlNamedIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {

            final CompletableFuture<TrestleResultSet> factsFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                try {
                    return this.ontology.executeSPARQLResults(validFactQuery);
                } finally {
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, this.objectWriterThreadPool);


//                    Get object existence information
            @SuppressWarnings("Duplicates") final CompletableFuture<Optional<TemporalObject>> existsFuture = readObjectExistence(owlNamedIndividual,
                    trestleTransaction,
                    this.objectWriterThreadPool,
                    !this.mergeEngine.existenceEnabled());

            final CompletableFuture<Void> mergeFuture = factsFuture.thenAcceptBothAsync(existsFuture, (validFactResultSet, existsTemporal) -> {

                final MergeScript newFactMergeScript = this.mergeEngine.mergeFacts(owlNamedIndividual, validTemporal, Collections.singletonList(newFactAxiom), validFactResultSet.getResults(), validTemporal.getIdTemporal(), databaseTemporal.getIdTemporal(), existsTemporal);
                final String update = this.qb.buildUpdateUnboundedTemporal(parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), newFactMergeScript.getFactsToVersionAsArray());
                this.ontology.executeUpdateSPARQL(update);

//        Write the new versions
                newFactMergeScript
                        .getNewFactVersions()
                        .forEach(fact -> writeObjectFacts(owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), fact.getDbTemporal()));

//        Write the new fact versions
                writeObjectFacts(owlNamedIndividual, newFactMergeScript.getNewFacts(), validTemporal, databaseTemporal);

//                Write the new existence axioms, if they exist
                final List<OWLDataPropertyAssertionAxiom> individualExistenceAxioms = newFactMergeScript.getIndividualExistenceAxioms();
                if (!individualExistenceAxioms.isEmpty()) {
                    final String updateExistenceQuery = this.qb.updateObjectProperties(individualExistenceAxioms, trestleObjectIRI);
                    this.ontology.executeUpdateSPARQL(updateExistenceQuery);

//                    Update events
                    this.eventEngine.adjustObjectEvents(individualExistenceAxioms);
                }

            }, this.objectWriterThreadPool);
            mergeFuture.join();
        } catch (RuntimeException e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            logger.error("Unable to add fact {} to object {}", factName, owlNamedIndividual, e);
//            recoverExceptionType(e, TrestleMergeConflict.class, TrestleMergeException.class);
            ExceptionUtils.rethrow(e.getCause());
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }


//        Update the cache
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, individual, null,
                parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));
        trestleCache.deleteTrestleObject(individualIRI);
    }

    /**
     * Writes a data property as an asserted fact for an individual TS_Object.
     *
     * @param rootIndividual   - OWLNamedIndividual of the TS_Object individual
     * @param properties       - List of OWLDataPropertyAssertionAxioms to write as Facts
     * @param validTemporal    - Temporal to associate with data property individual
     * @param databaseTemporal - Temporal representing database time
     */
    @Timed
    private void writeObjectFacts(OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        final OWLClass factClass = df.getOWLClass(factClassIRI);
        properties.forEach(property -> {
            final TrestleIRI factIdentifier = IRIBuilder.encodeIRI(V1,
                    this.reasonerPrefix,
                    rootIndividual.toStringID(),
                    property.getProperty().asOWLDataProperty().getIRI().toString(),
                    parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                    parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

            final OWLNamedIndividual propertyIndividual = df.getOWLNamedIndividual(factIdentifier);
            ontology.createIndividual(propertyIndividual, factClass);
            try {
//                Write the property
                logger.trace("Writing fact {} with value {} valid: {}, database: {}", factIdentifier, property.getObject(), validTemporal, databaseTemporal);
                ontology.writeIndividualDataProperty(propertyIndividual, property.getProperty().asOWLDataProperty(), property.getObject());
//                Write the valid validTemporal
                writeTemporal(validTemporal, propertyIndividual);
//                Write the relation back to the root individual
                ontology.writeIndividualObjectProperty(propertyIndividual, factOfIRI, rootIndividual);
//                Write the database time
                writeTemporal(databaseTemporal, propertyIndividual);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            }
        });
    }

    private void writeTemporal(TemporalObject temporal, OWLNamedIndividual individual) throws MissingOntologyEntity {
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
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        temporalValidFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            temporalValidToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            } else if (scope == TemporalScope.DATABASE) {
                //                Write from
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        temporalDatabaseFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            temporalDatabaseToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            } else {
//                Write from
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            StaticIRI.temporalExistsToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalValidAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            } else if (scope == TemporalScope.DATABASE) {
                logger.warn("Database time cannot be a point {}", individual);
            } else {
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            }
        }
    }

    /**
     * Read object existence {@link TemporalObject} for the given {@link OWLNamedIndividual}, unless we bypass it, then just return an empty optional
     *
     * @param individual  - {@link OWLNamedIndividual} to read
     * @param transaction - Execute as part of provided {@link TrestleTransaction}
     * @param executor    - Execute within provided {@link ExecutorService}
     * @param bypass      - {@code true} bypass execution and return {@link Optional#empty()}
     * @return - {@link Optional} of {@link TemporalObject} provided existence interval of the individual
     */
    private CompletableFuture<Optional<TemporalObject>> readObjectExistence(OWLNamedIndividual individual, @Nullable TrestleTransaction transaction, ExecutorService executor, boolean bypass) {
        if (!bypass) {
            return CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transaction);
                try {
                    final Set<OWLDataPropertyAssertionAxiom> individualExistenceProperties = this.ontology.getAllDataPropertiesForIndividual(individual);
                    return TemporalObjectBuilder.buildTemporalFromProperties(individualExistenceProperties, OffsetDateTime.class, null, null);
                } finally {
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, executor);
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
