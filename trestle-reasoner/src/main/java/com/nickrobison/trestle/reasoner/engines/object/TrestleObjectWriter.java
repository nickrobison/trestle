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
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventException;
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
import io.reactivex.rxjava3.functions.Supplier;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    public void writeTrestleObject(Object inputObject) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            writeTrestleObjectImpl(inputObject, null);
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (Exception e) {
            logger.error("Unable to write object.", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).to(endTemporal).build();
        }
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            writeTrestleObjectImpl(inputObject, databaseTemporal);
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (Exception e) {
            logger.error("Unable to write object.", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            addFactToTrestleObjectImpl(clazz, individual, factName, value, validAt, null, null, databaseFrom);
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (RuntimeException e) {
            logger.error("Unable to write object.", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            addFactToTrestleObjectImpl(clazz, individual, factName, value, null, validFrom, validTo, databaseFrom);
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (RuntimeException e) {
            logger.error("Unable to write object.", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            ExceptionUtils.rethrow(e);
        }
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
            this.ontology.writeIndividualDataProperty(eventIndividual, df.getOWLDataProperty(relationStrengthIRI), df.getOWLLiteral(strength)).blockingAwait();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (TrestleEventException e) {
            logger.error("Unable add Event", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } catch (Exception e) {
            logger.error("Unable to add individuals", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        }
    }

    @Override
    public void writeObjectRelationship(Object subject, Object object, ObjectRelation relation) {
        this.writeObjectProperty(subject, object, df.getOWLObjectProperty(relation.getIRI()));
    }

    @Override
    public void writeSpatialOverlap(Object subject, Object object, String wkt) {
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(this.reasonerPrefix,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
        this.ontology.createIndividual(overlapClassAssertion).blockingAwait();
//        Write the overlap intersection
        final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(sOverlapIRI), overlapIndividual, df.getOWLLiteral(wkt, df.getOWLDatatype(WKTDatatypeIRI)));
        this.ontology.writeIndividualDataProperty(sOverlapAssertion).blockingAwait();

//        Write the subject relation
        final OWLObjectProperty overlapProperty = df.getOWLObjectProperty(overlapOfIRI);
        this.writeIndirectObjectProperty(overlapIndividual, subject, overlapProperty);

//        Write the object relation
        this.writeIndirectObjectProperty(overlapIndividual, object, overlapProperty);
    }

    //    TODO(nrobison): Correctly implement this
    @Override
    public void writeTemporalOverlap(Object subject, Object object, String temporalOverlap) {
        logger.warn("Temporal overlaps not implemented yet, overlap value has no meaning");
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(this.reasonerPrefix,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            this.ontology.createIndividual(overlapClassAssertion).blockingAwait();

            //        Write the overlap intersection
            final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(tOverlapIRI), overlapIndividual, df.getOWLLiteral(temporalOverlap));
            this.ontology.writeIndividualDataProperty(sOverlapAssertion).blockingAwait();
            //        Write the subject relation
            final OWLObjectProperty overlapProperty = df.getOWLObjectProperty(overlapOfIRI);
            this.writeIndirectObjectProperty(overlapIndividual, subject, overlapProperty);

//        Write the object relation
            this.writeIndirectObjectProperty(overlapIndividual, object, overlapProperty);
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        } catch (Exception e) {
            logger.error("Unable to write overlap", e);
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
        final Class<?> aClass = inputObject.getClass();
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
            try {
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
                    final List<TrestleResult> currentFacts = this.ontology.executeSPARQLResults(individualFactquery).toList().blockingGet();

//                    Get object existence information
                    final Optional<TemporalObject> existsTemporal = readObjectExistence(owlNamedIndividual, !this.mergeEngine.existenceEnabled());

//                Get all the currently valid facts, compare them with the ones present on the object, and update the different ones.
                    final Timer.Context compareTimer = this.metrician.registerTimer("trestle-merge-comparison-timer").time();
                    final MergeScript mergeScript = this.mergeEngine.mergeFacts(owlNamedIndividual, factTemporal, individualFacts, currentFacts, factTemporal.getIdTemporal(), dTemporal.getIdTemporal(), existsTemporal);
                    compareTimer.stop();

//                Update all the unbounded DB temporals for the diverging facts
                    logger.trace("Setting DBTo: {} for {}", dTemporal.getIdTemporal(), mergeScript.getFactsToVersion());
                    final String temporalUpdateQuery = this.qb.buildUpdateUnboundedTemporal(TemporalParser.parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC), mergeScript.getFactsToVersionAsArray());
                    final Timer.Context temporalTimer = this.metrician.registerTimer("trestle-merge-temporal-timer").time();
                    this.ontology.executeUpdateSPARQL(temporalUpdateQuery).blockingAwait();
                    temporalTimer.stop();
//                Write new versions of all the previously valid facts
                    mergeScript
                            .getNewFactVersions()
                            .forEach(fact -> writeObjectFacts(aClass, owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), dTemporal));
//                Write the new valid facts
                    try (Timer.Context factsTimer = this.metrician.registerTimer("trestle-merge-facts-timer").time()) {
                        writeObjectFacts(aClass, owlNamedIndividual, mergeScript.getNewFacts(), factTemporal, dTemporal);
                        factsTimer.stop();
                    }

//                    Write new individual existence axioms, if they exist
                    if (!mergeScript.getIndividualExistenceAxioms().isEmpty()) {
                        final String updateExistenceQuery = this.qb.updateObjectProperties(mergeScript.getIndividualExistenceAxioms(), trestleObjectIRI);
                        this.ontology.executeUpdateSPARQL(updateExistenceQuery).blockingAwait();

//                                Update object events
                        this.eventEngine.adjustObjectEvents(mergeScript.getIndividualExistenceAxioms());
                    }

                }
            } finally {
                mergeTimer.stop();
            }
        } else { //        If the object doesn't exist, continue with the simple write
//        Write the class
            final OWLClass owlClass = this.classParser.getObjectClass(inputObject);
            try {
                ontology.associateOWLClass(owlClass, DATASET_CLASS).blockingAwait();
//        Write the individual
                ontology.createIndividual(owlNamedIndividual, owlClass).blockingAwait();
                writeTemporal(objectTemporal, owlNamedIndividual);

//        Write the data facts
                final Optional<List<OWLDataPropertyAssertionAxiom>> individualFacts = this.classParser.getFacts(inputObject);
                individualFacts.ifPresent(owlDataPropertyAssertionAxioms -> writeObjectFacts(aClass, owlNamedIndividual, owlDataPropertyAssertionAxioms, factTemporal, dTemporal));

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
                logger.error("Error while writing object {}", owlNamedIndividual, e);
                ExceptionUtils.rethrow(e.getCause());
            }
        }

//        Invalidate the cache and tracker
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, owlNamedIndividual.toStringID(), null,
                parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC));
        logger.debug("Purging {} from the cache", individualIRI);
        trestleCache.deleteTrestleObject(individualIRI);
        this.relationTracker.removeComputedRelations(owlNamedIndividual);
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
        final OWLLiteral parsedLiteral;
        if (owlDataProperty.getIRI().toString().contains(GEOSPARQLPREFIX)) {
            parsedLiteral = this.classBuilder.getProjectedWKT(clazz, value, null);
        } else {
            datatypeFromJavaClass = this.typeConverter.getDatatypeFromJavaClass(valueClass);
            parsedLiteral = df.getOWLLiteral(value.toString(), datatypeFromJavaClass);
        }
        final OWLDataPropertyAssertionAxiom newFactAxiom = df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, parsedLiteral);

