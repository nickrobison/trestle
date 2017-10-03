package com.nickrobison.trestle.reasoner;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.exporter.ShapefileExporter;
import com.nickrobison.trestle.exporter.ShapefileSchema;
import com.nickrobison.trestle.exporter.TSIndividual;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.TrestleOntologyModule;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.events.TrestleEventException;
import com.nickrobison.trestle.reasoner.exceptions.*;
import com.nickrobison.trestle.reasoner.merge.MergeScript;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeConflict;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeException;
import com.nickrobison.trestle.reasoner.parser.*;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.reasoner.utils.TemporalPropertiesPair;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.*;
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
import org.apache.commons.lang3.ClassUtils;
import org.checkerframework.checker.nullness.qual.KeyFor;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;
import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaExceptionUtil.recoverExceptionType;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.iri.IRIVersion.V1;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;
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
    private static final OffsetDateTime TEMPORAL_MAX_VALUE = LocalDate.of(3000, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

    private final String REASONER_PREFIX;
    private final ITrestleOntology ontology;
    private final Map<OWLClass, Class<?>> registeredClasses = new HashMap<>();
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(datasetClassIRI);
    private final QueryBuilder qb;
    private final QueryBuilder.Dialect spatialDalect;
    private final TrestleParser trestleParser;
    private final TrestleMergeEngine mergeEngine;
    private final TrestleEventEngine eventEngine;
    private final EqualityEngine equalityEngine;
    private final ContainmentEngine containmentEngine;
    private final Config trestleConfig;
    private final TrestleCache trestleCache;
    private final Metrician metrician;
    private final ExecutorService trestleThreadPool;
    private final TrestleExecutorService objectThreadPool;

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
//        metrician = null;
        metrician = injector.getInstance(Metrician.class);

//        Create our own thread pools to help isolate processes
        trestleThreadPool = TrestleExecutorService.executorFactory(builder.ontologyName.orElse("default"), trestleConfig.getInt("threading.default-pool.size"), this.metrician);
        objectThreadPool = TrestleExecutorService.executorFactory("object-pool", trestleConfig.getInt("threading.object-pool.size"), this.metrician);

//        Validate ontology name
        try {
            validateOntologyName(builder.ontologyName.orElse(DEFAULTNAME));
        } catch (InvalidOntologyName e) {
            logger.error("{} is an invalid ontology name", builder.ontologyName.orElse(DEFAULTNAME), e);
            throw new IllegalArgumentException("invalid ontology name", e);
        }

//        Register some constructor functions
        TypeConverter.registerTypeConstructor(UUID.class, df.getOWLDatatype(UUIDDatatypeIRI), (uuidString) -> UUID.fromString(uuidString.toString()));

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
        this.mergeEngine = injector.getInstance(TrestleMergeEngine.class);
        this.eventEngine = injector.getInstance(TrestleEventEngine.class);
        this.equalityEngine = injector.getInstance(EqualityEngine.class);
        this.containmentEngine = injector.getInstance(ContainmentEngine.class);

//            validate the classes
        builder.inputClasses.forEach(clazz -> {
            try {
                ClassRegister.ValidateClass(clazz);
                this.registeredClasses.put(trestleParser.classParser.getObjectClass(clazz), clazz);
            } catch (TrestleClassException e) {
                logger.error("Cannot validate class {}", clazz, e);
            }
        });

        trestleCache = injector.getInstance(TrestleCache.class);

////        Setup the query builder
//        if (ontology instanceof OracleOntology) {
//            spatialDalect = QueryBuilder.Dialect.ORACLE;
//        } else if (ontology instanceof VirtuosoOntology) {
//            spatialDalect = QueryBuilder.Dialect.VIRTUOSO;
//        } else if (ontology instanceof LocalOntology) {
//            spatialDalect = QueryBuilder.Dialect.JENA;
////        } else if (ontology instanceof StardogOntology) {
////            spatialDalect = QueryBuilder.Dialect.STARDOG;
//        } else {
//            spatialDalect = QueryBuilder.Dialect.SESAME;
//        }

//        This is probably a terrible idea, but whatever.
//        qb = injector
//                .createChildInjector(new QueryBuilderModule(spatialDalect,
//                        this.ontology.getUnderlyingPrefixManager()))
//                .getInstance(QueryBuilder.class);
//        qb = new QueryBuilder(spatialDalect, ontology.getUnderlyingPrefixManager());
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
        }
//        Check to make sure we don't have any open transactions
        final long openTransactionCount = this.ontology.getCurrentlyOpenTransactions();
        if (openTransactionCount > 0) {
            logger.error("{} currently open read/write transactions!", openTransactionCount);
        }
        this.trestleCache.shutdown(delete);
        this.ontology.close(delete);
        this.metrician.shutdown();
    }

    @Override
    public void registerTypeConstructor(Class<?> clazz, OWLDatatype datatype, Function constructorFunc) {
        TypeConverter.registerTypeConstructor(clazz, datatype, constructorFunc);
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
        return this.equalityEngine;
    }

    @Override
    public ContainmentEngine getContainmentEngine() {
        return this.containmentEngine;
    }

    @Override
    public Map<String, String> getReasonerPrefixes() {
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put(":", this.REASONER_PREFIX);
        prefixes.putAll(this.getUnderlyingOntology().getUnderlyingPrefixManager().getPrefixName2PrefixMap());
        return prefixes;
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
//    Validate Methods
//    ----------------------------

    private void checkRegisteredClass(Class<?> clazz) throws UnregisteredClassException {
        if (!this.registeredClasses.containsValue(clazz)) {
            throw new UnregisteredClassException(clazz);
        }
    }

    @Timed
    private boolean checkExists(IRI individualIRI) {
        return ontology.containsResource(individualIRI);
    }

//    ----------------------------
//    WRITE Methods
//    ----------------------------


    @Override
    public void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        writeTrestleObject(inputObject, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).build();
        } else {
            databaseTemporal = TemporalObjectBuilder.database().from(startTemporal).to(endTemporal).build();
        }
        writeTrestleObject(inputObject, databaseTemporal);
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
    private void writeTrestleObject(Object inputObject, @Nullable TemporalObject databaseTemporal) throws UnregisteredClassException, MissingOntologyEntity {
        final Class aClass = inputObject.getClass();
        checkRegisteredClass(aClass);

        final OWLNamedIndividual owlNamedIndividual = trestleParser.classParser.getIndividual(inputObject);

//            Create the database time object, set to UTC, of course
        final TemporalObject dTemporal;
        if (databaseTemporal == null) {
            dTemporal = TemporalObjectBuilder.database().from(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)).build();
        } else {
            dTemporal = databaseTemporal;
        }

        //        Get the temporal
        final Optional<List<TemporalObject>> temporalObjects = trestleParser.temporalParser.getTemporalObjects(inputObject);
        TemporalObject objectTemporal = temporalObjects.orElseThrow(() -> new RuntimeException(String.format("Cannot parse temporals for %s", owlNamedIndividual))).get(0);
        TemporalObject factTemporal = objectTemporal.castTo(TemporalScope.VALID);

