package com.nickrobison.trestle.reasoner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.TrestlePair;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.IndividualEngine;
import com.nickrobison.trestle.reasoner.engines.collection.ITrestleCollectionEngine;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.exporter.ITrestleDataExporter;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.relations.RelationTracker;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialComparisonReport;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Computable;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Filterable;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalComparisonReport;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalEngine;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.TypeConstructor;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.TrestleObjectHeader;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Supplier;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.utils.ConfigValidator.ValidateConfig;

/**
 * Created by nrobison on 5/17/16.
 */
@Metriced
// I'm tired of dealing with all these random @NoNull T cast issues.
@SuppressWarnings({"methodref.inference.unimplemented", "argument.type.incompatible", "assignment.type.incompatible", "return.type.incompatible"})
public class TrestleReasonerImpl implements TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasonerImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final String BLANK_TEMPORAL_ID = "blank";

    private final String reasonerPrefix;
    private final ITrestleOntology ontology;
    //    Seems gross?
    private final QueryBuilder qb;
    private final QueryBuilder.Dialect spatialDialect;
    private final TrestleParser trestleParser;
    private final ITrestleObjectWriter objectWriter;
    private final ITrestleObjectReader objectReader;
    private final ITrestleCollectionEngine collectionEngine;
    private final ITrestleDataExporter dataExporter;
    private final TrestleMergeEngine mergeEngine;
    private final TrestleEventEngine eventEngine;
    private final IndividualEngine individualEngine;
    private final SpatialEngine spatialEngine;
    private final AggregationEngine aggregationEngine;
    private final TemporalEngine temporalEngine;
    private final RelationTracker relationTracker;
    private final Config trestleConfig;
    private final TrestleCache trestleCache;
    private final Metrician metrician;
    private final ExecutorService trestleThreadPool;
    private final TrestleExecutorService comparisonThreadPool;

    @SuppressWarnings("dereference.of.nullable")
    TrestleReasonerImpl(TrestleBuilder builder) {

//        Read in the trestleConfig file and validate it
        trestleConfig = ConfigFactory.load().getConfig("trestle");
        ValidateConfig(trestleConfig);

        final Injector injector = Guice.createInjector(new TrestleModule(builder, builder.metrics, builder.caching, this.trestleConfig.getBoolean("merge.enabled"), this.trestleConfig.getBoolean("events.enabled"), this.trestleConfig.getBoolean("track.enabled")));
        //        Setup the reasoner prefix
        reasonerPrefix = injector.getInstance(Key.get(String.class, ReasonerPrefix.class));
        logger.info("Setting up reasoner with prefix {}", reasonerPrefix);

//        Setup metrics engine
        metrician = injector.getInstance(Metrician.class);

        TrestleExecutorFactory factory = injector.getInstance(TrestleExecutorFactory.class);

//        Create our own thread pools to help isolate processes
        trestleThreadPool = factory.create(builder.ontologyName.orElse("default"));
        comparisonThreadPool = factory.create("comparison-pool");

        ontology = injector.getInstance(ITrestleOntology.class);
        logger.debug("Ontology connected");
        if (builder.initialize) {
            logger.info("Initializing ontology");
            this.ontology.initializeOntology();
        } else {
//            If we're not starting fresh, then we might need to update the indexes and inferencer
            logger.debug("Not initializing ontology");
        }
        logger.info("Ontology {} ready", injector.getInstance(Key.get(String.class, OntologyName.class)));

//        Setup the Parser
        trestleParser = injector.getInstance(TrestleParser.class);

//      Engines on
        this.objectReader = injector.getInstance(ITrestleObjectReader.class);
        this.objectWriter = injector.getInstance(ITrestleObjectWriter.class);
        this.collectionEngine = injector.getInstance(ITrestleCollectionEngine.class);
        this.dataExporter = injector.getInstance(ITrestleDataExporter.class);
        this.mergeEngine = injector.getInstance(TrestleMergeEngine.class);
        this.eventEngine = injector.getInstance(TrestleEventEngine.class);
        this.individualEngine = injector.getInstance(IndividualEngine.class);
        this.spatialEngine = injector.getInstance(SpatialEngine.class);
        this.aggregationEngine = injector.getInstance(AggregationEngine.class);
        this.temporalEngine = injector.getInstance(TemporalEngine.class);
        this.relationTracker = injector.getInstance(RelationTracker.class);

//        Register type constructors from the service loader
        final ServiceLoader<TypeConstructor> constructors = ServiceLoader.load(TypeConstructor.class);
        for (final TypeConstructor constructor : constructors) {
            this.registerTypeConstructor(constructor);
        }

//            validate the classes
        builder.inputClasses.forEach(clazz -> {
            try {
                this.trestleParser.classRegistry.registerClass(trestleParser.classParser.getObjectClass(clazz), clazz);
            } catch (TrestleClassException e) {
                logger.error("Cannot validate class {}", clazz, e);
            }
        });

        trestleCache = injector.getInstance(TrestleCache.class);

//        Setup the query builder
        this.qb = injector.getInstance(QueryBuilder.class);
        this.spatialDialect = this.qb.getDialect();
        logger.debug("Using SPARQL dialect {}", spatialDialect);

        logger.info("Trestle Reasoner is ready");
    }

    @Override
    public void shutdown() {
        this.shutdown(false);
    }

    @Override
    public void shutdown(boolean delete) {
        if (delete) {
            logger.info("Shutting down reasoner, and removing the model");
        } else {
            logger.info("Shutting down reasoner");
        }
        this.trestleThreadPool.shutdown();
        this.comparisonThreadPool.shutdown();
        logger.debug("Waiting 10 Seconds for thread-pool to terminate");
        try {
            final boolean awaitTermination = this.trestleThreadPool.awaitTermination(10, TimeUnit.SECONDS);
            if (!awaitTermination) {
                logger.error("thread-pool terminated with processes in flight");
            }
        } catch (InterruptedException e) {
            logger.error("Unable to shutdown thread-pool", e);
            Thread.currentThread().interrupt();
        }
//        Check to make sure we don't have any open transactions
        final long openTransactionCount = this.ontology.getCurrentlyOpenTransactions();
        if (openTransactionCount > 0) {
            logger.error("Currently  has {} open read and {} open write transactions!",
                    this.ontology.getOpenReadTransactions(),
                    this.ontology.getOpenWriteTransactions());
        }
        this.trestleCache.shutdown(delete);
        this.ontology.close(delete);
        this.metrician.shutdown();
    }

    @Override
    public <C extends TypeConstructor> void registerTypeConstructor(TrestleReasonerImpl this, C typeConstructor) {
        this.trestleParser.typeConverter.registerTypeConstructor(typeConstructor);
    }

    //    When you get the ontology, the ownership passes away, so then the reasoner can't perform any more queries.
    @Override
    public ITrestleOntology getUnderlyingOntology() {
        return this.ontology;
    }

    @Override
    public Metrician getMetricsEngine() {
        return this.metrician;
    }

    @Override
    public TrestleMergeEngine getMergeEngine() {
        return this.mergeEngine;
    }

    @Override
    public EqualityEngine getEqualityEngine() {
        return this.spatialEngine;
    }

    @Override
    public SpatialEngine getSpatialEngine() {
        return this.spatialEngine;
    }

    public AggregationEngine getAggregationEngine() {
        return this.aggregationEngine;
    }

    @Override
    public TemporalEngine getTemporalEngine() {
        return this.temporalEngine;
    }

    @Override
    public ContainmentEngine getContainmentEngine() {
        return this.spatialEngine;
    }

    @Override
    public TrestleCache getCache() {
        return this.trestleCache;
    }

    @Override
    public Map<String, String> getReasonerPrefixes() {
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put(":", this.reasonerPrefix);
        prefixes.putAll(this.getUnderlyingOntology().getUnderlyingPrefixManager().getPrefixName2PrefixMap());
        return prefixes;
    }

    @Override
    public TrestleParser getUnderlyingParser() {
        return this.trestleParser;
    }

    @Override
    public List<TrestleResult> executeSPARQLSelect(String queryString) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final List<TrestleResult> resultSet = this.ontology.executeSPARQLResults(queryString).toList().blockingGet();
        this.ontology.returnAndCommitTransaction(trestleTransaction);
        return resultSet;
    }

    @Override
    public Set<OWLNamedIndividual> getInstances(Class inputClass) {
        final OWLClass owlClass = trestleParser.classParser.getObjectClass(inputClass);
        return this.ontology.getInstances(owlClass, true).collect((Supplier<HashSet<OWLNamedIndividual>>) HashSet::new, HashSet::add).blockingGet();
    }

    @Override
    public void writeOntology(URI filePath, boolean validate) {
        Instant start = Instant.now();
        try {
            logger.info("Writing Ontology to {}", filePath);
            ontology.writeOntology(IRI.create(filePath), validate);
        } catch (OWLOntologyStorageException e) {
            logger.error("Could not write ontology to {}", filePath, e);
        }
        Instant end = Instant.now();
        logger.info("Writing Ontology took {} ms", Duration.between(start, end).toMillis());
    }

