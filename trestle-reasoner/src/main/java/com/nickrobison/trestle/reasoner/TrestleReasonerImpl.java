package com.nickrobison.trestle.reasoner;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.SpatialReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.exporter.*;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.TrestleOntologyModule;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.IndividualEngine;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.object.TrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialComparisonReport;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalEngine;
import com.nickrobison.trestle.reasoner.exceptions.InvalidOntologyName;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.TypeConstructor;
import com.nickrobison.trestle.reasoner.parser.TypeConverter;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.reasoner.utils.TemporalPropertiesPair;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.TrestleFact;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ConceptRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;
import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;
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
    public static final String DEFAULTNAME = "trestle";
    public static final String ONTOLOGY_RESOURCE_NAME = "trestle.owl";
    public static final String BLANK_TEMPORAL_ID = "blank";

    private final String REASONER_PREFIX;
    private final ITrestleOntology ontology;
    //    Seems gross?
    private final QueryBuilder qb;
    private final QueryBuilder.Dialect spatialDalect;
    private final TrestleParser trestleParser;
    private final ITrestleObjectWriter objectWriter;
    private final ITrestleObjectReader objectReader;
    private final TrestleMergeEngine mergeEngine;
    private final TrestleEventEngine eventEngine;
    private final IndividualEngine individualEngine;
    private final SpatialEngine spatialEngine;
    private final TemporalEngine temporalEngine;
    private final Config trestleConfig;
    private final TrestleCache trestleCache;
    private final Metrician metrician;
    private final ExecutorService trestleThreadPool;
    private final TrestleExecutorService objectThreadPool;
    private final ExecutorService searchThreadPool;

    @SuppressWarnings("dereference.of.nullable")
    TrestleReasonerImpl(TrestleBuilder builder) throws OWLOntologyCreationException {

//        Read in the trestleConfig file and validate it
        trestleConfig = ConfigFactory.load().getConfig("trestle");
        ValidateConfig(trestleConfig);

        //        Setup the reasoner prefix
//        If not specified, use the default Trestle prefix
        REASONER_PREFIX = builder.reasonerPrefix.orElse(TRESTLE_PREFIX);
        logger.info("Setting up reasoner with prefix {}", REASONER_PREFIX);

//        If we have a manually specified ontology, use that.
        final URL ontologyResource;
        final InputStream ontologyIS;
        if (builder.ontologyIRI.isPresent()) {
            try {
                ontologyResource = builder.ontologyIRI.get().toURI().toURL();
                ontologyIS = new FileInputStream(new File(builder.ontologyIRI.get().toURI()));
            } catch (MalformedURLException e) {
                logger.error("Unable to parse IRI to URI", builder.ontologyIRI.get(), e);
                throw new IllegalArgumentException(String.format("Unable to parse IRI %s to URI", builder.ontologyIRI.get()), e);
            } catch (FileNotFoundException e) {
                logger.error("Cannot find ontology file {}", builder.ontologyIRI.get(), e);
                throw new MissingResourceException("File not found", this.getClass().getName(), builder.ontologyIRI.get().getIRIString());
            }
        } else {
//            Load with the class loader
            ontologyResource = TrestleReasoner.class.getClassLoader().getResource(ONTOLOGY_RESOURCE_NAME);
            ontologyIS = TrestleReasoner.class.getClassLoader().getResourceAsStream(ONTOLOGY_RESOURCE_NAME);
        }

        if (ontologyIS == null) {
            logger.error("Cannot load trestle ontology from resources");
            throw new MissingResourceException("Cannot load ontology file", this.getClass().getName(), ONTOLOGY_RESOURCE_NAME);
        }
        try {
            final int available = ontologyIS.available();
            if (available == 0) {
                throw new MissingResourceException("Ontology InputStream does not seem to be available", this.getClass().getName(), ontologyIS.toString());
            }
        } catch (IOException e) {
            throw new MissingResourceException("Ontology InputStream does not seem to be available", this.getClass().getName(), ontologyIS.toString());
        }
        logger.info("Loading ontology from {}", ontologyResource == null ? "Null resource" : ontologyResource);

        //        Setup the ontology builder
        logger.info("Connecting to ontology {} at {}", builder.ontologyName.orElse(DEFAULTNAME), builder.connectionString.orElse("localhost"));
        logger.debug("IS: {}", ontologyIS);
        logger.debug("Resource: {}", ontologyResource == null ? "Null resource" : ontologyResource);
        OntologyBuilder ontologyBuilder = new OntologyBuilder()
//                .fromIRI(IRI.create(ontologyResource))
                .fromInputStream(ontologyIS)
                .withPrefixManager(builder.pm.getDefaultPrefixManager())
                .name(builder.ontologyName.orElse(DEFAULTNAME));
        if (builder.connectionString.isPresent()) {
            ontologyBuilder = ontologyBuilder.withDBConnection(builder.connectionString.get(),
                    builder.username,
                    builder.password);
        }

        final Injector injector = Guice.createInjector(new TrestleOntologyModule(ontologyBuilder, REASONER_PREFIX), new TrestleModule(builder.metrics, builder.caching, this.trestleConfig.getBoolean("merge.enabled"), this.trestleConfig.getBoolean("events.enabled")));

//        Setup metrics engine
        metrician = injector.getInstance(Metrician.class);

//        Create our own thread pools to help isolate processes
        trestleThreadPool = TrestleExecutorService.executorFactory(builder.ontologyName.orElse("default"), trestleConfig.getInt("threading.default-pool.size"), this.metrician);
        objectThreadPool = TrestleExecutorService.executorFactory("object-pool", trestleConfig.getInt("threading.object-pool.size"), this.metrician);
        searchThreadPool = TrestleExecutorService.executorFactory("search-pool", trestleConfig.getInt("threading.search-pool.size"), this.metrician);

//        Validate ontology name
        try {
            validateOntologyName(builder.ontologyName.orElse(DEFAULTNAME));
        } catch (InvalidOntologyName e) {
            logger.error("{} is an invalid ontology name", builder.ontologyName.orElse(DEFAULTNAME), e);
            throw new IllegalArgumentException("invalid ontology name", e);
        }

//        Register type constructors from the service loader
        final ServiceLoader<TypeConstructor> constructors = ServiceLoader.load(TypeConstructor.class);
        for (final TypeConstructor constructor : constructors) {
            this.registerTypeConstructor(constructor);
        }

        ontology = injector.getInstance(ITrestleOntology.class);
        logger.debug("Ontology connected");
        if (builder.initialize) {
            logger.info("Initializing ontology");
            this.ontology.initializeOntology();
        } else {
//            If we're not starting fresh, then we might need to update the indexes and inferencer
            logger.info("Updating inference model");
            ontology.runInference();
        }
        logger.info("Ontology {} ready", builder.ontologyName.orElse(DEFAULTNAME));

//        Setup the Parser
        trestleParser = injector.getInstance(TrestleParser.class);

//      Engines on
        this.objectReader = injector.getInstance(ITrestleObjectReader.class);
        this.objectWriter = injector.getInstance(TrestleObjectWriter.class);
        this.mergeEngine = injector.getInstance(TrestleMergeEngine.class);
        this.eventEngine = injector.getInstance(TrestleEventEngine.class);
        this.individualEngine = injector.getInstance(IndividualEngine.class);
        this.spatialEngine = injector.getInstance(SpatialEngine.class);
        this.temporalEngine = injector.getInstance(TemporalEngine.class);

//            validate the classes
        builder.inputClasses.forEach(clazz -> {
            try {
                this.trestleParser.classRegistry.registerClass(trestleParser.classParser.getObjectClass(clazz), clazz);
            } catch (TrestleClassException e) {
                logger.error("Cannot validate class {}", clazz, e);
            }
        });

        trestleCache = injector.getInstance(TrestleCache.class);

////        Setup the query builder
        this.qb = injector.getInstance(QueryBuilder.class);
        this.spatialDalect = this.qb.getDialect();
        logger.debug("Using SPARQL dialect {}", spatialDalect);

        logger.info("Trestle Reasoner ready");
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
        TypeConverter.registerTypeConstructor(typeConstructor);
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
        prefixes.put(":", this.REASONER_PREFIX);
        prefixes.putAll(this.getUnderlyingOntology().getUnderlyingPrefixManager().getPrefixName2PrefixMap());
        return prefixes;
    }

    @Override
    public TrestleParser getUnderlyingParser() {
        return this.trestleParser;
    }

    @Override
    public TrestleResultSet executeSPARQLSelect(String queryString) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(queryString);
        this.ontology.returnAndCommitTransaction(trestleTransaction);
        return resultSet;
    }

    @Override
    public Set<OWLNamedIndividual> getInstances(Class inputClass) {
        final OWLClass owlClass = trestleParser.classParser.getObjectClass(inputClass);
        return this.ontology.getInstances(owlClass, true);
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
    public void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        this.objectWriter.writeTrestleObject(inputObject);
    }

    @Override
    public void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {
        this.objectWriter.writeTrestleObject(inputObject, startTemporal, endTemporal);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        this.objectWriter.addFactToTrestleObject(clazz, individual, factName, value, validAt, databaseFrom);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        this.objectWriter.addFactToTrestleObject(clazz, individual, factName, value, validFrom, validTo, databaseFrom);
//        addFactToTrestleObjectImpl(clazz, individual, factName, value, null, validFrom, validTo, databaseFrom);
    }