//        Merge operation, if the object exists
        // temporal merging occurs by default but may be disabled in the configuration
        if (this.mergeEngine.mergeOnLoad() && checkExists(owlNamedIndividual.getIRI())) {
            final Timer.Context mergeTimer = this.metrician.registerTimer("trestle-merge-timer").time();
            final Optional<List<OWLDataPropertyAssertionAxiom>> individualFactsOptional = trestleParser.classParser.getFacts(inputObject);
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
                    }, this.objectThreadPool);

//                    Get object existence information
                    @SuppressWarnings("Duplicates") final CompletableFuture<Optional<TemporalObject>> existsFuture = readObjectExistence(owlNamedIndividual, trestleTransaction, this.objectThreadPool, !this.mergeEngine.existenceEnabled());


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
                    }, this.objectThreadPool);
                    mergeFuture.join();

                }
            } catch (RuntimeException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Error while writing object {}", owlNamedIndividual, e);
                recoverExceptionType(e, TrestleMergeConflict.class, TrestleMergeException.class);
            } finally {
                ontology.returnAndCommitTransaction(trestleTransaction);
                mergeTimer.stop();
            }
        } else {
//        If the object doesn't exist, continue with the simple write

//        Write the class
            final OWLClass owlClass = trestleParser.classParser.getObjectClass(inputObject);
//        Open the transaction
            final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(true);
            ontology.associateOWLClass(owlClass, datasetClass);
//        Write the individual
            ontology.createIndividual(owlNamedIndividual, owlClass);
            writeTemporal(objectTemporal, owlNamedIndividual);

//        Write the data facts
            final Optional<List<OWLDataPropertyAssertionAxiom>> individualFacts = trestleParser.classParser.getFacts(inputObject);
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

            ontology.returnAndCommitTransaction(trestleTransaction);
        }

//        Invalidate the cache
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, REASONER_PREFIX, owlNamedIndividual.toStringID(), null,
                parseTemporalToOntologyDateTime(factTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(dTemporal.getIdTemporal(), ZoneOffset.UTC));
        logger.debug("Purging {} from the cache", individualIRI);
        trestleCache.deleteTrestleObject(individualIRI);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom) {
        addFactToTrestleObjectImpl(clazz, individual, factName, value, validAt, null, null, databaseFrom);
    }

    @Override
    public void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom) {
        addFactToTrestleObjectImpl(clazz, individual, factName, value, null, validFrom, validTo, databaseFrom);
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
        final OWLNamedIndividual owlNamedIndividual = df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual));
//        Parse String to Fact IRI
        final Optional<IRI> factIRI = this.trestleParser.classParser.getFactIRI(clazz, factName);
        if (!factIRI.isPresent()) {
            logger.error("Cannot parse {} for individual {}", factName, individual);
            throw new TrestleMissingFactException(owlNamedIndividual, parseStringToIRI(REASONER_PREFIX, factName));
        }
        final OWLDataProperty owlDataProperty = df.getOWLDataProperty(factIRI.get());

//        Validate that we have the correct type
        final Optional<Class<?>> factDatatypeOptional = this.trestleParser.classParser.getFactDatatype(clazz, factName);
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
            }, this.objectThreadPool);


//                    Get object existence information
            @SuppressWarnings("Duplicates") final CompletableFuture<Optional<TemporalObject>> existsFuture = readObjectExistence(owlNamedIndividual,
                    trestleTransaction,
                    this.objectThreadPool,
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

            }, this.objectThreadPool);
            mergeFuture.join();
        } catch (RuntimeException e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            logger.error("Unable to add fact {} to object {}", factName, owlNamedIndividual, e);
            recoverExceptionType(e, TrestleMergeConflict.class, TrestleMergeException.class);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }


//        Update the cache
        final TrestleIRI individualIRI = IRIBuilder.encodeIRI(V1, REASONER_PREFIX, individual, null,
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
                    REASONER_PREFIX,
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


//    ----------------------------
//    READ Methods
//    ----------------------------

//    ----------------------------
//    String Methods
//    ----------------------------

    @Override
    public <T> @NonNull T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException {
        return readTrestleObject(datasetClassID, objectID, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NonNull T readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException {
//        Lookup class
        final OWLClass datasetClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, datasetClassID));
        final Optional<@KeyFor("this.registeredClasses") OWLClass> matchingClass = this.registeredClasses
                .keySet()
                .stream()
                .filter(owlclass -> owlclass.equals(datasetClass))
                .findFirst();

        if (!matchingClass.isPresent()) {
            throw new MissingOntologyEntity("Cannot find matching class for: ", datasetClass);
        }

        final Class<@NonNull T> aClass = (Class<@NonNull T>) this.registeredClasses.get(matchingClass.get());
        return readTrestleObject(aClass, objectID, validTemporal, databaseTemporal);
    }

    @Override
    public <T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull String objectID) throws TrestleClassException, MissingOntologyEntity {
        return readTrestleObject(clazz, objectID, null, null);
    }


    @Override
    @SuppressWarnings({"argument.type.incompatible", "dereference.of.nullable"})
    public <@NonNull T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity {

        final IRI individualIRI = parseStringToIRI(REASONER_PREFIX, objectID);
        return readTrestleObject(clazz, individualIRI, false, validTemporal, databaseTemporal);
    }

//    ----------------------------
//    IRI Methods
//    ----------------------------

    /**
     * ReadAsObject interface, builds the default database temporal, optionally returns the object from the cache
     * Returns the currently valid facts, at the current database time
     *
     * @param clazz         - Java class of type T to return
     * @param individualIRI - IRI of individual
     * @param bypassCache   - Bypass cache?
     * @param <T>           - Java class to return
     * @return - Java object of type T
     */
    <T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, boolean bypassCache) {
        return readTrestleObject(clazz, individualIRI, bypassCache, null, null);
    }

    /**
     * ReadAsObject interface, (optionally) building the database temporal and retrieving from the cache
     * Returns the state of the object at the specified valid/database point
     *
     * @param clazz         - Java class of type T to return
     * @param individualIRI - IRI of individual to return
     * @param bypassCache   - Bypass cache access?
     * @param startTemporal - Optional ValidAt temporal
     * @param endTemporal   - Optional DatabaseAt temporal
     * @param <T>           - Java class to return
     * @return - Java object of type T
     */
    @SuppressWarnings({"return.type.incompatible", "argument.type.incompatible", "unchecked"})
    <@NonNull T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt) {
        logger.debug("Reading {}", individualIRI);

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
        final TrestleIRI trestleIRI = IRIBuilder.encodeIRI(V1, REASONER_PREFIX, individualIRI.getIRIString(), null,
                parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC),
                parseTemporalToOntologyDateTime(databaseTemporal.getIdTemporal(), ZoneOffset.UTC));

//        Try from cache first
        final @Nullable T individual = this.trestleCache.getTrestleObject(clazz, trestleIRI);
        if (individual != null) {
            return individual;
        }
        logger.debug("Individual is null, continuing");

//        final Optional<@NonNull T> constructedObject = readTrestleObjectImpl(clazz, individualIRI, validTemporal, databaseTemporal);
        final Optional<TrestleObjectResult<@NonNull T>> constructedObject = readTrestleObjectImpl(clazz, individualIRI, validTemporal, databaseTemporal);
        if (constructedObject.isPresent()) {
            logger.debug("Finished reading {}", individualIRI);
//            Write back to index
            final TrestleObjectResult<@NonNull T> value = constructedObject.get();
            try {
                this.trestleCache.writeTrestleObject(trestleIRI, value.getValidFrom(), value.getValidTo(), value.getDbFrom(), value.getDbTo(), value.getObject());
            } catch (Exception e) {
                logger.error("Unable to write Trestle Object {} to cache", individualIRI, e);
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
    @SuppressWarnings({"method.invocation.invalid"})
    private <@NonNull T> Optional<TrestleObjectResult<@NonNull T>> readTrestleObjectImpl(Class<@NonNull T> clazz, @NonNull IRI individualIRI, PointTemporal<?> validTemporal, PointTemporal<?> databaseTemporal) {
        logger.trace("Reading individual {} at {}/{}", individualIRI, validTemporal, databaseTemporal);

//        Contains class?
        try {
            checkRegisteredClass(clazz);
        } catch (UnregisteredClassException e) {
            logger.error("Unregistered class", e);
            throw new CompletionException(e);
        }

//        Do some things before opening a transaction
        final Optional<List<OWLDataProperty>> dataProperties = ClassBuilder.getPropertyMembers(clazz);

//        If no temporals are provided, perform the intersection at query time.
        final OffsetDateTime dbAtTemporal;
        final OffsetDateTime validAtTemporal;
        dbAtTemporal = parseTemporalToOntologyDateTime(databaseTemporal.getPointTime(), ZoneOffset.UTC);
        validAtTemporal = parseTemporalToOntologyDateTime(validTemporal.getPointTime(), ZoneOffset.UTC);

//            Get the temporal objects to figure out the correct return type
        final Class<? extends Temporal> baseTemporalType = TemporalParser.getTemporalType(clazz);

//        Build the fact query
        final String factQuery = qb.buildObjectFactRetrievalQuery(validAtTemporal, dbAtTemporal, true, null, df.getOWLNamedIndividual(individualIRI));

        final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(false);

//        Figure out its name
        if (!checkExists(individualIRI)) {
            logger.error("Missing individual {}", individualIRI);
            return Optional.empty();
        }

        if (dataProperties.isPresent()) {

//            Facts
            final CompletableFuture<List<TrestleFact>> factsFuture = CompletableFuture.supplyAsync(() -> {
                final Instant individualRetrievalStart = Instant.now();
                final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
                TrestleResultSet resultSet = ontology.executeSPARQLResults(factQuery);
                ontology.returnAndCommitTransaction(tt);
//                final Set<OWLDataPropertyAssertionAxiom> objectFacts = ontology.getFactsForIndividual(df.getOWLNamedIndividual(individualIRI), validAtTemporal, dbAtTemporal, true);
//                logger.debug("Retrieved {} facts for {}", resultSet.getResults().size(), individualIRI);
                if (resultSet.getResults().isEmpty()) {
                    throw new NoValidStateException(individualIRI, validAtTemporal, dbAtTemporal);
                }
                final Instant individualRetrievalEnd = Instant.now();
                logger.debug("Retrieving {} facts took {} ms", resultSet.getResults().size(), Duration.between(individualRetrievalStart, individualRetrievalEnd).toMillis());
                return resultSet;
            }, objectThreadPool)
                    .thenApply(resultSet -> {
//                        From the resultSet, build the Facts
                        return resultSet.getResults()
                                .stream()
                                .map(result -> {
                                    final OWLDataPropertyAssertionAxiom assertion = df.getOWLDataPropertyAssertionAxiom(
                                            df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new RuntimeException("Unable to get individual")).toStringID()),
                                            result.getIndividual("individual").orElseThrow(() -> new RuntimeException("Unable to get individual")),
                                            result.getLiteral("object").orElseThrow(() -> new RuntimeException("Unable to get individual")));
//                                    Get valid temporal
                                    final Optional<TemporalObject> factValidTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.VALID, result.getLiteral("va"), result.getLiteral("vf"), result.getLiteral("vt"));
//                                    Get database temporal
                                    final Optional<TemporalObject> factDatabaseTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.DATABASE, Optional.empty(), result.getLiteral("df"), result.getLiteral("dt"));
                                    //noinspection unchecked
                                    return new TrestleFact<>(
                                            clazz,
                                            assertion,
                                            factValidTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact valid temporal")),
                                            factDatabaseTemporal.orElseThrow(() -> new RuntimeException("Unable to build fact database temporal")));
                                })
                                .collect(Collectors.toList());
                    });