//    ----------------------------
//    WRITE Methods
//    ----------------------------


    @Override
    public Completable writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        return this.objectWriter.writeTrestleObject(inputObject);
    }

    @Override
    public Completable writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {
        return this.objectWriter.writeTrestleObject(inputObject, startTemporal, endTemporal);
    }

    @Override
    public Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        return this.objectWriter.addFactToTrestleObject(clazz, individual, factName, value, validAt, databaseFrom);
    }

    @Override
    public Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        return this.objectWriter.addFactToTrestleObject(clazz, individual, factName, value, validFrom, validTo, databaseFrom);
    }


//    ----------------------------
//    READ Methods
//    ----------------------------

//    ----------------------------
//    String Methods
//    ----------------------------

    @Override
    public <T extends @NonNull Object> Single<@NonNull Object> readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException {
        return this.objectReader.readTrestleObject(datasetClassID, objectID);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException {
        return this.objectReader.readTrestleObject(datasetClassID, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity {
        return this.objectReader.readTrestleObject(clazz, objectID);
    }


    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity {
        return this.objectReader.readTrestleObject(clazz, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public @io.reactivex.rxjava3.annotations.NonNull Maybe<TrestleObjectHeader> readObjectHeader(Class<?> clazz, String individual) {
        return this.objectReader.readObjectHeader(clazz, individual);
    }

    //    ----------------------------
//    IRI Methods
//    ----------------------------

    @Override
    public <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache) {
        return this.objectReader.readTrestleObject(clazz, individualIRI, bypassCache);
    }

    @Override
    public @io.reactivex.rxjava3.annotations.NonNull <T extends @NonNull Object> Single<T> readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt) {
        return this.objectReader.readTrestleObject(clazz, individualIRI, bypassCache, validAt, databaseAt);
    }

    @Override
    public Flowable<Object> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
        return this.objectReader.getFactValues(clazz, individual, factName, validStart, validEnd, databaseTemporal);
    }

    @Override
    public @io.reactivex.rxjava3.annotations.NonNull Flowable<Object> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
        return this.objectReader.getFactValues(clazz, individual, factName, validStart, validEnd, databaseTemporal);
    }

    @Override
    public Flowable<Object> sampleFactValues(Class<?> clazz, String factName, long sampleLimit) {
        return this.objectReader.sampleFactValues(clazz, factName, sampleLimit);
    }

    @Override
    public @io.reactivex.rxjava3.annotations.NonNull Flowable<Object> sampleFactValues(Class<?> clazz, OWLDataProperty factName, long sampleLimit) {
        return this.objectReader.sampleFactValues(clazz, factName, sampleLimit);
    }

    @Override
    public <T> Flowable<T> getRelatedObjects(Class<T> clazz, String identifier, ObjectRelation relation, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        return this.objectReader.getRelatedObjects(clazz, identifier, relation, validAt, dbAt);
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, String individual) {
        return getIndividualEvents(clazz, df.getOWLNamedIndividual(parseStringToIRI(reasonerPrefix, individual)));
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual) {
        return this.individualEngine.getIndividualEvents(clazz, individual);
    }

    @Override
    public void addTrestleObjectEvent(TrestleEventType type, String individual, Temporal eventTemporal) {
        addTrestleObjectEvent(type, df.getOWLNamedIndividual(parseStringToIRI(reasonerPrefix, individual)), eventTemporal);
    }

    @Override
    public void addTrestleObjectEvent(TrestleEventType type, OWLNamedIndividual individual, Temporal eventTemporal) {
        if (type == TrestleEventType.SPLIT || type == TrestleEventType.MERGED) {
            throw new IllegalArgumentException("SPLIT and MERGED events cannot be added through this method");
        }
        this.eventEngine.addEvent(type, individual, eventTemporal);
    }

    @Override
    public <T extends @NonNull Object> Completable addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength) {
        return this.objectWriter.addTrestleObjectSplitMerge(type, subject, objects, strength);
    }