//    ----------------------------
//    READ Methods
//    ----------------------------

//    ----------------------------
//    String Methods
//    ----------------------------

    @Override
    public <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException {
        return this.objectReader.readTrestleObject(datasetClassID, objectID);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException {
        return this.objectReader.readTrestleObject(datasetClassID, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity {
        return this.objectReader.readTrestleObject(clazz, objectID);
    }


    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity {
        return this.objectReader.readTrestleObject(clazz, objectID, validTemporal, databaseTemporal);
    }

//    ----------------------------
//    IRI Methods
//    ----------------------------

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache) {
        return this.objectReader.readTrestleObject(clazz, individualIRI, bypassCache);
    }

    @Override
    public <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt) {
        return this.objectReader.readTrestleObject(clazz, individualIRI, bypassCache, validAt, databaseAt);
    }

    @Override
    public Optional<List<Object>> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
        return this.objectReader.getFactValues(clazz, individual, factName, validStart, validEnd, databaseTemporal);
    }

    @Override
    public Optional<List<Object>> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
        return this.objectReader.getFactValues(clazz, individual, factName, validStart, validEnd, databaseTemporal);
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, String individual) {
        return getIndividualEvents(clazz, df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual)));
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual) {
        return this.individualEngine.getIndividualEvents(clazz, individual);
    }

    @Override
    public void addTrestleObjectEvent(TrestleEventType type, String individual, Temporal eventTemporal) {
        addTrestleObjectEvent(type, df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual)), eventTemporal);
    }

    @Override
    public void addTrestleObjectEvent(TrestleEventType type, OWLNamedIndividual individual, Temporal eventTemporal) {
        if (type == TrestleEventType.SPLIT || type == TrestleEventType.MERGED) {
            throw new IllegalArgumentException("SPLIT and MERGED events cannot be added through this method");
        }
        this.eventEngine.addEvent(type, individual, eventTemporal);
    }

    @Override
    public <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength) {
        this.objectWriter.addTrestleObjectSplitMerge(type, subject, objects, strength);
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
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer, @Nullable Temporal temporalAt) {
        return this.spatialEngine.spatialIntersectObject(inputObject, buffer, temporalAt);
    }

    @Override
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer);
    }

    @Override
    @Timed(name = "spatial-intersect-timer")
    @Metered(name = "spatial-intersect-meter")
    @SuppressWarnings({"override.return.invalid"})
    public <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer, @Nullable Temporal validAt) {
        return this.spatialEngine.spatialIntersect(clazz, wkt, buffer, validAt);
    }

    @Override
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return this.spatialEngine.calculateSpatialUnion(inputObjects, inputSR, matchThreshold);
    }

    @Override
    public <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, SpatialReference inputSR) {
        return this.spatialEngine.calculateUnionContribution(result, inputSR);
    }

    @Override
    public <T extends @NonNull Object> boolean isApproximatelyEqual(T inputObject, T matchObject, SpatialReference inputSR, double threshold) {
        return this.spatialEngine.isApproximatelyEqual(inputObject, matchObject, inputSR, threshold);
    }

    @Override
    public <T extends @NonNull Object> double calculateSpatialEquals(T inputObject, T matchObject, SpatialReference inputSR) {
        return this.spatialEngine.calculateSpatialEquals(inputObject, matchObject, inputSR);
    }

    @Override
    public <T> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold) {
        return this.spatialEngine.getApproximateContainment(objectA, objectB, inputSR, threshold);
    }

    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @Override
    @SuppressWarnings("return.type.incompatible")
    @Deprecated
    public <T extends @NonNull Object> Optional<Map<T, Double>> getRelatedObjects(Class<T> clazz, String objectID, double cutoff) {
        throw new UnsupportedOperationException("Migrating");
    }

    @Override
    public Optional<Map<String, List<String>>> getRelatedConcepts(String individual, @Nullable String conceptID, double relationStrength) {
        final String conceptQuery;
        final OWLNamedIndividual owlIndividual = df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual));
        if (conceptID != null) {
            conceptQuery = this.qb.buildConceptRetrievalQuery(
                    owlIndividual,
                    df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, conceptID)),
                    relationStrength);
        } else {
            conceptQuery = this.qb.buildConceptRetrievalQuery(
                    owlIndividual,
                    null,
                    relationStrength);
        }
        ListMultimap<String, String> conceptIndividuals = ArrayListMultimap.create();

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(conceptQuery);
            resultSet.getResults()
                    .forEach(result -> conceptIndividuals.put(result.getIndividual("concept").orElseThrow(() -> new RuntimeException("concept is null")).toStringID(), result.getIndividual("individual").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()));

            if (conceptIndividuals.keySet().size() == 0) {
                logger.info("Individual {} has no related concepts");
                return Optional.empty();
            }
            return Optional.of(Multimaps.asMap(conceptIndividuals));
        } catch (RuntimeException e) {
            logger.error("Problem getting concepts related to individual: {}", individual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    /**
     * Remove individuals from the ontology
     *
     * @param inputObject - Individual to remove
     * @param <T>         - Type of individual to remove
     */
    public <T extends @NonNull Object> void removeIndividual(T... inputObject) {
        final List<CompletableFuture<Void>> completableFutures = Arrays.stream(inputObject)
                .map(object -> CompletableFuture.supplyAsync(() -> trestleParser.classParser.getIndividual(object), trestleThreadPool))
                .map(idFuture -> idFuture.thenApply(ontology::getAllObjectPropertiesForIndividual))
                .map(propertyFutures -> propertyFutures.thenCompose(this::removeRelatedObjects))
                .map(removedFuture -> removedFuture.thenAccept(ontology::removeIndividual))
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
                    .map(object -> ontology.getIndividualObjectProperty(object.getObject().asOWLNamedIndividual(), temporalOfIRI))
                    .filter(Optional::isPresent)
                    .filter(properties -> properties.orElseThrow(RuntimeException::new).size() <= 1)
                    .map(properties -> properties.orElseThrow(RuntimeException::new).stream().findAny())
                    .filter(Optional::isPresent)
                    .forEach(property -> ontology.removeIndividual(property.orElseThrow(RuntimeException::new).getObject().asOWLNamedIndividual()));

//            And the database time object
            objectProperties
                    .stream()
                    .filter(property -> property.getProperty().getNamedProperty().getIRI().equals(databaseTimeIRI))
                    .map(object -> ontology.getIndividualObjectProperty(object.getObject().asOWLNamedIndividual(), databaseTimeOfIRI))
                    .filter(Optional::isPresent)
                    .filter(properties -> properties.orElseThrow(RuntimeException::new).size() <= 1)
                    .map(properties -> properties.orElseThrow(RuntimeException::new).stream().findAny())
                    .filter(Optional::isPresent)
                    .forEach(property -> ontology.removeIndividual(property.orElseThrow(RuntimeException::new).getObject().asOWLNamedIndividual()));

            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return objectProperties.stream().findAny().orElseThrow(RuntimeException::new).getSubject().asOWLNamedIndividual();
        }, trestleThreadPool);

    }


    @Override
    public Optional<UnionContributionResult> calculateSpatialUnionWithContribution(String datasetClassID, List<String> individualIRIs, int inputSR, double matchThreshold) {
        return this.spatialEngine.calculateSpatialUnionWithContribution(datasetClassID, individualIRIs, inputSR, matchThreshold);
    }

    @Override
    public Optional<List<SpatialComparisonReport>> compareTrestleObjects(String datasetID, String objectAID, List<String> comparisonObjectIDs, int inputSR, double matchThreshold) {
        return this.spatialEngine.compareTrestleObjects(datasetID, objectAID, comparisonObjectIDs, inputSR, matchThreshold);
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
            final List<CompletableFuture<T>> individualsFutureList = equivalentIndividuals
                    .stream()
                    .map(individual -> CompletableFuture.supplyAsync(() -> {
                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                        try {
                            return this.objectReader.readTrestleObject(clazz, individual.getIRI(), false, queryTemporal, null);
                        } finally {
                            this.ontology.returnAndCommitTransaction(tt);
                        }
                    }, this.objectThreadPool))
                    .collect(Collectors.toList());
            final CompletableFuture<List<T>> individualsFuture = sequenceCompletableFutures(individualsFutureList);
            return Optional.of(individualsFuture.join());
        } catch (Exception e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            logger.error("Unable to get equivalent objects for {} at {}", individuals, queryTemporal, e);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public List<String> searchForIndividual(String individualIRI) {
        return searchForIndividual(individualIRI, null, null);
    }

    @Override
    public List<String> searchForIndividual(String individualIRI, @Nullable String datasetClass, @Nullable Integer limit) {
        final OWLClass owlClass;
        if (datasetClass != null) {
            owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, datasetClass));
        } else {
            owlClass = null;
        }

        final CompletableFuture<List<String>> searchFuture = CompletableFuture.supplyAsync(() -> {
            final String query = qb.buildIndividualSearchQuery(individualIRI, owlClass, limit);
            final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
            try {
                final TrestleResultSet resultSet = ontology.executeSPARQLResults(query);
                return resultSet.getResults()
                        .stream()
                        .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                this.ontology.returnAndAbortWithForce(trestleTransaction);
                return ExceptionUtils.rethrow(e);
            } finally {
                this.ontology.returnAndCommitTransaction(trestleTransaction);
            }
        }, this.searchThreadPool);

        try {
            return searchFuture.get();
        } catch (InterruptedException e) {
            logger.error("Search interrupted");
            Thread.currentThread().interrupt();
            return ExceptionUtils.rethrow(e);
        } catch (ExecutionException e) {
            logger.error("Problem searching for {}", individualIRI, e);
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
    public Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<?> clazz, String wkt, double buffer, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal) {
        return this.spatialEngine.spatialIntersectIndividuals(clazz, wkt, buffer, atTemporal, dbTemporal);
    }

    @Override
    public TrestleIndividual getTrestleIndividual(String individualIRI) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individualIRI)));
    }

    private TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return this.individualEngine.getTrestleIndividual(individual);
    }

    @Override
    public Optional<Set<String>> STIntersectConcept(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        final String queryString;
        final OffsetDateTime atTemporal;
        final OffsetDateTime dbTemporal;
        if (validAt == null) {
            atTemporal = null;
        } else {
            atTemporal = parseTemporalToOntologyDateTime(validAt, ZoneOffset.UTC);
        }
        if (dbAt == null) {
            dbTemporal = OffsetDateTime.now();
        } else {
            dbTemporal = parseTemporalToOntologyDateTime(dbAt, ZoneOffset.UTC);
        }

        try {
            queryString = qb.buildTemporalSpatialConceptIntersection(wkt, buffer, strength, atTemporal, dbTemporal);
        } catch (UnsupportedFeatureException e) {
            logger.error("Database {} does not support spatial queries", this.spatialDalect);
            return Optional.empty();
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(queryString);
            final Set<String> intersectedConceptURIs = resultSet.getResults()
                    .stream()
                    .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                    .collect(Collectors.toSet());
            return Optional.of(intersectedConceptURIs);
        } catch (RuntimeException e) {
            logger.error("Problem intersecting spatial concept", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public <T> Optional<List<T>> getConceptMembers(Class<T> clazz, String conceptID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection) {


        final OWLClass datasetClass = trestleParser.classParser.getObjectClass(clazz);
        final String retrievalStatement = qb.buildConceptObjectRetrieval(datasetClass, parseStringToIRI(REASONER_PREFIX, conceptID), strength);

        final OffsetDateTime atTemporal;
        if (temporalIntersection != null) {
            atTemporal = parseTemporalToOntologyDateTime(temporalIntersection, ZoneOffset.UTC);
        } else {
            atTemporal = OffsetDateTime.now();
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        Set<String> individualIRIs = this.ontology.executeSPARQLResults(retrievalStatement)
                .getResults()
                .stream()
                .map(result -> result.getIndividual("m"))
                .filter(Optional::isPresent)
                .map(individual -> individual.get().toStringID())
                .collect(Collectors.toSet());

//        Try to retrieve the object members in an async fashion
//        We need to figure out the exists time of each object, so if the intersection point comes after the exists interval of the object, we grab the latest version of that object. Likewise intersection -> before -> object, grab the earliest
        final List<CompletableFuture<T>> completableFutureList = individualIRIs
                .stream()
                .map(iri -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final Temporal adjustedIntersection = this.getAdjustedQueryTemporal(iri, atTemporal, tt);
                        return this.readTrestleObject(clazz, iri, adjustedIntersection, null);
                    } catch (MissingOntologyEntity e) {
                        logger.error("Cannot find ontology individual {}", e.getIndividual(), e);
                        this.ontology.returnAndAbortTransaction(tt);
                        throw new CompletionException(e);
                    } catch (TrestleClassException e) {
                        logger.error("Unregistered class", e);
                        this.ontology.returnAndAbortTransaction(tt);
                        throw new CompletionException(e);
                    } finally {
                        this.ontology.returnAndCommitTransaction(tt);
                    }
                }, trestleThreadPool))
                .collect(Collectors.toList());
        final CompletableFuture<List<T>> conceptObjectsFuture = sequenceCompletableFutures(completableFutureList);
        try {
            List<T> objects = conceptObjectsFuture.get();
            return Optional.of(objects);
        } catch (InterruptedException e) {
            logger.error("Object retrieval for concept {}, interrupted", conceptID, e.getCause());
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Unable to retrieve all objects for concept {}", conceptID, e.getCause());
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public void addObjectToConcept(String conceptIRI, Object inputObject, ConceptRelationType relationType, double strength) {

        //        Create the concept relation
        final IRI concept = parseStringToIRI(REASONER_PREFIX, conceptIRI);
        final OWLNamedIndividual conceptIndividual = df.getOWLNamedIndividual(concept);
        final OWLNamedIndividual individual = this.trestleParser.classParser.getIndividual(inputObject);
        final IRI relationIRI = IRI.create(String.format("relation:%s:%s",
                extractTrestleIndividualName(concept),
                extractTrestleIndividualName(individual.getIRI())));
        final OWLNamedIndividual relationIndividual = df.getOWLNamedIndividual(relationIRI);
        final OWLClass relationClass = df.getOWLClass(trestleRelationIRI);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);

        try {
            //        Write the object
            this.writeTrestleObject(inputObject);
//        TODO(nrobison): Implement relation types
//        switch (relationType) {
//            case SEMANTIC:
//                relationClass = df.getOWLClass(semanticRelationIRI);
//                break;
//            case SPATIAL:
//                relationClass = df.getOWLClass(spatialRelationIRI);
//                break;
//            case TEMPORAL:
//                relationClass = df.getOWLClass(temporalRelationIRI);
//                break;
//            default:
//                relationClass = df.getOWLClass(trestleConceptIRI);
//                break;
//        }
//        Write the concept properties
            ontology.createIndividual(df.getOWLClassAssertionAxiom(relationClass, relationIndividual));
            ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                    df.getOWLObjectProperty(relationOfIRI),
                    relationIndividual,
                    individual));
            ontology.writeIndividualDataProperty(relationIndividual,
                    df.getOWLDataProperty(relationStrengthIRI),
                    df.getOWLLiteral(strength));

//        Write the relation to the concept
//            TODO(nrobison): This is gross, catching exceptions is really expensive.
            try {
                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(relatedToIRI),
                        relationIndividual,
                        conceptIndividual
                ));
            } catch (MissingOntologyEntity missingOntologyEntity) {
//            If the concept doesn't exist, create it.
                logger.debug("Missing concept {}, creating", missingOntologyEntity.getIndividual());
                ontology.createIndividual(df.getOWLClassAssertionAxiom(df.getOWLClass(trestleConceptIRI), conceptIndividual));
//            Try again
                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(relatedToIRI),
                        relationIndividual,
                        conceptIndividual));
            }
        } catch (MissingOntologyEntity | TrestleClassException e) {
            logger.error("Problem adding individual {} to concept {}", individual, conceptIndividual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public void writeObjectRelationship(Object subject, Object object, ObjectRelation relation) {
        this.writeObjectProperty(subject, object, df.getOWLObjectProperty(relation.getIRI()));
    }

    @Override
    public void writeSpatialOverlap(Object subject, Object object, String wkt) {
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
        this.ontology.createIndividual(overlapClassAssertion);
//        Write the overlap intersection
        final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(sOverlapIRI), overlapIndividual, df.getOWLLiteral(wkt, df.getOWLDatatype(WKTDatatypeIRI)));
        try {
            this.ontology.writeIndividualDataProperty(sOverlapAssertion);
        } catch (MissingOntologyEntity missingOntologyEntity) {
            logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
        }

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
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.getIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        this.ontology.createIndividual(overlapClassAssertion);
//        Write the overlap intersection
        final OWLDataPropertyAssertionAxiom sOverlapAssertion = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(tOverlapIRI), overlapIndividual, df.getOWLLiteral(temporalOverlap));
        try {
            this.ontology.writeIndividualDataProperty(sOverlapAssertion);
        } catch (MissingOntologyEntity missingOntologyEntity) {
            logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
        }

//        Write the subject relation
        final OWLObjectProperty overlapProperty = df.getOWLObjectProperty(overlapOfIRI);
        this.writeIndirectObjectProperty(overlapIndividual, subject, overlapProperty);

//        Write the object relation
        this.writeIndirectObjectProperty(overlapIndividual, object, overlapProperty);
        this.ontology.returnAndCommitTransaction(trestleTransaction);
    }

    /**
     * Write an indirect object property between a Java object and an intermediate OWL individual
     * The OWL individual must exist before calling this function, but the Java object is created if it doesn't exist.
     *
     * @param subject  - OWLNamedIndividual of intermediate OWL object
     * @param object   - Java object to write as object of assertion
     * @param property - OWLObjectProperty to assert
     */
    private void writeIndirectObjectProperty(OWLNamedIndividual subject, Object object, OWLObjectProperty property) {
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = df.getOWLObjectPropertyAssertionAxiom(property, subject, objectIndividual);
        try {
            this.ontology.writeIndividualObjectProperty(owlObjectPropertyAssertionAxiom);
        } catch (MissingOntologyEntity missingOntologyEntity) {
            logger.debug("Missing individual {}, creating", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            try {
                this.writeTrestleObject(object);
            } catch (TrestleClassException | MissingOntologyEntity e) {
                logger.error("Problem writing assertion for individual", objectIndividual, e);
            }
        }
    }

    /**
     * Write an object property assertion between two objects, writing them into the database if they don't exist.
     *
     * @param subject  - Java object to write as subject of assertion
     * @param object   - Java object to write as object of assertion
     * @param property - OWLObjectProperty to assert between the two objects
     */
    private void writeObjectProperty(Object subject, Object object, OWLObjectProperty property) {
        logger.debug("Writing relationship {} between {} (subject) and {} (object)", property, subject, object);
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.getIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.getIndividual(object);
        final OWLObjectPropertyAssertionAxiom objectRelationshipAssertion = df.getOWLObjectPropertyAssertionAxiom(property,
                subjectIndividual,
                objectIndividual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            ontology.writeIndividualObjectProperty(objectRelationshipAssertion);
        } catch (MissingOntologyEntity e) {
            logger.debug("Individual {} does not exist, creating", e.getIndividual(), e);
//            Do we need to write the subject, or the object?
//            Start with object, and then try for the subject
            if (e.getIndividual().equals(objectIndividual.toString())) {
                try {
                    this.writeTrestleObject(subject);
                } catch (TrestleClassException e1) {
                    logger.error("Class exception", e1);
                } catch (MissingOntologyEntity missingOntologyEntity) {
                    logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
                }
                try {
//                    Try to write again, if it fails, write the subject
                    ontology.writeIndividualObjectProperty(objectRelationshipAssertion);
                } catch (MissingOntologyEntity missingOntologyEntity) {
                    try {
                        this.writeTrestleObject(object);
                    } catch (TrestleClassException e2) {
                        logger.error("Class exception", e2);
                    } catch (MissingOntologyEntity m2) {
                        logger.error("Missing individual {}", m2.getIndividual(), m2);
                    }
                }
            }
        }

        this.ontology.returnAndCommitTransaction(trestleTransaction);
    }

    /**
     * Build a TrestleFact from a given OWLIndividual
     * Retrieves all the asserted properties and types of a given Individual, in their native forms.
     *
     * @param factIndividual    - OWLNamedIndividual to construct fact from
     * @param transactionObject - TrestleTransaction object that gets passed from the parent function
     * @return - TrestleFact
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<TrestleFact> buildTrestleFact(OWLNamedIndividual factIndividual, TrestleTransaction transactionObject) {
        return CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            logger.trace("Opened TrestleFact transaction");
            try {
                final Set<OWLDataPropertyAssertionAxiom> dataProperties = ontology.getAllDataPropertiesForIndividual(factIndividual);
//            Build fact pair
                final Optional<OWLDataPropertyAssertionAxiom> dataPropertyAssertion = dataProperties
                        .stream()
                        .filter(property -> !(property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalStartIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalEndIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalAtIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalPropertyIRI)))
                        .findFirst();

                final OWLDataPropertyAssertionAxiom assertion = dataPropertyAssertion.orElseThrow(() -> new TrestleMissingFactException(factIndividual));
                final Class<?> datatype = TypeConverter.lookupJavaClassFromOWLDatatype(assertion, null);
                final Object literalObject = TypeConverter.extractOWLLiteral(datatype, assertion.getObject());
//            Get valid time
                final Set<OWLDataPropertyAssertionAxiom> validTemporals = dataProperties
                        .stream()
                        .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI))
                        .collect(Collectors.toSet());
                final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromProperties(validTemporals, null, BLANK_TEMPORAL_ID);
//            Database time
                final Set<OWLDataPropertyAssertionAxiom> dbTemporals = dataProperties
                        .stream()
                        .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                                property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI))
                        .collect(Collectors.toSet());
                final Optional<TemporalObject> dbTemporal = TemporalObjectBuilder.buildTemporalFromProperties(dbTemporals, null, BLANK_TEMPORAL_ID);
                return new TrestleFact<>(
                        factIndividual.getIRI().toString(),
                        assertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                        literalObject,
                        validTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)),
                        dbTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)));
            } catch (Exception e) {
                logger.error("Error building Trestle Face {}", factIndividual, e);
                this.ontology.returnAndAbortTransaction(tt);
                throw new CompletionException(e.getCause());
            } finally {
                logger.trace("Committing Trestle Fact Transaction");
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, trestleThreadPool);
    }

    /**
     * Get temporal objects for given Fact
     *
     * @param individual        - Fact OWLNamedIndividual
     * @param temporalIRI       - IRI of temporalProperties to retrieve
     * @param transactionObject - TrestleTransaction object that gets passed from the parent function
     * @return - Completable future of Optional TemporalObject of given Fact individual
     */
    private CompletableFuture<Optional<TemporalObject>> getFactTemporal(OWLNamedIndividual individual, IRI temporalIRI, TrestleTransaction transactionObject) {
        return CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            final Optional<List<OWLObjectPropertyAssertionAxiom>> temporalIndividual = ontology.getIndividualObjectProperty(individual, temporalIRI);
            this.ontology.returnAndCommitTransaction(tt);
            return temporalIndividual;
        }, trestleThreadPool)
                .thenApply(temporalProperties -> temporalProperties.orElseThrow(() -> new TrestleMissingFactException(individual, temporalIRI)).stream().findFirst())
                .thenApply(temporalProperty -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
                    final OWLNamedIndividual temporalIndividual = temporalProperty.orElseThrow(() -> new TrestleMissingFactException(individual, temporalIRI)).getObject().asOWLNamedIndividual();
                    final Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getAllDataPropertiesForIndividual(temporalIndividual);
                    this.ontology.returnAndCommitTransaction(tt);
                    return new TemporalPropertiesPair(temporalIndividual, allDataPropertiesForIndividual);
                })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()));
    }

    /**
     * Get the adjusted {@link Temporal} for a given individual
     * If temporal occurs AFTER the existence interval of the object, then we retrieve the LATEST state of the object
     * If it occurs BEFORE, we return the earliest state of the object
     *
     * @param individual         - {@link String} individual ID
     * @param atTemporal         - {@link Temporal} temporal to adjust to
     * @param trestleTransaction - {@link Nullable} {@link TrestleTransaction}
     * @return - {@link Temporal}
     */
    @SuppressWarnings({"squid:S3655"})
    private Temporal getAdjustedQueryTemporal(String individual, OffsetDateTime atTemporal, @Nullable TrestleTransaction trestleTransaction) {
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
        try {
            final Set<OWLDataPropertyAssertionAxiom> temporalsForIndividual = this.ontology.getTemporalsForIndividual(df.getOWLNamedIndividual(IRI.create(individual)));
            final Optional<TemporalObject> individualExistsTemporal = TemporalObjectBuilder.buildTemporalFromProperties(temporalsForIndividual, null, BLANK_TEMPORAL_ID);
            final TemporalObject temporalObject = individualExistsTemporal.orElseThrow(() -> new RuntimeException(String.format("Unable to get exists temporals for %s", individual)));
            final int compared = temporalObject.compareTo(atTemporal);
            final Temporal adjustedIntersection;
            if (compared == -1) { // Intersection is after object existence, get the latest version
                if (temporalObject.isInterval()) {
//                            we need to do a minus one precision unit, because the intervals are exclusive on the end {[)}
                    adjustedIntersection = (Temporal) temporalObject.asInterval().getAdjustedToTime(-1).get();
                } else {
                    adjustedIntersection = temporalObject.asPoint().getPointTime();
                }
            } else if (compared == 0) { // Intersection is during existence, continue
                adjustedIntersection = atTemporal;
            } else { // Intersection is before object existence, get earliest version
                adjustedIntersection = temporalObject.getIdTemporal();
            }
            return adjustedIntersection;
        } finally {
            this.ontology.returnAndCommitTransaction(tt);
        }
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

    /**
     * Write temporal object into the database, optionally override given scope
     * If no OWLNamedIndividual is given, don't write any association
     *
     * @param temporal-                   TemporalObject to create
     * @param individual                  - Optional OWLNamedIndividual to associate with temporal
     * @param overrideTemporalScope       - Optionally override scope of temporal object
     * @param overrideTemporalAssociation - Optionally override temporal association
     * @throws MissingOntologyEntity - Throws if it can't find the temporal to write properties on
     */
    @Deprecated
    private void writeTemporal(TemporalObject temporal, @Nullable OWLNamedIndividual individual, @Nullable TemporalScope overrideTemporalScope, @Nullable IRI overrideTemporalAssociation) throws MissingOntologyEntity {
//        Write the object
        final IRI temporalIRI = IRI.create(REASONER_PREFIX, temporal.getID());
        ontology.createIndividual(temporalIRI, temporalClassIRI);
        TemporalScope scope = temporal.getScope();
        TemporalType type = temporal.getType();

        if (overrideTemporalScope != null) {
            scope = overrideTemporalScope;
        }

//        Write the properties using the scope and type variables set above
        if (type == TemporalType.INTERVAL) {
            if (scope == TemporalScope.VALID) {
//                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        temporalValidFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            temporalValidToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            } else {
//                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalExistsToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalValidAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            } else {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            }
        }

//        Associate with individual
        if (individual != null) {
            if (overrideTemporalAssociation == null) {
                ontology.writeIndividualObjectProperty(
                        individual.getIRI(),
                        StaticIRI.hasTemporalIRI,
                        temporalIRI);
            } else {
                ontology.writeIndividualObjectProperty(
                        individual.getIRI(),
                        overrideTemporalAssociation,
                        temporalIRI);
            }
        }
    }

    @Override
    public Set<String> getAvailableDatasets() {

        final String datasetQuery = qb.buildDatasetQuery();
        final TrestleResultSet resultSet = ontology.executeSPARQLResults(datasetQuery);
        Set<OWLClass> datasetsInOntology = resultSet
                .getResults()
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
        final OWLClass owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, owlClassString));
        return this.trestleParser.classRegistry.lookupClass(owlClass);
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException {
        return exportDataSetObjects(inputClass, objectID, null, null, exportType);
    }

    @Override
    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException {

//        Build shapefile schema
//        TODO(nrobison): Extract type from wkt
//        FIXME(nrobison): Shapefile schema doesn't support multiple languages. Need to figure out how to flatten
        final ShapefileSchema shapefileSchema = new ShapefileSchema(MultiPolygon.class);
        final Optional<List<OWLDataProperty>> propertyMembers = this.trestleParser.classBuilder.getPropertyMembers(inputClass, true);
        propertyMembers.ifPresent(owlDataProperties -> owlDataProperties.forEach(property -> shapefileSchema.addProperty(this.trestleParser.classParser.matchWithClassMember(inputClass, property.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, property))));

//        Now the temporals
        final Optional<List<OWLDataProperty>> temporalProperties = trestleParser.temporalParser.getTemporalsAsDataProperties(inputClass);
        temporalProperties.ifPresent(owlDataProperties -> owlDataProperties.forEach(temporal -> shapefileSchema.addProperty(this.trestleParser.classParser.matchWithClassMember(inputClass, temporal.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, temporal))));


        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final List<CompletableFuture<Optional<TSIndividual>>> completableFutures = objectID
                .stream()
                .map(id -> IRIUtils.parseStringToIRI(REASONER_PREFIX, id))
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final T object = readTrestleObject(inputClass, id, false, validAt, databaseAt);
                        return Optional.of(object);
                    } catch (NoValidStateException e) {
                        this.ontology.returnAndAbortTransaction(tt);
                        return Optional.empty();
                    } finally {
                        this.ontology.returnAndCommitTransaction(tt);
                    }
                }, trestleThreadPool))
                .map(objectFuture -> objectFuture.thenApply(object -> parseIndividualToShapefile(object, shapefileSchema)))
                .collect(Collectors.toList());

        final CompletableFuture<List<Optional<TSIndividual>>> sequencedFutures = sequenceCompletableFutures(completableFutures);

        try {
            final List<TSIndividual> individuals = sequencedFutures
                    .get()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            switch (exportType) {
                case SHAPEFILE: {
                    final ShapefileExporter shapeFileExporter = new ShapefileExporter.ShapefileExporterBuilder(shapefileSchema.getGeomName(), shapefileSchema.getGeomType(), shapefileSchema).build();
                    return shapeFileExporter.writePropertiesToByteBuffer(individuals, null);
                }
                case GEOJSON: {
                    return new GeoJsonExporter().writePropertiesToByteBuffer(individuals, null);
                }
                case KML: {
                    return new KMLExporter(false).writePropertiesToByteBuffer(individuals, null);
                }
                case KMZ: {
                    return new KMLExporter(true).writePropertiesToByteBuffer(individuals, null);
                }
                default: {
                    throw new IllegalArgumentException(String.format("Cannot export to %s format", exportType.toString()));
                }
            }


        } catch (ExecutionException e) {
            logger.error("Error constructing object", e.getCause());
            return ExceptionUtils.rethrow(e.getCause());
        } catch (InterruptedException e) {
            logger.error("Object construction excepted", e.getCause());
            Thread.currentThread().interrupt();
            return ExceptionUtils.rethrow(e.getCause());
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T extends @NonNull Object> Optional<TSIndividual> parseIndividualToShapefile(Optional<T> objectOptional, ShapefileSchema shapefileSchema) {
        if (!objectOptional.isPresent()) {
            return Optional.empty();
        }
        final T object = objectOptional.get();
//        if (objectOptional.isPresent()) {
//            final T object = objectOptional.get();
        final Class<?> inputClass = object.getClass();
        final Optional<OWLDataPropertyAssertionAxiom> spatialProperty = trestleParser.classParser.getSpatialFact(object);
        if (!spatialProperty.isPresent()) {
            logger.error("Individual is not a spatial object");
            return Optional.empty();
        }
        final TSIndividual individual = new TSIndividual(spatialProperty.get().getObject().getLiteral(), shapefileSchema);
//                    Data properties, filtering out the spatial members
        final Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = trestleParser.classParser.getFacts(object, true);
        owlDataPropertyAssertionAxioms.ifPresent(owlDataPropertyAssertionAxioms1 -> owlDataPropertyAssertionAxioms1.forEach(property -> {
            final Class<@NonNull ?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, object.getClass());
            final Object literal = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
            individual.addProperty(this.trestleParser.classParser.matchWithClassMember(inputClass, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                    literal);
        }));
//                    Temporals
        final Optional<List<TemporalObject>> temporalObjects = trestleParser.temporalParser.getTemporalObjects(object);
        if (temporalObjects.isPresent()) {
            final TemporalObject temporalObject = temporalObjects.get().get(0);
            if (temporalObject.isInterval()) {
                final IntervalTemporal intervalTemporal = temporalObject.asInterval();
                final String startName = intervalTemporal.getStartName();
                individual.addProperty(this.trestleParser.classParser.matchWithClassMember(inputClass, intervalTemporal.getStartName()), intervalTemporal.getFromTime().toString());
                final Optional toTime = intervalTemporal.getToTime();
                if (toTime.isPresent()) {
                    final Temporal to = (Temporal) toTime.get();
                    individual.addProperty(this.trestleParser.classParser.matchWithClassMember(inputClass, intervalTemporal.getEndName()), to.toString());
                }
            } else {
                final PointTemporal pointTemporal = temporalObject.asPoint();
                individual.addProperty(pointTemporal.getParameterName(), pointTemporal.getPointTime().toString());
            }
        }
        return Optional.of(individual);
    }

    private boolean checkObjectRelation(OWLNamedIndividual firstIndividual, OWLNamedIndividual secondIndividual) {

//        This should get all the Concept Relations and the individuals related to the first individual, and it's symmetric
        final Optional<List<OWLObjectPropertyAssertionAxiom>> relatedToProperties = ontology.getIndividualObjectProperty(firstIndividual, hasRelationIRI);
        if (relatedToProperties.isPresent()) {
            final Optional<OWLIndividual> isRelated = relatedToProperties.get()
                    .stream()
                    .map(OWLPropertyAssertionAxiom::getObject)
                    .filter(p -> p.equals(secondIndividual))
                    .findFirst();

            return isRelated.isPresent();
        }
        return false;
    }

    /**
     * Validates the ontology name to make sure it doesn't include unsupported characters
     *
     * @param ontologyName - String to validate
     * @throws InvalidOntologyName - Exception thrown if the name is invalid
     */
    private static void validateOntologyName(String ontologyName) throws InvalidOntologyName {
        if (ontologyName.contains("-")) {
            throw new InvalidOntologyName("-");
        } else if (ontologyName.length() > 90) {
            throw new InvalidOntologyName(ontologyName.length());
        }
    }
}