//            TODO(nrobison): This should be collapsed into a single async call. We can do both of them at the same time, since all the properties are on the same individual
//            Get the temporals
            final CompletableFuture<Optional<TemporalObject>> temporalFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
//                final Set<OWLDataPropertyAssertionAxiom> properties = ontology.getFactsForIndividual(df.getOWLNamedIndividual(individualIRI), validAtTemporal, dbAtTemporal, false);
                final Set<OWLDataPropertyAssertionAxiom> properties = ontology.getTemporalsForIndividual(df.getOWLNamedIndividual(individualIRI));
                ontology.returnAndCommitTransaction(tt);
                return properties;
            }, objectThreadPool)
                    .thenApply(temporalProperties -> TemporalObjectBuilder.buildTemporalFromProperties(temporalProperties, baseTemporalType, clazz));


//            Constructor arguments
            final CompletableFuture<TrestleObjectState> argumentsFuture = factsFuture.thenCombineAsync(temporalFuture, (facts, temporals) -> {
                logger.debug("In the arguments future");
                final ConstructorArguments constructorArguments = new ConstructorArguments();
                facts.forEach(fact -> constructorArguments.addArgument(
                        ClassParser.matchWithClassMember(clazz, fact.getName(), fact.getLanguage()),
                        fact.getJavaClass(),
                        fact.getValue()));
                if (!temporals.isPresent()) {
                    throw new RuntimeException(String.format("Cannot restore temporal from ontology for %s", individualIRI));
                }
//            Add the temporal to the constructor args
                final TemporalObject temporal = temporals.get();
                if (temporal.isInterval()) {
                    final IntervalTemporal intervalTemporal = temporal.asInterval();
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, intervalTemporal.getStartName()),
                            intervalTemporal.getBaseTemporalType(),
                            intervalTemporal.getFromTime());
                    if (!intervalTemporal.isDefault() && intervalTemporal.getToTime().isPresent()) {
                        constructorArguments.addArgument(
                                ClassParser.matchWithClassMember(clazz, intervalTemporal.getEndName()),
                                intervalTemporal.getBaseTemporalType(),
                                intervalTemporal.getToTime().get());
                    }
                } else {
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, temporal.asPoint().getParameterName()),
                            temporal.asPoint().getBaseTemporalType(),
                            temporal.asPoint().getPointTime());
                }
//                Get the minimum temporal ranges
//                Valid first
                Comparator<Temporal> temporalComparator = (t1, t2) -> ((Comparable) t1).compareTo((Comparable) t2);
                final Optional<Temporal> validStart = facts
                        .stream()
                        .map(TrestleFact::getValidTemporal)
                        .map(TemporalObject::getIdTemporal)
                        .max(temporalComparator);

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
                        .min(temporalComparator)
                        .map(Temporal.class::cast);