//    ----------------------------
//    Spatial Methods
//    ----------------------------

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer) {
        return this.spatialEngine.spatialIntersectObject(inputObject, buffer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer, @Nullable Temporal temporalAt, Temporal dbAt) {
        return this.spatialEngine.spatialIntersectObject(inputObject, buffer, temporalAt, null);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer, Unit<Length> bufferUnit) {
        return this.spatialEngine.spatialIntersectObject(inputObject, buffer, bufferUnit);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer, Unit<Length> bufferUnit, Temporal temporalAt, Temporal dbAt) {
        return this.spatialEngine.spatialIntersectObject(inputObject, buffer, bufferUnit, temporalAt, null);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer, Unit<Length> bufferUnit) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer, bufferUnit);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer, Unit<Length> bufferUnit, Temporal validAt, Temporal dbAt) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer, bufferUnit, validAt, null);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer, @Nullable Temporal validAt, Temporal dbAt) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer, validAt, null);
    }

    @Override
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, int inputSRID, double matchThreshold) {
        return this.spatialEngine.calculateSpatialUnion(inputObjects, inputSRID, matchThreshold);
    }

    @Override
    public <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, int inputSRID) {
        return this.spatialEngine.calculateUnionContribution(result, inputSRID);
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> boolean isApproximatelyEqual(A inputObject, B matchObject, double threshold) {
        return this.spatialEngine.isApproximatelyEqual(inputObject, matchObject, threshold);
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> double calculateSpatialEquals(A inputObject, B matchObject) {
        return this.spatialEngine.calculateSpatialEquals(inputObject, matchObject);
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> ContainmentDirection getApproximateContainment(A objectA, B objectB, double threshold) {
        return this.spatialEngine.getApproximateContainment(objectA, objectB, threshold);
    }

//    ----------------------------
//    Collection Methods
//    ----------------------------


    @Override
    public List<String> getCollections() {
        return this.collectionEngine.getCollections();
    }

    @Override
    public Optional<Map<String, List<String>>> getRelatedCollections(String individual, @Nullable String collectionID, double relationStrength) {
        return this.collectionEngine.getRelatedCollections(individual, collectionID, relationStrength);
    }

    @Override
    public Optional<Set<String>> STIntersectCollection(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        return this.collectionEngine.STIntersectCollection(wkt, buffer, strength, validAt, dbAt);
    }

    @Override
    public Optional<Set<String>> STIntersectCollection(String wkt, double buffer, Unit<Length> bufferUnit, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        return this.collectionEngine.STIntersectCollection(wkt, buffer, bufferUnit, strength, validAt, dbAt);
    }

    @Override
    public <T> Optional<List<T>> getCollectionMembers(Class<T> clazz, String collectionID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection) {
        return this.collectionEngine.getCollectionMembers(clazz, collectionID, strength, spatialIntersection, temporalIntersection);
    }

    @Override
    public void addObjectToCollection(String collectionIRI, Object inputObject, CollectionRelationType relationType, double strength) {
        this.collectionEngine.addObjectToCollection(collectionIRI, inputObject, relationType, strength);
    }

    @Override
    public void removeObjectFromCollection(String collectionIRI, Object inputObject, boolean removeEmptyCollection) {
        this.collectionEngine.removeObjectFromCollection(collectionIRI, inputObject, removeEmptyCollection);
    }

    @Override
    public void removeCollection(String collectionIRI) {
        this.collectionEngine.removeCollection(collectionIRI);
    }

    @Override
    public boolean collectionsAreAdjacent(String subjectCollectionID, String objectCollectionID, double strength) {
        return this.collectionEngine.collectionsAreAdjacent(subjectCollectionID, objectCollectionID, strength);
    }

    @Override
    public Completable writeObjectRelationship(Object subject, Object object, ObjectRelation relation) {
        return this.objectWriter.writeObjectRelationship(subject, object, relation);
    }

    @Override
    public Completable writeSpatialOverlap(Object subject, Object object, String wkt) {
        return this.objectWriter.writeSpatialOverlap(subject, object, wkt);
    }

    @Override
    public Completable writeTemporalOverlap(Object subject, Object object, String temporalOverlap) {
        return this.objectWriter.writeTemporalOverlap(subject, object, temporalOverlap);
    }

    /**
     * Remove individuals from the ontology
     *
     * @param inputObject - Individual to remove
     * @param <T>         - Type of individual to remove
     */
    @SafeVarargs
    public final <T extends @NonNull Object> void removeIndividual(T... inputObject) {
        // TODO(nickrobison): TRESTLE-733: Make better with RxJava
        final List<CompletableFuture<Void>> completableFutures = Arrays.stream(inputObject)
                .map(object -> CompletableFuture.supplyAsync(() -> trestleParser.classParser.getIndividual(object), trestleThreadPool))
                .map(idFuture -> idFuture.thenApply(individual -> this.ontology.getAllObjectPropertiesForIndividual(individual).collect((Supplier<HashSet<OWLObjectPropertyAssertionAxiom>>) HashSet::new, HashSet::add).blockingGet()))
                .map(propertyFutures -> propertyFutures.thenCompose(this::removeRelatedObjects))
                .map(removedFuture -> removedFuture.thenAccept(individual -> this.ontology.removeIndividual(individual).blockingAwait()))
                .collect(Collectors.toList());
        final CompletableFuture<List<Void>> listCompletableFuture = sequenceCompletableFutures(completableFutures);
        try {
            listCompletableFuture.get();
        } catch (InterruptedException e) {
            logger.error("Delete interrupted", e.getCause());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.error("Execution error", e.getCause());
        }
    }

    /**
     * Given a set of OWL Objects, remove them and return the subject as an OWLNamedIndividual
     *
     * @param objectProperties - Set of OWLObjectPropertyAssertionAxioms to remove the object assertions from the ontology
     * @return - OWLNamedIndividual representing the subject of the object assertions
     */
    private CompletableFuture<OWLNamedIndividual> removeRelatedObjects(Set<OWLObjectPropertyAssertionAxiom> objectProperties) {
        // TODO(nickrobison): TRESTLE-733: Make better with rxJava
        return CompletableFuture.supplyAsync(() -> {

//            Remove the facts
            final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
            objectProperties
                    .stream()
                    .filter(property -> property.getProperty().getNamedProperty().getIRI().equals(hasFactIRI))
                    .forEach(object -> ontology.removeIndividual(object.getObject().asOWLNamedIndividual()));

//            And the temporals, but make sure the temporal doesn't have any other dependencies
            objectProperties
                    .stream()
                    .filter(property -> property.getProperty().getNamedProperty().getIRI().equals(hasTemporalIRI))
                    .map(object -> Optional.of(ontology.getIndividualObjectProperty(object.getObject().asOWLNamedIndividual(), temporalOfIRI).toList().blockingGet()))
                    .filter(properties -> properties.orElseThrow(RuntimeException::new).size() <= 1)
                    .map(properties -> properties.orElseThrow(RuntimeException::new).stream().findAny())
                    .filter(Optional::isPresent)
                    .forEach(property -> ontology.removeIndividual(property.orElseThrow(RuntimeException::new).getObject().asOWLNamedIndividual()));

//            And the database time object
            objectProperties
                    .stream()
                    .filter(property -> property.getProperty().getNamedProperty().getIRI().equals(databaseTimeIRI))
                    .map(object -> Optional.of(ontology.getIndividualObjectProperty(object.getObject().asOWLNamedIndividual(), databaseTimeOfIRI).toList().blockingGet()))
                    .filter(properties -> properties.orElseThrow(RuntimeException::new).size() <= 1)
                    .map(properties -> properties.orElseThrow(RuntimeException::new).stream().findAny())
                    .filter(Optional::isPresent)
                    .forEach(property -> ontology.removeIndividual(property.orElseThrow(RuntimeException::new).getObject().asOWLNamedIndividual()));

            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return objectProperties.stream().findAny().orElseThrow(RuntimeException::new).getSubject().asOWLNamedIndividual();
        }, trestleThreadPool);

    }


    @Override
    public Optional<UnionContributionResult> calculateSpatialUnionWithContribution(String datasetClassID, List<String> individualIRIs, int inputSRID, double matchThreshold) {
        return this.spatialEngine.calculateSpatialUnionWithContribution(datasetClassID, individualIRIs, inputSRID, matchThreshold);
    }

    @Override
    public Optional<List<SpatialComparisonReport>> compareTrestleObjects(String datasetID, String objectAID, List<String> comparisonObjectIDs, int inputSR, double matchThreshold) {
        return this.spatialEngine.compareTrestleObjects(datasetID, objectAID, comparisonObjectIDs, inputSR, matchThreshold);
    }

    @Override
    public <A, B> SpatialComparisonReport compareTrestleObjects(A objectA, B objectB, double matchThreshold) {
        return this.spatialEngine.compareTrestleObjects(objectA, objectB, matchThreshold);
    }

    @Override
    public <T> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, OWLNamedIndividual individual, Temporal queryTemporal) {
        return this.spatialEngine.getEquivalentIndividuals(clazz, individual, queryTemporal);
    }

    @Override
    public <T> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, List<OWLNamedIndividual> individual, Temporal queryTemporal) {
        return this.spatialEngine.getEquivalentIndividuals(clazz, individual, queryTemporal);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> getEquivalentObjects(Class<T> clazz, IRI individual, Temporal queryTemporal) {
        return getEquivalentObjects(clazz, Collections.singletonList(individual), queryTemporal);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> getEquivalentObjects(Class<T> clazz, List<IRI> individuals, Temporal queryTemporal) {
        final List<OWLNamedIndividual> individualSubjects = individuals
                .stream()
                .map(df::getOWLNamedIndividual)
                .collect(Collectors.toList());
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final List<OWLNamedIndividual> equivalentIndividuals = this.spatialEngine.getEquivalentIndividuals(clazz, individualSubjects, queryTemporal);
            final List<T> individualList = equivalentIndividuals
                    .stream()
                    .map(individual -> {
                        return this.objectReader.readTrestleObject(clazz, individual.getIRI(), false, queryTemporal, null).blockingGet();
                    })
                    .collect(Collectors.toList());
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.of(individualList);
        } catch (Exception e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            logger.error("Unable to get equivalent objects for {} at {}", individuals, queryTemporal, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> void calculateSpatialAndTemporalRelationships(Class<T> clazz, String individual, @Nullable Temporal validAt) throws TrestleClassException, MissingOntologyEntity {
        final IRI individualIRI = parseStringToIRI(this.reasonerPrefix, individual);

//        Read the object first
        final T trestleObject = this.objectReader.readTrestleObject(clazz, individual, validAt, null).blockingGet();
//        Intersect it
        final Optional<List<T>> intersectedObjects = this.spatialEngine.spatialIntersectObject(trestleObject, 1, validAt, null);
//        Now, compute the relationships (I'm fine doing this on an alternate thread, but it should return a completable future, rather than blocking the thread
        // TODO(nickrobison): TRESTLE-733 - Make this better with RxJava
        if (intersectedObjects.isPresent()) {
            final List<CompletableFuture<@Nullable TrestlePair<T, TrestlePair<SpatialComparisonReport, TemporalComparisonReport>>>> comparisonFutures = intersectedObjects.get()
                    .stream()
                    .map(intersectedObject -> CompletableFuture.supplyAsync(() -> {
                        final IRI intersectedIRI = this.trestleParser.classParser.getIndividual(intersectedObject).getIRI();
//                        If we've already computed these two, don't do them again.
//                        Or, if the objects are the same, skip them
                        if (this.relationTracker.hasRelation(individualIRI, intersectedIRI) || intersectedIRI.equals(individualIRI)) {
                            logger.debug("Already computed relationships between {} and {}", individualIRI, intersectedIRI);
                            return null;
                        }
                        logger.debug("Writing relationships between {} and {}", individualIRI, this.trestleParser.classParser.getIndividual(intersectedObject));
                        final SpatialComparisonReport spatialComparisonReport = this.compareTrestleObjects(trestleObject, intersectedObject, 0.9);
                        final TemporalComparisonReport temporalComparisonReport = this.temporalEngine.compareObjects(trestleObject, intersectedObject);
                        return new TrestlePair<>(intersectedObject, new TrestlePair<>(spatialComparisonReport, temporalComparisonReport));
                    }, this.comparisonThreadPool))
                    .collect(Collectors.toList());

            final CompletableFuture<List<@Nullable TrestlePair<T, TrestlePair<SpatialComparisonReport, TemporalComparisonReport>>>> comparisonFuture = sequenceCompletableFutures(comparisonFutures);


//            Write all the relationships in a single transaction
//            TODO(nickrobison): This should not be on the main thread.
            final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
            try {
//                Filter out null objects and those that have no spatial interactions
                final List<@Nullable TrestlePair<T, TrestlePair<SpatialComparisonReport, TemporalComparisonReport>>> comparisons = comparisonFuture
                        .get()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(pair -> !pair.getRight().getLeft().getRelations().isEmpty())
                        .collect(Collectors.toList());

                comparisons.forEach(cPair -> {
                    final T intersectedObject = cPair.getLeft();
                    final SpatialComparisonReport spatialComparisonReport = cPair.getRight().getLeft();
                    final TemporalComparisonReport temporalComparisonReport = cPair.getRight().getRight();
                    final IRI intersectedObjectID = parseStringToIRI(this.reasonerPrefix, spatialComparisonReport.getObjectBID());

                    //                Write the relationships
                    spatialComparisonReport.getRelations().forEach(relation -> {
                        logger.trace("Writing spatial relationship {}", relation);
                        this.writeObjectRelationship(trestleObject, intersectedObject, relation);
                    });

//                Write overlaps
                    spatialComparisonReport.getSpatialOverlap().ifPresent(overlap -> {
                        logger.debug("Writing spatial overlap");
                        this.writeSpatialOverlap(trestleObject, intersectedObject, overlap);
                    });

                    //            Temporal relations
                    temporalComparisonReport.getRelations().forEach(tRelation -> {
                        logger.debug("Writing temporal relationship {}", tRelation);
                        this.writeObjectRelationship(trestleObject, intersectedObject, tRelation);
                    });
                    this.relationTracker.addRelation(individualIRI, intersectedObjectID);

                });
                this.ontology.returnAndCommitTransaction(trestleTransaction);
            } catch (InterruptedException e) {
                logger.error("Interrupted while computing relationships for {}", individualIRI, e);
                Thread.currentThread().interrupt();
                this.ontology.returnAndAbortTransaction(trestleTransaction);
            } catch (ExecutionException e) {
                logger.error("Cannot compute relationships for {}", individualIRI, ExceptionUtils.getRootCause(e));
                this.ontology.returnAndAbortTransaction(trestleTransaction);
            }
        }
    }

    @Override
    public <T extends @NonNull Object, B extends Number> AggregationEngine.AdjacencyGraph<T, B> buildSpatialGraph(Class<T> clazz, String objectID, Computable<T, T, B> edgeCompute, Filterable<T> filter, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        return this.aggregationEngine.buildSpatialGraph(clazz, objectID, edgeCompute, filter, validAt, dbAt);
    }

    @Override
    public List<String> searchForIndividual(String individualIRI) {
        return searchForIndividual(individualIRI, null, null);
    }

    @Override
    public List<String> searchForIndividual(String individualIRI, @Nullable String datasetClass, @Nullable Integer limit) {
        final OWLClass owlClass;
        if (datasetClass != null) {
            owlClass = df.getOWLClass(parseStringToIRI(reasonerPrefix, datasetClass));
        } else {
            owlClass = null;
        }

        final String query = qb.buildIndividualSearchQuery(individualIRI, owlClass, limit);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        try {
            final List<String> results = ontology.executeSPARQLResults(query).toList().blockingGet()
                    .stream()
                    .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                    .collect(Collectors.toList());
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return results;
        } catch (Exception e) {
            logger.error("Problem searching for {}", individualIRI, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return ExceptionUtils.rethrow(e);
        }
    }

    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer) {
        return this.spatialEngine.spatialIntersectIndividuals(datasetClassID, wkt, buffer);
    }


    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        return this.spatialEngine.spatialIntersectIndividuals(datasetClassID, wkt, buffer, atTemporal, dbTemporal);
    }

    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, Unit<Length> bufferUnit) {
        return this.spatialEngine.spatialIntersectIndividuals(datasetClassID, wkt, buffer, bufferUnit);
    }

    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, Unit<Length> bufferUnit, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        return this.spatialEngine.spatialIntersectIndividuals(datasetClassID, wkt, buffer, bufferUnit, atTemporal, dbTemporal);
    }

    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<?> clazz, String wkt, double buffer, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        return this.spatialEngine.spatialIntersectIndividuals(clazz, wkt, buffer, atTemporal, dbTemporal);
    }

    @Override
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, Unit<Length> bufferUnit, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        return this.spatialEngine.spatialIntersectIndividuals(clazz, wkt, buffer, bufferUnit, validAt, dbAt);
    }

    @Override
    public TrestleIndividual getTrestleIndividual(String individualIRI) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(reasonerPrefix, individualIRI)));
    }

    private TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return this.individualEngine.getTrestleIndividual(individual);
    }

    @Override
    public void registerClass(Class inputClass) throws TrestleClassException {
        final OWLClass owlClass = this.trestleParser.classParser.getObjectClass(inputClass);
        this.trestleParser.classRegistry.registerClass(owlClass, inputClass);
    }

    @Override
    public void deregisterClass(Class inputClass) {
        this.trestleParser.classRegistry.deregisterClass(inputClass);
    }

    @Override
    public Set<String> getAvailableDatasets() {

        final String datasetQuery = qb.buildDatasetQuery();
        final List<TrestleResult> resultSet = ontology.executeSPARQLResults(datasetQuery).toList().blockingGet();
        Set<OWLClass> datasetsInOntology = resultSet
                .stream()
                .map(result -> df.getOWLClass(result.getIndividual("dataset").orElseThrow(() -> new RuntimeException("dataset is null")).toStringID()))
                .collect(Collectors.toSet());

        return this.trestleParser.classRegistry
                .getRegisteredOWLClasses()
                .stream()
                .filter(datasetsInOntology::contains)
                .map(individual -> individual.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    @Override
    public Class<?> getDatasetClass(String owlClassString) throws UnregisteredClassException {
        final OWLClass owlClass = df.getOWLClass(parseStringToIRI(reasonerPrefix, owlClassString));
        return this.trestleParser.classRegistry.lookupClass(owlClass);
    }

    @Override
    public List<String> getDatasetProperties(Class<?> clazz) {
        final List<OWLDataProperty> owlDataProperties = this.trestleParser.classBuilder.getPropertyMembers(clazz)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot get properties for %s", this.trestleParser.classParser.getObjectClass(clazz))));
        return owlDataProperties
                .stream()
                .map(OWLEntity::toStringID)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDatasetMembers(Class<?> clazz) {
        final OWLClass objectClass = this.trestleParser.classParser.getObjectClass(clazz);
        return this.ontology.getInstances(objectClass, true).toList().blockingGet()
                .stream()
                .map(OWLIndividual::toStringID)
                .collect(Collectors.toList());
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException {
        return this.dataExporter.exportDataSetObjects(inputClass, objectID, exportType);
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException {
        return this.dataExporter.exportDataSetObjects(inputClass, objectID, validAt, databaseAt, exportType);
    }
}