//        Find existing facts
//        final String validFactQuery = this.qb.buildCurrentlyValidFactQuery(owlNamedIndividual, owlDataProperty, parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

        try {
            final String validFactQuery = this.qb.buildObjectFactRetrievalQuery(parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC), parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), true, Collections.singletonList(owlDataProperty), owlNamedIndividual);
            final List<TrestleResult> validFactResultSet = this.ontology.executeSPARQLResults(validFactQuery).toList().blockingGet();

//                    Get object existence information
            Optional<TemporalObject> existsTemporal = readObjectExistence(owlNamedIndividual,
                    !this.mergeEngine.existenceEnabled());

            final MergeScript newFactMergeScript = this.mergeEngine.mergeFacts(owlNamedIndividual, validTemporal, Collections.singletonList(newFactAxiom), validFactResultSet, validTemporal.getIdTemporal(), databaseTemporal.getIdTemporal(), existsTemporal);
            final String update = this.qb.buildUpdateUnboundedTemporal(parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC), newFactMergeScript.getFactsToVersionAsArray());
            this.ontology.executeUpdateSPARQL(update).blockingAwait();

//        Write the new versions
            newFactMergeScript
                    .getNewFactVersions()
                    .forEach(fact -> writeObjectFacts(clazz, owlNamedIndividual, Collections.singletonList(fact.getAxiom()), fact.getValidTemporal(), fact.getDbTemporal()));

//        Write the new fact versions
            writeObjectFacts(clazz, owlNamedIndividual, newFactMergeScript.getNewFacts(), validTemporal, databaseTemporal);

//                Write the new existence axioms, if they exist
            final List<OWLDataPropertyAssertionAxiom> individualExistenceAxioms = newFactMergeScript.getIndividualExistenceAxioms();
            if (!individualExistenceAxioms.isEmpty()) {
                final String updateExistenceQuery = this.qb.updateObjectProperties(individualExistenceAxioms, trestleObjectIRI);
                this.ontology.executeUpdateSPARQL(updateExistenceQuery).blockingAwait();

//                    Update events
                this.eventEngine.adjustObjectEvents(individualExistenceAxioms);
            }

        } catch (RuntimeException e) {
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
            logger.error("Unable to add fact {} to object {}", factName, owlNamedIndividual, e);
            throw e;
        }