//                Database temporal, next
                final Optional<Temporal> dbStart = facts
                        .stream()
                        .map(TrestleFact::getDatabaseTemporal)
                        .map(TemporalObject::getIdTemporal)
                        .max(temporalComparator)
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
                        .min(temporalComparator)
                        .map(Temporal.class::cast);

                return new TrestleObjectState(constructorArguments, validStart.get(), validEnd.get(), dbStart.get(), dbEnd.get());
            }, objectThreadPool);

            final TrestleObjectState objectState;
            try {
                objectState = argumentsFuture.get();
            } catch (InterruptedException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Read object {} interrupted", individualIRI, e);
                return Optional.empty();
            } catch (ExecutionException e) {
                ontology.returnAndAbortTransaction(trestleTransaction);
                logger.error("Execution exception when reading object {}", individualIRI, e);
                return Optional.empty();
            } finally {
                ontology.returnAndCommitTransaction(trestleTransaction);
            }
            if (objectState == null) {
                logger.error("Object state is null, error must have occured");
                return Optional.empty();
            }
            try {
                final @NonNull T constructedObject = ClassBuilder.constructObject(clazz, objectState.getArguments());
                return Optional.of(new TrestleObjectResult<>(individualIRI, constructedObject, objectState.getMinValidFrom(), objectState.getMinValidTo(), objectState.getMinDatabaseFrom(), objectState.getMinDatabaseTo()));
            } catch (MissingConstructorException e) {
                logger.error("Problem with constructor", e);
                return Optional.empty();
            }
        } else {
            ontology.returnAndAbortTransaction(trestleTransaction);
            throw new RuntimeException("No data properties, not even trying");
        }
    }

    @Override
    public Optional<List<Object>> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {
//        Parse String to Fact IRI
        final Optional<IRI> factIRI = this.trestleParser.classParser.getFactIRI(clazz, factName);
        if (!factIRI.isPresent()) {
            logger.error("Cannot parse {} for individual {}", individual, factName);
            return Optional.empty();
        }

        return getFactValues(clazz,
                df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual)),
                df.getOWLDataProperty(factIRI.get()), validStart, validEnd, databaseTemporal);
    }

    @Override
    public Optional<List<Object>> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal) {

        final Optional<Class<@NonNull ?>> datatypeOptional = this.trestleParser.classParser.getFactDatatype(clazz, factName.getIRI().toString());

        if (!datatypeOptional.isPresent()) {
            logger.warn("Individual {} has no Fact {}", individual, factName);
            return Optional.empty();
        }

        Class<@NonNull ?> datatype = datatypeOptional.get();
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
        final List<Object> factValues = resultSet.getResults()
                .stream()
                .map(result -> result.getLiteral("value"))
                .filter(Optional::isPresent)
                .map(literal -> TypeConverter.extractOWLLiteral(datatype, literal.get()))
                .collect(Collectors.toList());
        return Optional.of(factValues);
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, String individual) {
        return getIndividualEvents(clazz, df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individual)));
    }

    @Override
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual) {

        final Class<? extends Temporal> temporalType = TemporalParser.getTemporalType(clazz);

        logger.debug("Retrieving events for {}", individual);
        //        Build the query string
        final String eventQuery = this.qb.buildIndividualEventQuery(individual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final TrestleResultSet resultSet;
        try {
            resultSet = this.ontology.executeSPARQLResults(eventQuery);
        } catch (Exception e) {
            logger.error("Unable to get events for individual: {}", individual);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
//        Parse out the events
//        I think I can suppress this, because if the above method throws an error, the catch statement will return an empty optional
        @SuppressWarnings({"dereference.of.nullable"}) final List<TrestleResult> results = resultSet.getResults();
        final Set<TrestleEvent> individualEvents = results
                .stream()
//                Filter out Trestle_Event from results
                .filter(result -> !result
                        .unwrapIndividual("type")
                        .asOWLNamedIndividual()
                        .getIRI().equals(trestleEventIRI))
                .map(result -> {
                    final OWLNamedIndividual eventIndividual = result.unwrapIndividual("r").asOWLNamedIndividual();
                    final IRI eventTypeIRI = result.unwrapIndividual("type").asOWLNamedIndividual().getIRI();
                    final TrestleEventType eventType = TrestleEventType.getEventClassFromIRI(eventTypeIRI);
                    final Temporal temporal = parseToTemporal(result.unwrapLiteral("t"), temporalType);
                    return new TrestleEvent(eventType, individual, eventIndividual, temporal);
                })
                .collect(Collectors.toSet());

        return Optional.of(individualEvents);
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
        if (!(type == TrestleEventType.SPLIT) && !(type == TrestleEventType.MERGED)) {
            throw new IllegalArgumentException("Only MERGED and SPLIT types are valid");
        }

        final OWLNamedIndividual subjectIndividual = this.trestleParser.classParser.getIndividual(subject);
//        Build the event name
        final OWLNamedIndividual eventIndividual = TrestleEventEngine.buildEventName(df, this.REASONER_PREFIX, subjectIndividual, type);
//        Get the event temporal to use, and just grab the first one
        final Optional<List<TemporalObject>> temporalObjects = this.trestleParser.temporalParser.getTemporalObjects(subject);
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
                .map(this.trestleParser.classParser::getIndividual)
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

    @Override
    @SuppressWarnings("return.type.incompatible")
    public <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer) {
        return spatialIntersectObject(inputObject, buffer, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <@NonNull T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer, @Nullable Temporal temporalAt) {
        final OWLNamedIndividual owlNamedIndividual = trestleParser.classParser.getIndividual(inputObject);
        final Optional<String> wktString = SpatialParser.getSpatialValueAsString(inputObject);

        if (wktString.isPresent()) {
            return spatialIntersect((Class<@NonNull T>) inputObject.getClass(), wktString.get(), buffer, temporalAt);
        }

        logger.info("{} doesn't have a spatial component", owlNamedIndividual);
        return Optional.empty();
    }

    @Override
    @SuppressWarnings({"override.return.invalid"})
    public <@NonNull T> Optional<List<@NonNull T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer) {
        return spatialIntersect(clazz, wkt, buffer, null);
    }

    @Override
    @Timed(name = "spatial-intersect-timer")
    @Metered(name = "spatial-intersect-meter")
    @SuppressWarnings({"override.return.invalid"})
    public <@NonNull T> Optional<List<@NonNull T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer, @Nullable Temporal validAt) {
        final OWLClass owlClass = trestleParser.classParser.getObjectClass(clazz);

        final OffsetDateTime atTemporal;
        final OffsetDateTime dbTemporal;

        if (validAt == null) {
            atTemporal = OffsetDateTime.now();
        } else {
            atTemporal = parseTemporalToOntologyDateTime(validAt, ZoneOffset.UTC);
        }

//            TODO(nrobison): Implement DB intersection
        dbTemporal = OffsetDateTime.now();

        String spatialIntersection;
        try {
            logger.debug("Running spatial intersection at time {}", atTemporal);
            spatialIntersection = qb.buildTemporalSpatialIntersection(owlClass, wkt, buffer, QueryBuilder.Units.METER, atTemporal, dbTemporal);
//                }
        } catch (UnsupportedFeatureException e) {
            logger.error("Database {} doesn't support spatial intersections.", spatialDalect, e);
            return Optional.empty();
        }
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final String finalSpatialIntersection = spatialIntersection;
            final CompletableFuture<List<@NonNull T>> objectsFuture = CompletableFuture.supplyAsync(() -> {
                logger.debug("Executing async spatial query");
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                logger.debug("Transaction opened");
                try {
                    return this.ontology.executeSPARQLResults(finalSpatialIntersection);
                } finally {
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, trestleThreadPool)
                    .thenApply(resultSet -> resultSet.getResults()
                            .stream()
                            .map(result -> IRI.create(result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()))
                            .collect(Collectors.toSet()))
                    .thenApply(intersectedIRIs -> intersectedIRIs
                            .stream()
                            .map(iri -> CompletableFuture.supplyAsync(() -> {
                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                                try {
                                    return readTrestleObject(clazz, iri, false, atTemporal, dbTemporal);
                                } finally {
                                    this.ontology.returnAndCommitTransaction(tt);
                                }
                            }, trestleThreadPool))
                            .collect(Collectors.toList()))
                    .thenCompose(LambdaUtils::sequenceCompletableFutures);

            try {
                final List<@NonNull T> intersectedObjects = objectsFuture.get();
                return Optional.of(intersectedObjects);
            } catch (InterruptedException e) {
                logger.error("Spatial intersection interrupted", e);
                this.ontology.returnAndAbortTransaction(trestleTransaction);
                return Optional.empty();
            } catch (ExecutionException e) {
                logger.error("Spatial intersection execution exception", e);
                this.ontology.returnAndAbortTransaction(trestleTransaction);
                return Optional.empty();
            }
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @Override
    @SuppressWarnings("return.type.incompatible")
    @Deprecated
    public <T> Optional<Map<@NonNull T, Double>> getRelatedObjects(Class<@NonNull T> clazz, String objectID, double cutoff) {
//
//
//        final OWLClass owlClass = trestleParser.classParser.getObjectClass(clazz);
//
//        final String relationQuery = qb.buildRelationQuery(df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX, objectID)), owlClass, cutoff);
//        TrestleTransaction transaction = ontology.createandOpenNewTransaction(false);
//        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(relationQuery);
//
//        Set<IRI> relatedIRIs = new HashSet<>();
//        Map<@NonNull T, Double> relatedObjects = new HashMap<>();
//        Map<IRI, Double> relatedObjectResults = new HashMap<>();
//        resultSet.getResults()
//                .forEach(result -> relatedObjectResults.put(IRI.create(result.getIndividual("f").orElseThrow(() -> new RuntimeException("fact is null")).toStringID()), result.getLiteral("s").orElseThrow(() -> new RuntimeException("strength is null")).parseDouble()));
//
//        relatedObjectResults
//                .entrySet().forEach(entry -> {
//            final @NonNull T object = readTrestleObject(clazz, entry.getKey(), false);
//            relatedObjects.put(object, entry.getValue());
//        });
//        ontology.returnAndCommitTransaction(transaction);
//
//        if (relatedObjects.size() == 0) {
//            return Optional.empty();
//        }
//
//        return Optional.of(relatedObjects);
        logger.warn("Deprecated");
        return Optional.empty();
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
    public <T> void removeIndividual(@NonNull T... inputObject) {
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
            logger.error("Delete interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Execution error", e);
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
    public <@NonNull T> Optional<List<T>> getEquivalentObjects(Class<T> clazz, IRI individual, Temporal queryTemporal) {
        return getEquivalentObjects(clazz, Collections.singletonList(individual), queryTemporal);
    }

    @Override
    public <@NonNull T> Optional<List<T>> getEquivalentObjects(Class<T> clazz, List<IRI> individuals, Temporal queryTemporal) {
        final List<OWLNamedIndividual> individualSubjects = individuals
                .stream()
                .map(df::getOWLNamedIndividual)
                .collect(Collectors.toList());
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final List<OWLNamedIndividual> equivalentIndividuals = this.equalityEngine.getEquivalentIndividuals(clazz, individualSubjects, queryTemporal);
            final List<CompletableFuture<T>> individualsFutureList = equivalentIndividuals
                    .stream()
                    .map(individual -> CompletableFuture.supplyAsync(() -> {
                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                        try {
                            return this.readTrestleObject(clazz, individual.getIRI(), false, queryTemporal, null);
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
        @Nullable OWLClass owlClass = null;
        if (datasetClass != null) {
            owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, datasetClass));
        }
        final String query = qb.buildIndividualSearchQuery(individualIRI, owlClass, limit);
        final TrestleResultSet resultSet = ontology.executeSPARQLResults(query);
        List<String> individuals = resultSet.getResults()
                .stream()
                .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                .collect(Collectors.toList());
        return individuals;
    }

    @Override
    public TrestleIndividual getTrestleIndividual(String individualIRI) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individualIRI)));
    }

    /**
     * Return a TrestleIndividual with all available facts and relations
     *
     * @param individual - OWLNamedIndividual to retrieve facts for
     * @return - TrestleIndividual
     */
    @Timed
    private TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {

        logger.debug("Building trestle individual {}", individual);
        @Nullable final TrestleIndividual cacheIndividual = this.trestleCache.getTrestleIndividual(individual);
        if (cacheIndividual != null) {
            logger.debug("Retrieved {} from cache");
            return cacheIndividual;
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        final CompletableFuture<TrestleIndividual> temporalFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            final Set<OWLDataPropertyAssertionAxiom> individualDataProperties = ontology.getAllDataPropertiesForIndividual(individual);
            this.ontology.returnAndCommitTransaction(tt);
            return new TemporalPropertiesPair(individual, individualDataProperties);
        })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()))
                .thenApply(temporalObject -> new TrestleIndividual(individual.toStringID(), temporalObject.orElseThrow(() -> new CompletionException(new TrestleMissingIndividualException(individual)))));

//                Get all the facts
        final Optional<List<OWLObjectPropertyAssertionAxiom>> individualFacts = ontology.getIndividualObjectProperty(individual, hasFactIRI);
        final List<CompletableFuture<TrestleFact>> factFutureList = individualFacts.orElse(new ArrayList<>())
                .stream()
                .map(fact -> buildTrestleFact(fact.getObject().asOWLNamedIndividual(), trestleTransaction))
                .collect(Collectors.toList());

        CompletableFuture<List<TrestleFact>> factsFuture = sequenceCompletableFutures(factFutureList);

//                Get the relations
        final CompletableFuture<List<TrestleRelation>> relationsFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            String query = this.qb.buildIndividualRelationQuery(individual);
            try {
                return ontology.executeSPARQLResults(query);
            } catch (Exception e) {
                this.ontology.returnAndAbortTransaction(tt);
                throw new CompletionException(e.getCause());
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, trestleThreadPool)
                .thenApply(sparqlResults -> {
                    List<TrestleRelation> relations = new ArrayList<>();
                    sparqlResults.getResults()
                            .stream()
//                            We want the subProperties of Temporal/Spatial/Event relations. So we filter them out
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(temporalRelationIRI))
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(spatialRelationIRI))
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(eventRelationIRI))
//                            Filter out self
                            .filter(result -> !result.unwrapIndividual("p").asOWLNamedIndividual().equals(individual))
                            .forEach(result -> relations.add(new TrestleRelation(result.unwrapIndividual("m").toStringID(),
                                    ObjectRelation.getRelationFromIRI(IRI.create(result.unwrapIndividual("o").toStringID())),
                                    result.unwrapIndividual("p").toStringID())));
                    return relations;
                });

//        Get the events
        final CompletableFuture<List<TrestleEvent>> eventsFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            final String query = this.qb.buildIndividualEventQuery(individual);
            try {
                return this.ontology.executeSPARQLResults(query);
            } catch (Exception e) {
                this.ontology.returnAndAbortTransaction(tt);
                throw new CompletionException(e.getCause());
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, this.trestleThreadPool)
                .thenApply(results -> {
                    List<TrestleEvent> events = new ArrayList<>();
                    return results.getResults()
                            .stream()
                            .filter(result -> !result.unwrapIndividual("type").asOWLNamedIndividual().getIRI().equals(trestleEventIRI))
                            .map(result -> {
                                final OWLNamedIndividual eventIndividual = result.unwrapIndividual("r").asOWLNamedIndividual();
                                final IRI typeIRI = result.unwrapIndividual("type").asOWLNamedIndividual().getIRI();
                                final TrestleEventType eventType = TrestleEventType.getEventClassFromIRI(typeIRI);
                                final Temporal temporal = parseToTemporal(result.unwrapLiteral("t"), OffsetDateTime.class);
                                return new TrestleEvent(eventType, individual, eventIndividual, temporal);
                            })
                            .collect(Collectors.toList());
                });

        final CompletableFuture<TrestleIndividual> individualFuture = temporalFuture.thenCombine(relationsFuture, (trestleIndividual, relations) -> {
            relations.forEach(trestleIndividual::addRelation);
            return trestleIndividual;
        })
                .thenCombine(factsFuture, (trestleIndividual, trestleFacts) -> {
                    trestleFacts.forEach(trestleIndividual::addFact);
                    return trestleIndividual;
                })
                .thenCombine(eventsFuture, (trestleIndividual, events) -> {
                    events.forEach(trestleIndividual::addEvent);
                    return trestleIndividual;
                });

        try {
            TrestleIndividual trestleIndividual = individualFuture.get();
            try {
                this.trestleCache.writeTrestleIndividual(individual, trestleIndividual);
            } catch (Exception e) {
                logger.error("Unable to write Trestle Individual {} to cache", individual, e);
            }
            return trestleIndividual;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Interruption exception building Trestle Individual {}", individual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            if (e.getCause().getClass().isAssignableFrom(TrestleMissingIndividualException.class)) {
                throw TrestleMissingIndividualException.class.cast(e.getCause());
            }
            throw new RuntimeException(e);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
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
                    final TrestleTransaction futureTransaction = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    final Set<OWLDataPropertyAssertionAxiom> temporalsForIndividual = this.ontology.getTemporalsForIndividual(df.getOWLNamedIndividual(IRI.create(iri)));
                    final Optional<TemporalObject> individualExistsTemporal = TemporalObjectBuilder.buildTemporalFromProperties(temporalsForIndividual, null, "blank");
                    final TemporalObject temporalObject = individualExistsTemporal.orElseThrow(() -> new RuntimeException(String.format("Unable to get exists temporals for %s", iri)));
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
                    try {
                        final @NonNull T retrievedObject = this.readTrestleObject(clazz, iri, adjustedIntersection, null);
                        this.ontology.returnAndCommitTransaction(futureTransaction);
                        return retrievedObject;
                    } catch (TrestleClassException e) {
                        logger.error("Unregistered class", e);
                        this.ontology.returnAndCommitTransaction(futureTransaction);
                        throw new RuntimeException(e);
                    } catch (MissingOntologyEntity e) {
                        logger.error("Cannot find ontology individual {}", e.getIndividual(), e);
                        this.ontology.returnAndCommitTransaction(futureTransaction);
                        throw new RuntimeException(e);
                    }
                }, trestleThreadPool))
                .collect(Collectors.toList());
        final CompletableFuture<List<T>> conceptObjectsFuture = sequenceCompletableFutures(completableFutureList);
        try {
            List<T> objects = conceptObjectsFuture.get();
            return Optional.of(objects);
        } catch (InterruptedException e) {
            logger.error("Object retrieval for concept {}, interrupted", conceptID, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Unable to retrieve all objects for concept {}", conceptID, e);
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
            final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromProperties(validTemporals, null, "blank");
//            Database time
            final Set<OWLDataPropertyAssertionAxiom> dbTemporals = dataProperties
                    .stream()
                    .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                            property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI))
                    .collect(Collectors.toSet());
            final Optional<TemporalObject> dbTemporal = TemporalObjectBuilder.buildTemporalFromProperties(dbTemporals, null, "blank");
            return new TrestleFact<>(
                    factIndividual.getIRI().toString(),
                    assertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                    literalObject,
                    validTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)),
                    dbTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)));
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

    @Override
    public void registerClass(Class inputClass) throws TrestleClassException {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.put(trestleParser.classParser.getObjectClass(inputClass), inputClass);
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

    private void writeTemporal(TemporalObject temporal, OWLNamedIndividual individual) throws MissingOntologyEntity {
//        Write the object
//        final IRI temporalIRI = IRI.create(REASONER_PREFIX, temporal.getID());
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

    @Override
    public Set<String> getAvailableDatasets() {

        final String datasetQuery = qb.buildDatasetQuery();
        final TrestleResultSet resultSet = ontology.executeSPARQLResults(datasetQuery);
        List<OWLClass> datasetsInOntology = resultSet
                .getResults()
                .stream()
                .map(result -> df.getOWLClass(result.getIndividual("dataset").orElseThrow(() -> new RuntimeException("dataset is null")).toStringID()))
                .collect(Collectors.toList());

        return this.registeredClasses
                .keySet()
                .stream()
                .filter(datasetsInOntology::contains)
                .map(individual -> individual.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    @Override
    public Class<?> getDatasetClass(String owlClassString) throws UnregisteredClassException {
        final OWLClass owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, owlClassString));
        final Class<?> aClass = this.registeredClasses.get(owlClass);
        if (aClass == null) {
            throw new UnregisteredClassException(owlClass);
        }
        return aClass;
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
        final Optional<List<OWLDataProperty>> propertyMembers = ClassBuilder.getPropertyMembers(inputClass, true);
        propertyMembers.ifPresent(owlDataProperties -> owlDataProperties.forEach(property -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, property.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, property))));

//        Now the temporals
        final Optional<List<OWLDataProperty>> temporalProperties = trestleParser.temporalParser.getTemporalsAsDataProperties(inputClass);
        temporalProperties.ifPresent(owlDataProperties -> owlDataProperties.forEach(temporal -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, temporal.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, temporal))));


        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final List<CompletableFuture<Optional<TSIndividual>>> completableFutures = objectID
                .stream()
                .map(id -> IRIUtils.parseStringToIRI(REASONER_PREFIX, id))
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final @NonNull T object = readTrestleObject(inputClass, id, false, validAt, databaseAt);
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

        final ShapefileExporter shapeFileExporter = new ShapefileExporter.Builder<>(shapefileSchema.getGeomName(), shapefileSchema.getGeomType(), shapefileSchema).build();
        try {
            final List<TSIndividual> individuals = sequencedFutures
                    .get()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return shapeFileExporter.writePropertiesToByteBuffer(individuals, null);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error constructing object", e);
            throw new RuntimeException("Problem constructing object");
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T> Optional<TSIndividual> parseIndividualToShapefile(Optional<@NonNull T> objectOptional, ShapefileSchema shapefileSchema) {
        if (!objectOptional.isPresent()) {
            return Optional.empty();
        }
        final @NonNull T object = objectOptional.get();
//        if (objectOptional.isPresent()) {
//            final T object = objectOptional.get();
        final Class<?> inputClass = object.getClass();
        final Optional<OWLDataPropertyAssertionAxiom> spatialProperty = trestleParser.classParser.GetSpatialFact(object);
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
            individual.addProperty(ClassParser.matchWithClassMember(inputClass, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                    literal);
        }));