//        Update the cache and tracker
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, this.reasonerPrefix, individual, null,
                parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));
        trestleCache.deleteTrestleObject(individualIRI);
        this.relationTracker.removeComputedRelations(owlNamedIndividual);
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
    private void writeObjectFacts(Class<?> clazz, OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        final OWLClass factClass = df.getOWLClass(factClassIRI);
        properties.forEach(property -> {
            final TrestleIRI factIdentifier = IRIBuilder.encodeIRI(V1,
                    this.reasonerPrefix,
                    rootIndividual.toStringID(),
                    property.getProperty().asOWLDataProperty().getIRI().toString(),
                    parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                    parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

            final OWLNamedIndividual propertyIndividual = df.getOWLNamedIndividual(factIdentifier);
            ontology.createIndividual(propertyIndividual, factClass).blockingAwait();
            try {
//                Write the property
                logger.trace("Writing fact {} with value {} valid: {}, database: {}", factIdentifier, property.getObject(), validTemporal, databaseTemporal);
                ontology.writeIndividualDataProperty(propertyIndividual, property.getProperty().asOWLDataProperty(), property.getObject()).blockingAwait();
//                Write the valid validTemporal
                writeTemporal(validTemporal, propertyIndividual);
//                Write the relation back to the root individual
                ontology.writeIndividualObjectProperty(propertyIndividual, factOfIRI, rootIndividual).blockingAwait();
//                Write the database time
                writeTemporal(databaseTemporal, propertyIndividual);

                // Write any contributes_to relationships
                if (this.classParser.isFactRelated(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm())) {

                    final String contributesToQuery = this.qb.buildContributesToQuery(rootIndividual, property);
                    this.ontology.executeUpdateSPARQL(contributesToQuery).blockingAwait();
                }

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
                        dateTimeDatatypeIRI).blockingAwait();

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            temporalValidToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI).blockingAwait();
                }
            } else if (scope == TemporalScope.DATABASE) {
                //                Write from
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        temporalDatabaseFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI).blockingAwait();

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            temporalDatabaseToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI).blockingAwait();
                }
            } else {
//                Write from
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI).blockingAwait();

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            individual.getIRI(),
                            StaticIRI.temporalExistsToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI).blockingAwait();
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalValidAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI).blockingAwait();
            } else if (scope == TemporalScope.DATABASE) {
                logger.warn("Database time cannot be a point {}", individual);
            } else {
                ontology.writeIndividualDataProperty(
                        individual.getIRI(),
                        StaticIRI.temporalExistsAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI).blockingAwait();
            }
        }
    }

    /**
     * Read object existence {@link TemporalObject} for the given {@link OWLNamedIndividual}, unless we bypass it, then just return an empty optional
     *
     * @param individual - {@link OWLNamedIndividual} to read
     * @param bypass     - {@code true} bypass execution and return {@link Optional#empty()}
     * @return - {@link Optional} of {@link TemporalObject} provided existence interval of the individual
     */
    private Optional<TemporalObject> readObjectExistence(OWLNamedIndividual individual, boolean bypass) {
        if (!bypass) {
            final Set<OWLDataPropertyAssertionAxiom> individualExistenceProperties = this.ontology.getAllDataPropertiesForIndividual(individual).collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add).blockingGet();
            return TemporalObjectBuilder.buildTemporalFromProperties(individualExistenceProperties, OffsetDateTime.class, null, null);
        }
        return Optional.empty();
    }

    /**
     * Write an indirect object property between a Java object and an intermediate OWL individual
     * The OWL individual must exist before calling this function, but the Java object is created if it doesn't exist.
     *
     * @param subject  - {@link OWLNamedIndividual} of intermediate OWL object
     * @param object   - Java {@link Object} to write as object of assertion
     * @param property - {@link OWLObjectProperty} to assert
     */
    private void writeIndirectObjectProperty(OWLNamedIndividual subject, Object object, OWLObjectProperty property) {
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = df.getOWLObjectPropertyAssertionAxiom(property, subject, objectIndividual);
        try {
            this.ontology.writeIndividualObjectProperty(owlObjectPropertyAssertionAxiom).blockingAwait();
        } catch (Exception e) { // I don't think this is ever called
            logger.error("Exception while writing property, trying to write object", e);
            this.writeTrestleObject(object);
        }
    }


    /**
     * Write an object property assertion between two objects, writing them into the database if they don't exist.
     *
     * @param subject  - Java {@link Object} to write as subject of assertion
     * @param object   - Java {@link Object} to write as object of assertion
     * @param property - {@link OWLObjectProperty} to assert between the two objects
     */
    private void writeObjectProperty(Object subject, Object object, OWLObjectProperty property) {
        logger.debug("Writing relationship {} between {} (subject) and {} (object)", property, subject, object);
        final OWLNamedIndividual subjectIndividual = this.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = this.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom objectRelationshipAssertion = df.getOWLObjectPropertyAssertionAxiom(property,
                subjectIndividual,
                objectIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            ontology.writeIndividualObjectProperty(objectRelationshipAssertion).blockingAwait();
        } catch (Exception e) {
            logger.debug("Individual does not exist, creating", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
////            Do we need to write the subject, or the object?
////            Start with object, and then try for the subject
//            if (e.getIndividual().equals(objectIndividual.toString())) {
//                this.writeTrestleObject(subject);
//                ontology.writeIndividualObjectProperty(objectRelationshipAssertion);
//            }
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }
}