//                    Temporals
        final Optional<List<TemporalObject>> temporalObjects = trestleParser.temporalParser.getTemporalObjects(object);
        if (temporalObjects.isPresent()) {
            final TemporalObject temporalObject = temporalObjects.get().get(0);
            if (temporalObject.isInterval()) {
                final IntervalTemporal intervalTemporal = temporalObject.asInterval();
                final String startName = intervalTemporal.getStartName();
                individual.addProperty(ClassParser.matchWithClassMember(inputClass, intervalTemporal.getStartName()), intervalTemporal.getFromTime().toString());
                final Optional toTime = intervalTemporal.getToTime();
                if (toTime.isPresent()) {
                    final Temporal to = (Temporal) toTime.get();
                    individual.addProperty(ClassParser.matchWithClassMember(inputClass, intervalTemporal.getEndName()), to.toString());
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
     * Read object existence {@link TemporalObject} for the given {@link OWLNamedIndividual}, unless we bypass it, then just return an empty optional
     *
     * @param individual  - {@link OWLNamedIndividual} to read
     * @param transaction - Execute as part of provided {@link TrestleTransaction}
     * @param executor    - Execute within provided {@link ExecutorService}
     * @param bypass      - {@code true} bypass execution and return {@link Optional#empty()}
     * @return - {@link Optional} of {@link TemporalObject} provided existence interval of the individual
     */
    private CompletableFuture<Optional<TemporalObject>> readObjectExistence(OWLNamedIndividual individual, TrestleTransaction transaction, ExecutorService executor, boolean bypass) {
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
