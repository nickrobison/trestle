package com.nickrobison.trestle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.nickrobison.trestle.caching.TrestleCache;
import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.exceptions.*;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.exporter.ShapefileExporter;
import com.nickrobison.trestle.exporter.ShapefileSchema;
import com.nickrobison.trestle.exporter.TSIndividual;
import com.nickrobison.trestle.ontology.*;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.parser.*;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.*;
import com.nickrobison.trestle.types.relations.ConceptRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import com.nickrobison.trestle.utils.FactPair;
import com.nickrobison.trestle.utils.TemporalPair;
import com.nickrobison.trestle.utils.TemporalPropertiesPair;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;
import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaExceptionUtil.rethrowFunction;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.parser.TemporalParser.parseTemporalToOntologyDateTime;
import static com.nickrobison.trestle.utils.ConfigValidator.ValidateConfig;

/**
 * Created by nrobison on 5/17/16.
 */
@SuppressWarnings({"methodref.inference.unimplemented"})
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final String DEFAULTNAME = "trestle";

    private final String REASONER_PREFIX;
    private final ITrestleOntology ontology;
    private final Map<OWLClass, Class<?>> registeredClasses = new HashMap<>();
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(datasetClassIRI);
    private final QueryBuilder qb;
    private final QueryBuilder.DIALECT spatialDalect;
    private boolean cachingEnabled = true;
    private @Nullable TrestleCache trestleCache = null;
    private final TrestleParser trestleParser;
    private final Config trestleConfig;

    @SuppressWarnings("dereference.of.nullable")
    TrestleReasoner(TrestleBuilder builder) throws OWLOntologyCreationException {

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
                throw new RuntimeException("Unable to parse IRI to URI", e);
            } catch (FileNotFoundException e) {
                logger.error("Cannot find ontology file {}", builder.ontologyIRI.get(), e);
                throw new RuntimeException("File not found", e);
            }
        } else {
//            Load with the class loader
            ontologyResource = TrestleReasoner.class.getClassLoader().getResource("trestle.owl");
            ontologyIS = TrestleReasoner.class.getClassLoader().getResourceAsStream("trestle.owl");
        }

        if (ontologyIS == null) {
            logger.error("Cannot load trestle ontology from resources");
            throw new RuntimeException("Cannot load ontology");
        }
        logger.info("Loading ontology from {}", ontologyResource);

//        Validate ontology name
        try {
            validateOntologyName(builder.ontologyName.orElse(DEFAULTNAME));
        } catch (InvalidOntologyName e) {
            logger.error("{} is an invalid ontology name", builder.ontologyName.orElse(DEFAULTNAME), e);
            throw new RuntimeException("invalid ontology name", e);
        }

//        Setup the Parser
        trestleParser = new TrestleParser(df, REASONER_PREFIX, trestleConfig.getBoolean("enableMultiLanguage"), trestleConfig.getString("defaultLanguage"));
        logger.info("Ontology {} ready", builder.ontologyName.orElse(DEFAULTNAME));

//            validate the classes
        builder.inputClasses.forEach(clazz -> {
            try {
                ClassRegister.ValidateClass(clazz);
                this.registeredClasses.put(trestleParser.classParser.GetObjectClass(clazz), clazz);
            } catch (TrestleClassException e) {
                logger.error("Cannot validate class {}", clazz, e);
            }
        });

//        Register some constructor functions
        TypeConverter.registerTypeConstructor(UUID.class, df.getOWLDatatype(UUIDDatatypeIRI), (uuidString) -> UUID.fromString(uuidString.toString()));

        logger.info("Connecting to ontology {} at {}", builder.ontologyName.orElse(DEFAULTNAME), builder.connectionString.orElse("localhost"));
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

        ontology = ontologyBuilder.build();
        logger.debug("Ontology connected");
        if (builder.initialize) {
            logger.info("Initializing ontology");
            this.ontology.initializeOntology();
        } else {
//            If we're not starting fresh, then we might need to update the indexes and inferencer
            logger.info("Updating inference model");
            ontology.runInference();
        }

//        Are we a caching reasoner?
        if (!builder.caching) {
            this.cachingEnabled = false;
        } else {
            logger.info("Building trestle caches");
            trestleCache = builder.sharedCache.orElse(new TrestleCache.TrestleCacheBuilder().build());
        }

//        Setup the query builder
        if (ontology instanceof OracleOntology) {
            spatialDalect = QueryBuilder.DIALECT.ORACLE;
        } else if (ontology instanceof VirtuosoOntology) {
            spatialDalect = QueryBuilder.DIALECT.VIRTUOSO;
        } else if (ontology instanceof LocalOntology) {
            spatialDalect = QueryBuilder.DIALECT.JENA;
//        } else if (ontology instanceof StardogOntology) {
//            spatialDalect = QueryBuilder.DIALECT.STARDOG;
        } else {
            spatialDalect = QueryBuilder.DIALECT.SESAME;
        }
        logger.debug("Using SPARQL dialect {}", spatialDalect);
        qb = new QueryBuilder(spatialDalect, ontology.getUnderlyingPrefixManager());
    }

    /**
     * Shutdown the ontology and potentially delete
     *
     * @param delete - delete the ontology on shutdown?
     */
    public void shutdown(boolean delete) {
        logger.info("Shutting down ontology");
        this.ontology.close(delete);
    }

    /**
     * Register custom constructor function for a given java class/OWLDataType intersection
     *
     * @param clazz           - Java class to construct
     * @param datatype        - OWLDatatype to match with Java class
     * @param constructorFunc - Function lambda function to take OWLLiteral and generate given java class
     */
    public void registerTypeConstructor(Class<?> clazz, OWLDatatype datatype, Function constructorFunc) {
        TypeConverter.registerTypeConstructor(clazz, datatype, constructorFunc);
    }

    //    When you get the ontology, the ownership passes away, so then the reasoner can't perform any more queries.
    public ITrestleOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public Set<OWLNamedIndividual> getInstances(Class inputClass) {
        final OWLClass owlClass = trestleParser.classParser.GetObjectClass(inputClass);
        return this.ontology.getInstances(owlClass, true);
    }

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

//    /**
//     * Write a Java object as a Trestle_Concept
//     *
//     * @param inputObject - Input object to write as concept
//     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
//     */
////    FIXME(nrobison): This actually doesn't work, we've split the concepts and objects up into their own classes.
//    public void writeObjectAsConcept(Object inputObject, ConceptRelationType type) throws TrestleClassException, MissingOntologyEntity {
//
//        CompletableFuture.supplyAsync(() -> writeObj)
//
//        writeObject(inputObject, TemporalScope.EXISTS, null);
//    }


    /**
     * Write a Java object as a Trestle_Object
     *
     * @param inputObject - Input object to write as fact
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     */
    public void writeAsTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        writeObject(inputObject, TemporalScope.EXISTS, null);
    }

    /**
     * Write a Java object as a Trestle_Object
     * Use the provided temporals to setup the database time
     *
     * @param inputObject   - Object to write into the ontology
     * @param startTemporal - Start of database time interval
     * @param endTemporal   - @Nullable Temporal of ending interval time
     */
    @SuppressWarnings("unchecked")
    public void writeAsTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).withRelations(trestleParser.classParser.GetIndividual(inputObject));
        } else {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).to(endTemporal).withRelations(trestleParser.classParser.GetIndividual(inputObject));
        }
        writeObject(inputObject, TemporalScope.EXISTS, databaseTemporal);
    }

    /**
     * Writes a data property as an asserted fact for an individual TS_Object.
     *
     * @param rootIndividual   - OWLNamedIndividual of the TS_Object individual
     * @param properties       - List of OWLDataPropertyAssertionAxioms to write as Facts
     * @param temporal         - Temporal to associate with data property individual
     * @param databaseTemporal - Temporal representing database time
     */
    private void writeObjectFact(OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject temporal, TemporalObject databaseTemporal) {
        final long now = Instant.now().getEpochSecond();
        final OWLClass factClass = df.getOWLClass(factClassIRI);
        properties.forEach(property -> {

//            TODO(nrobison): We should change this to lookup any existing records to correctly increment the record number
//            TODO(nrobison): Should this prefix be the reasoner prefix? Probably, but not sure.
            final OWLNamedIndividual propertyIndividual = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, String.format("%s:%s:%d", rootIndividual.getIRI().getShortForm(), property.getProperty().asOWLDataProperty().getIRI().getShortForm(), now)));
            ontology.createIndividual(propertyIndividual, factClass);
            try {
//                Write the property
                ontology.writeIndividualDataProperty(propertyIndividual, property.getProperty().asOWLDataProperty(), property.getObject());
//                Write the temporal relation
                ontology.writeIndividualObjectProperty(propertyIndividual, validTimeIRI, df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX, temporal.getID())));
//                Write the relation back to the root individual
                ontology.writeIndividualObjectProperty(propertyIndividual, factOfIRI, rootIndividual);
//                Write the database time
                ontology.writeIndividualObjectProperty(propertyIndividual, databaseTimeIRI, df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX, databaseTemporal.getID())));
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            }
        });
    }

    /**
     * Writes an object into the ontology using the given temporal scope
     * If a temporal is provided it uses that for the database time interval
     *
     * @param inputObject      - Object to write to the ontology
     * @param scope            - TemporalScope to determine if it's a fact or concept
     * @param databaseTemporal - Optional TemporalObject to manually set database time
     */
    void writeObject(Object inputObject, TemporalScope scope, @Nullable TemporalObject databaseTemporal) throws UnregisteredClassException, MissingOntologyEntity {
        final Class aClass = inputObject.getClass();
        checkRegisteredClass(aClass);

        final OWLNamedIndividual owlNamedIndividual = trestleParser.classParser.GetIndividual(inputObject);

//            Create the database time object
        final TemporalObject dTemporal;
        if (databaseTemporal == null) {
            dTemporal = TemporalObjectBuilder.valid().from(OffsetDateTime.now()).withRelations(owlNamedIndividual);
        } else {
            dTemporal = databaseTemporal;
        }

//        Write the class
        final OWLClass owlClass = trestleParser.classParser.GetObjectClass(inputObject);
        final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(true);
        ontology.associateOWLClass(owlClass, datasetClass);
//        Write the individual
        ontology.createIndividual(owlNamedIndividual, owlClass);
//        Write the temporal
        final Optional<List<TemporalObject>> temporalObjects = trestleParser.temporalParser.GetTemporalObjects(inputObject);
        TemporalObject objectTemporal = temporalObjects.orElseThrow(() -> new RuntimeException(String.format("Cannot parse temporals for %s", owlNamedIndividual))).get(0);
//        temporalObjects.ifPresent(temporalObject -> temporalObject.forEach(temporal -> {
//            try {
//                writeTemporal(temporal, owlNamedIndividual, scope, existsTimeIRI);
//            } catch (MissingOntologyEntity e) {
//                logger.error("Individual {} missing in ontology", e.getIndividual(), e);
//            }
//        }));

//            Write the database temporal
//        An object doesn't have a database temporal, just an exists temporal
//        TODO(nrobison): This shouldn't write an association to the individual itself, just to the facts.
        writeTemporal(dTemporal, null, null, null);
//        Write the individual temporal
        writeTemporal(objectTemporal, owlNamedIndividual, TemporalScope.EXISTS, existsTimeIRI);
//        Create a facts temporal and write it, with no association
        TemporalObject factTemporal = objectTemporal.castTo(TemporalScope.VALID);
        writeTemporal(factTemporal, null, null, null);


//        Write the data properties
        final Optional<List<OWLDataPropertyAssertionAxiom>> dataProperties = trestleParser.classParser.GetDataProperties(inputObject);
        dataProperties.ifPresent(owlDataPropertyAssertionAxioms -> writeObjectFact(owlNamedIndividual, owlDataPropertyAssertionAxioms, factTemporal, dTemporal));

//        Write the object properties
        ontology.returnAndCommitTransaction(trestleTransaction);
    }

    private void checkRegisteredClass(Class<?> clazz) throws UnregisteredClassException {
        if (!this.registeredClasses.containsValue(clazz)) {
            throw new UnregisteredClassException(clazz);
        }
    }

    private boolean checkExists(IRI individualIRI) {
        return ontology.containsResource(individualIRI);
    }

    public <T> @NonNull T readAsObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException {
        return readAsObject(datasetClassID, objectID, null, null);
    }

    public <T> @NonNull T readAsObject(String datasetClassID, String objectID, @Nullable Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, TrestleClassException {
//        Lookup class
        final OWLClass datasetClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, datasetClassID));
        final Optional<OWLClass> matchingClass = this.registeredClasses
                .keySet()
                .stream()
                .filter(owlclass -> owlclass.equals(datasetClass))
                .findFirst();

        if (!matchingClass.isPresent()) {
            throw new MissingOntologyEntity("Cannot find matching class for: ", datasetClass);
        }

        final Class<T> aClass = (Class<T>) this.registeredClasses.get(matchingClass.get());
        return readAsObject(aClass, objectID, startTemporal, endTemporal);
    }

    public <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull String objectID) throws TrestleClassException, MissingOntologyEntity {
        return readAsObject(clazz, objectID, null, null);
    }


    @SuppressWarnings({"argument.type.incompatible", "dereference.of.nullable"})
    public <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull String objectID, @Nullable Temporal startTemporal, @Nullable Temporal endTemporal) throws TrestleClassException, MissingOntologyEntity {

        final IRI individualIRI = parseStringToIRI(REASONER_PREFIX, objectID);
//        Check cache first
        if (cachingEnabled) {
            logger.debug("Retrieving {} from cache", individualIRI);
            return clazz.cast(trestleCache.ObjectCache().get(individualIRI, rethrowFunction(iri -> readAsObject(clazz, individualIRI, true))));
        } else {
            logger.debug("Bypassing cache and directly retrieving object");
            return readAsObject(clazz, individualIRI, false, startTemporal, endTemporal);
        }
    }

    <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, boolean bypassCache) {
        return readAsObject(clazz, individualIRI, bypassCache, null, null);
    }

    @SuppressWarnings({"return.type.incompatible", "argument.type.incompatible", "unchecked"})
    <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, boolean bypassCache, @Nullable Temporal startTemporal, @Nullable Temporal endTemporal) {
        logger.debug("Reading {}", individualIRI);
//        Check for cache hit first, provided caching is enabled and we're not set to bypass the cache
        if (isCachingEnabled() & !bypassCache) {
            logger.debug("Retrieving {} from cache", individualIRI);
            return clazz.cast(trestleCache.ObjectCache().get(individualIRI, rethrowFunction(iri -> readAsObject(clazz, individualIRI, true))));
        } else {
            logger.debug("Bypassing cache and directly retrieving object");
        }

        TemporalObject databaseTemporal = null;
        if (startTemporal != null && endTemporal != null) {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).to(startTemporal).withRelations(df.getOWLNamedIndividual(individualIRI));
        } else if (startTemporal != null) {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).withRelations(df.getOWLNamedIndividual(individualIRI));
        }
        final Optional<@NonNull T> constructedObject = readAsObject(clazz, individualIRI, databaseTemporal);
        if (constructedObject.isPresent()) {
            logger.debug("Done with {}", individualIRI);
            return constructedObject.get();
        } else {
            throw new RuntimeException("Problem constructing object");
        }
    }

    private <T> Optional<T> readAsObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, @Nullable TemporalObject databaseTemporal) {

//        Contains class?
        try {
            checkRegisteredClass(clazz);
        } catch (UnregisteredClassException e) {
            logger.error("Unregistered class", e);
            throw new CompletionException(e);
        }

//        Do some things before opening a transaction
        final Optional<List<OWLDataProperty>> dataProperties = ClassBuilder.getPropertyMembers(clazz);
//        Setup the database time temporal
        @Nullable OffsetDateTime startTemporal = null;
        @Nullable OffsetDateTime endTemporal = null;
        if (databaseTemporal != null) {
            if (databaseTemporal.asInterval().isContinuing()) {
                startTemporal = parseTemporalToOntologyDateTime(databaseTemporal.asInterval().getFromTime(), TemporalParser.IntervalType.START, ZoneOffset.UTC);
            } else {
                startTemporal = parseTemporalToOntologyDateTime(databaseTemporal.asInterval().getFromTime(), TemporalParser.IntervalType.START, ZoneOffset.UTC);
                endTemporal = parseTemporalToOntologyDateTime((Temporal) databaseTemporal.asInterval().getToTime().get(), TemporalParser.IntervalType.END, ZoneOffset.UTC);
            }
        }

//            Get the temporal objects to figure out the correct return type
        final Class<? extends Temporal> baseTemporalType = TemporalParser.GetTemporalType(clazz);

        final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(false);

//        Figure out its name
        if (!checkExists(individualIRI)) {
            logger.error("Missing individual {}", individualIRI);
            return Optional.empty();
        }

        if (dataProperties.isPresent()) {

            @Nullable OffsetDateTime finalStartTemporal = startTemporal;
            @Nullable OffsetDateTime finalEndTemporal = endTemporal;
            final CompletableFuture<Set<OWLDataPropertyAssertionAxiom>> factsFuture = CompletableFuture.supplyAsync(() -> {
                final Instant individualRetrievalStart = Instant.now();
                final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
                final Set<OWLDataPropertyAssertionAxiom> objectFacts = ontology.GetFactsForIndividual(df.getOWLNamedIndividual(individualIRI), finalStartTemporal, finalEndTemporal);
                logger.debug("Retrieved {} facts for {}", objectFacts.size(), individualIRI);
                ontology.returnAndCommitTransaction(tt);
                final Instant individualRetrievalEnd = Instant.now();
                logger.debug("Retrieving {} facts took {} ms", objectFacts.size(), Duration.between(individualRetrievalStart, individualRetrievalEnd).toMillis());
                return objectFacts;
            });

//            Get the temporals
            final CompletableFuture<Optional<TemporalObject>> temporalFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction);
                final Set<OWLDataPropertyAssertionAxiom> properties = ontology.GetTemporalsForIndividual(df.getOWLNamedIndividual(individualIRI));
                ontology.returnAndCommitTransaction(tt);
                return properties;
            })
                    .thenApply(temporalProperties -> TemporalObjectBuilder.buildTemporalFromProperties(temporalProperties, baseTemporalType, clazz));


            final CompletableFuture<ConstructorArguments> argumentsFuture = factsFuture.thenCombine(temporalFuture, (facts, temporals) -> {
                logger.debug("In the arguments future");
                final ConstructorArguments constructorArguments = new ConstructorArguments();
                facts.forEach(property -> {
                    @Nullable String languageTag = null;
                    if (property.getObject().hasLang()) {
                        languageTag = property.getObject().getLang();
                    }
                    final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, clazz);
                    final Object literalValue = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm(), languageTag),
                            javaClass,
                            literalValue);
                });
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
                    if (!intervalTemporal.isDefault() & intervalTemporal.getToTime().isPresent()) {
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
                return constructorArguments;
            });

            final ConstructorArguments constructorArguments;
            try {
                constructorArguments = argumentsFuture.get();
                ontology.returnAndCommitTransaction(trestleTransaction);
            } catch (InterruptedException e) {
                logger.error("Read object {} interrupted", individualIRI, e);
                throw new RuntimeException("Read object interrupted", e);
            } catch (ExecutionException e) {
                logger.error("Execution exception when reading object {}", individualIRI, e);
                throw new RuntimeException("Execution exception when reading object", e);
            }
            try {
                final @NonNull T constructedObject = ClassBuilder.ConstructObject(clazz, constructorArguments);
                return Optional.of(constructedObject);
            } catch (MissingConstructorException e) {
                logger.error("Problem with constructor", e);
                return Optional.empty();
            }
        } else {
            throw new RuntimeException("No data properties, not even trying");
        }
    }

    /**
     * Spatial Intersect Object with most recent records in the database
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param inputObject - Object to intersect
     * @param buffer      - Additional buffer (in meters)
     * @param <T>         - Type to specialize method
     * @return - An Optional List of Object T
     */
    @SuppressWarnings("return.type.incompatible")
    public <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer) {
        return spatialIntersectObject(inputObject, buffer, null);
    }

    /**
     * Spatial Intersect Object with records in the database valid at that given time
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param inputObject - Object to intersect
     * @param buffer      - Additional buffer to build around object (in meters)
     * @param temporalAt  - Temporal of intersecting time point
     * @param <T>         - Type to specialize method
     * @return - An Optional List of Object T
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer, @Nullable Temporal temporalAt) {
        final OWLNamedIndividual owlNamedIndividual = trestleParser.classParser.GetIndividual(inputObject);
        final Optional<String> wktString = SpatialParser.GetSpatialValue(inputObject);

        if (wktString.isPresent()) {
            return spatialIntersect((Class<T>) inputObject.getClass(), wktString.get(), buffer, temporalAt);
        }

        logger.info("{} doesn't have a spatial component", owlNamedIndividual);
        return Optional.empty();
    }

    /**
     * Find objects of a given class that intersect with a specific WKT boundary.
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param clazz  - Class of object to return
     * @param wkt    - WKT of spatial boundary to intersect with
     * @param buffer - Double buffer to build around wkt (in meters)
     * @param <T>    - Type to specialize method
     * @return - An Optional List of Object T
     */
    public <T> Optional<List<T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer) {
        return spatialIntersect(clazz, wkt, buffer, null);
    }

    /**
     * Find objects of a given class that intersect with a specific WKT boundary.
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param clazz      - Class of object to return
     * @param wkt        - WKT of spatial boundary to intersect with
     * @param buffer     - Double buffer to build around wkt
     * @param atTemporal - Temporal to filter results to specific valid time point
     * @param <T>        - Class to specialize method with.
     * @return - An Optional List of Object T.
     */
    @SuppressWarnings("return.type.incompatible")
    public <T> Optional<List<T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer, @Nullable Temporal atTemporal) {

        final CompletableFuture<Optional<List<@NonNull T>>> intersectFuture = spatialIntersectAsync(clazz, wkt, buffer, atTemporal);
        try {
            return intersectFuture.get();
        } catch (InterruptedException e) {
            logger.error("Interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Execution exception", e);
        }

        throw new RuntimeException("Problem intersecting object");
    }

    /**
     * Async intersection of given class with WKT boundary
     *
     * @param clazz      - Class of object to return
     * @param wkt        - WKT of spatial boundary to intersect with
     * @param buffer     - Double buffer to build around WKT
     * @param atTemporal - Temporal to filter results to specific valid time point
     * @param <T>        - Type to specialize Future with
     * @return - Completable Future of Optional List of T
     */
    private <T> CompletableFuture<Optional<List<T>>> spatialIntersectAsync(Class<@NonNull T> clazz, String wkt, double buffer, @Nullable Temporal atTemporal) {
        return CompletableFuture.supplyAsync(() -> {
            final OWLClass owlClass = trestleParser.classParser.GetObjectClass(clazz);

            String spatialIntersection = null;
            try {
                if (atTemporal == null) {
                    logger.debug("Running generic spatial intersection");
                    spatialIntersection = qb.buildSpatialIntersection(owlClass, wkt, buffer, QueryBuilder.UNITS.METER);
                } else {
                    final OffsetDateTime atLDTime = parseTemporalToOntologyDateTime(atTemporal, TemporalParser.IntervalType.START, ZoneOffset.UTC);
                    logger.debug("Running spatial intersection at time {}", atLDTime);
                    spatialIntersection = qb.buildTemporalSpatialIntersection(owlClass, wkt, buffer, QueryBuilder.UNITS.METER, atLDTime);
                }
            } catch (UnsupportedFeatureException e) {
                logger.error("Database {} doesn't support spatial intersections.", spatialDalect, e);
                return Optional.empty();
            }

            logger.debug("Executing spatial query");
            final Instant start = Instant.now();
            final TrestleResultSet resultSet = this.ontology.executeSPARQLTRS(spatialIntersection);
            final Instant end = Instant.now();
            logger.debug("Spatial query returned in {} ms", Duration.between(start, end).toMillis());
            Set<IRI> intersectedIRIs = resultSet.getResults()
                    .stream()
                    .map(result -> IRI.create(result.getIndividual("m").toStringID()))
                    .collect(Collectors.toSet());
            logger.debug("Intersected with {} objects", intersectedIRIs.size());
            if (intersectedIRIs.size() == 0) {
                logger.info("No intersected results");
                return Optional.of(new ArrayList<@NonNull T>());
            }

//            I think I need to suppress this warning to deal with generics in streams
            @SuppressWarnings("argument.type.incompatible") final List<@NonNull T> intersectedObjects = intersectedIRIs
                    .stream()
                    .map(iri -> {
                        try {
                            return (@NonNull T) readAsObject(clazz, iri, false);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            return Optional.of(intersectedObjects);
        });
    }

    /**
     * Get a map of related objects and their relative strengths
     *
     * @param clazz    - Java class of object to serialize to
     * @param objectID - Object ID to retrieve related objects
     * @param cutoff   - Double of relation strength cutoff
     * @param <T>      - Type to specialize return with
     * @return - Optional Map of related java objects and their corresponding relational strength
     */
    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @SuppressWarnings("return.type.incompatible")
    @Deprecated
    public <T> Optional<Map<@NonNull T, Double>> getRelatedObjects(Class<@NonNull T> clazz, String objectID, double cutoff) {


        final OWLClass owlClass = trestleParser.classParser.GetObjectClass(clazz);

        final String relationQuery = qb.buildRelationQuery(df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX, objectID)), owlClass, cutoff);
        TrestleTransaction transaction = ontology.createandOpenNewTransaction(false);
        final TrestleResultSet resultSet = this.ontology.executeSPARQLTRS(relationQuery);

        Set<IRI> relatedIRIs = new HashSet<>();
        Map<@NonNull T, Double> relatedObjects = new HashMap<>();
        Map<IRI, Double> relatedObjectResults = new HashMap<>();
        resultSet.getResults()
                .forEach(result -> relatedObjectResults.put(IRI.create(result.getIndividual("f").toStringID()), result.getLiteral("s").parseDouble()));

        relatedObjectResults
                .entrySet().forEach(entry -> {
            final @NonNull T object = readAsObject(clazz, entry.getKey(), false);
            relatedObjects.put(object, entry.getValue());
        });
        ontology.returnAndCommitTransaction(transaction);

        if (relatedObjects.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(relatedObjects);
    }

    /**
     * For a given individual, get all related concepts and the IRIs of all members of those concepts,
     * that have a relation strength above the given cutoff value
     *
     * @param individual       - String of individual IRI to return relations for
     * @param conceptID        - Nullable String of concept IRI to filter members of
     * @param relationStrength - Cutoff value of minimum relation strength
     * @return
     */
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
        final TrestleResultSet resultSet = this.ontology.executeSPARQLTRS(conceptQuery);
        resultSet.getResults()
                .forEach(result -> conceptIndividuals.put(result.getIndividual("concept").toStringID(), result.getIndividual("individual").toStringID()));

        if (conceptIndividuals.keySet().size() == 0) {
            logger.info("Individual {} has no related concepts");
            return Optional.empty();
        }
        return Optional.of(Multimaps.asMap(conceptIndividuals));
    }

    /**
     * Remove individuals from the ontology
     *
     * @param inputObject - Individual to remove
     * @param <T>         - Type of individual to remove
     */
    public <T> void removeIndividual(@NonNull T... inputObject) {
        T[] objects = inputObject;
        final List<CompletableFuture<Void>> completableFutures = Arrays.stream(objects)
                .map(object -> CompletableFuture.supplyAsync(() -> trestleParser.classParser.GetIndividual(object)))
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
        });

    }

    /**
     * Search the ontology for individuals with IRIs that match the given search string
     *
     * @param individualIRI - String to search for matching IRI
     * @return - List of Strings representing IRIs of matching individuals
     */
    public List<String> searchForIndividual(String individualIRI) {
        return searchForIndividual(individualIRI, null, null);
    }

    /**
     * Search the ontology for individuals with IRIs that match the given search string
     *
     * @param individualIRI - String to search for matching IRI
     * @param datasetClass  - Optional datasetClass to restrict search to
     * @param limit         - Optional limit to returned results
     * @return - List of Strings representing IRIs of matching individuals
     */
    public List<String> searchForIndividual(String individualIRI, @Nullable String datasetClass, @Nullable Integer limit) {
        @Nullable OWLClass owlClass = null;
        if (datasetClass != null) {
            owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, datasetClass));
        }
        final String query = qb.buildIndividualSearchQuery(individualIRI, owlClass, limit);
        final TrestleResultSet resultSet = ontology.executeSPARQLTRS(query);
        List<String> individuals = resultSet.getResults()
                .stream()
                .map(result -> result.getIndividual("m").toStringID())
                .collect(Collectors.toList());
        return individuals;
    }

    /**
     * Return a TrestleIndividual with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - TrestleIndividual
     */
    public TrestleIndividual getTrestleIndividual(String individualIRI) {
        if (cachingEnabled) {
            return trestleCache.IndividualCache().get(individualIRI, iri -> getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, iri))));
        }
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(REASONER_PREFIX, individualIRI)));
    }

    /**
     * Return a TrestleIndividual with all available facts and relations
     *
     * @param individual - OWLNamedIndividual to retrieve facts for
     * @return - TrestleIndividual
     */
    private TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {

        logger.debug("Building trestle individual {}", individual);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        final CompletableFuture<TrestleIndividual> temporalFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(individual, hasTemporalIRI);
            this.ontology.returnAndCommitTransaction(tt);
            return individualObjectProperty;
        })
                .thenApply(individualObjectProperty -> {
                    if (!individualObjectProperty.isPresent()) {
                        throw new CompletionException(new TrestleMissingIndividualException(individual));
                    }
                    return individualObjectProperty.get().stream().findFirst().orElseThrow(() -> new CompletionException(new TrestleMissingFactException(individual, existsTimeIRI)));
                })
                .thenApply(temporalProperty -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    final OWLNamedIndividual temporalIndividual = temporalProperty.getObject().asOWLNamedIndividual();
                    final Set<OWLDataPropertyAssertionAxiom> temporalDataProperties = ontology.getAllDataPropertiesForIndividual(temporalIndividual);
                    this.ontology.returnAndCommitTransaction(tt);
                    return new TemporalPropertiesPair(temporalIndividual, temporalDataProperties);
                })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()))
                .thenApply(temporalObject -> new TrestleIndividual(individual.toStringID(), temporalObject.orElseThrow(() -> new CompletionException(new TrestleMissingFactException(individual, hasTemporalIRI)))));

//                Get all the facts
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualFacts = ontology.getIndividualObjectProperty(individual, hasFactIRI);
        final List<CompletableFuture<TrestleFact>> factFutureList = individualFacts.orElse(new HashSet<>())
                .stream()
                .map(fact -> buildTrestleFact(fact.getObject().asOWLNamedIndividual(), trestleTransaction))
                .collect(Collectors.toList());

        CompletableFuture<List<TrestleFact>> factsFuture = sequenceCompletableFutures(factFutureList);

        final CompletableFuture<List<TrestleRelation>> relationsFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            String query = this.qb.buildIndividualRelationQuery(individual);
            TrestleResultSet resultSet = ontology.executeSPARQLTRS(query);
            this.ontology.returnAndCommitTransaction(tt);
            return resultSet;
        })
                .thenApply(sparqlResults -> {
                    List<TrestleRelation> relations = new ArrayList<>();
                    sparqlResults.getResults()
                            .stream()
//                            We want the subProperties of Temporal/Spatial relations. So we filter them out
                            .filter(result -> !result.getIndividual("o").asOWLNamedIndividual().getIRI().equals(temporalRelationIRI))
                            .filter(result -> !result.getIndividual("o").asOWLNamedIndividual().getIRI().equals(spatialRelationIRI))
//                            Filter out self
                            .filter(result -> !result.getIndividual("p").asOWLNamedIndividual().equals(individual))
                            .forEach(result -> relations.add(new TrestleRelation(result.getIndividual("m").toStringID(),
                                    ObjectRelation.getRelationFromIRI(IRI.create(result.getIndividual("o").toStringID())),
                                    result.getIndividual("p").toStringID())));
                    return relations;
                });

        final CompletableFuture<TrestleIndividual> individualFuture = temporalFuture.thenCombine(relationsFuture, (trestleIndividual, relations) -> {
            relations.forEach(trestleIndividual::addRelation);
            return trestleIndividual;
        })
                .thenCombine(factsFuture, (trestleIndividual, trestleFacts) -> {
                    trestleFacts.forEach(trestleIndividual::addFact);
                    return trestleIndividual;
                });

        try {
            TrestleIndividual trestleIndividual = individualFuture.get();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return trestleIndividual;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Interruption exception building Trestle Individual {}", individual, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a set of Trestle_Concepts that intersect with the given WKT
     *
     * @param wkt    - String of WKT to intersect with
     * @param buffer - double buffer to draw around WKT
     * @return - Optional Set of String URIs for intersected concepts
     */
    public Optional<Set<String>> STIntersectConcept(String wkt, double buffer) {
        final String queryString;

        try {
            queryString = qb.buildTemporalSpatialConceptIntersection(wkt, buffer, null);
        } catch (UnsupportedFeatureException e) {
            logger.error("Database {} does not support spatial queries", this.spatialDalect);
            return Optional.empty();
        }


        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final TrestleResultSet resultSet = this.ontology.executeSPARQLTRS(queryString);
        final Set<String> intersectedConceptURIs = resultSet.getResults()
                .stream()
                .map(result -> result.getIndividual("m").toStringID())
                .collect(Collectors.toSet());
        this.ontology.returnAndCommitTransaction(trestleTransaction);

        return Optional.of(intersectedConceptURIs);
    }

    /**
     * Retrieve all members of a specified concept that match a given class
     *
     * @param <T>                  - Generic type T of returned object
     * @param clazz                - Input class to retrieve from concept
     * @param conceptID            - String ID of concept to retrieve
     * @param spatialIntersection  - Optional spatial intersection to restrict results
     * @param temporalIntersection - Optional temporal intersection to restrict results
     * @return - Optional Set of T objects
     */
    public <T> Optional<List<T>> getConceptMembers(Class<T> clazz, String conceptID, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection) {
        if ((spatialIntersection != null) || (temporalIntersection != null)) {
            logger.warn("Spatio-temporal intersections not implemented yet");
        }


        final OWLClass datasetClass = trestleParser.classParser.GetObjectClass(clazz);
        final String retrievalStatement = qb.buildConceptObjectRetrieval(datasetClass, parseStringToIRI(REASONER_PREFIX, conceptID));

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        Set<String> individualIRIs = this.ontology.executeSPARQLTRS(retrievalStatement)
                .getResults()
                .stream()
                .map(result -> result.getIndividual("m").toStringID())
                .collect(Collectors.toSet());

//        Try to retrieve the object members in an async fashion
        final List<CompletableFuture<T>> completableFutureList = individualIRIs
                .stream()
                .map(iri -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction futureTransaction = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final @NonNull T retrievedObject = this.readAsObject(clazz, iri);
                        return retrievedObject;
                    } catch (TrestleClassException e) {
                        logger.error("Unregistered class", e);
                        throw new RuntimeException(e);
                    } catch (MissingOntologyEntity e) {
                        logger.error("Cannot find ontology individual {}", e.getIndividual(), e);
                        throw new RuntimeException(e);
                    } finally {
                        this.ontology.returnAndCommitTransaction(futureTransaction);
                    }
                }))
                .collect(Collectors.toList());
        final CompletableFuture<List<T>> conceptObjectsFuture = sequenceCompletableFutures(completableFutureList);
        try {
            List<T> objects = conceptObjectsFuture.get();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.of(objects);
        } catch (InterruptedException e) {
            logger.error("Object retrieval for concept {}, interrupted", conceptID, e);
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Unable to retrieve all objects for concept {}", conceptID, e);
            return Optional.empty();
        }
    }

    /**
     * Write an object into the database, as a member of a given concept
     *
     * @param conceptIRI   - String of
     * @param inputObject  - Object to write into databse
     * @param relationType - ConceptRelationType
     * @param strength     - Strength parameter of relation
     */
    public void addObjectToConcept(String conceptIRI, Object inputObject, ConceptRelationType relationType, double strength) {
//        Write the object
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            this.writeAsTrestleObject(inputObject);
        } catch (TrestleClassException e) {
            logger.error("Problem with class", e);
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual", e.getIndividual(), e);
        }
//        Create the concept relation
        final IRI concept = parseStringToIRI(REASONER_PREFIX, conceptIRI);
        final OWLNamedIndividual conceptIndividual = df.getOWLNamedIndividual(concept);
        final OWLNamedIndividual individual = this.trestleParser.classParser.GetIndividual(inputObject);
        final IRI relationIRI = IRI.create(String.format("relation:%s:%s",
                extractTrestleIndividualName(concept),
                extractTrestleIndividualName(individual.getIRI())));
        final OWLNamedIndividual relationIndividual = df.getOWLNamedIndividual(relationIRI);
        final OWLClass relationClass = df.getOWLClass(trestleRelationIRI);
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
        try {
            ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                    df.getOWLObjectProperty(relationOfIRI),
                    relationIndividual,
                    individual));
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", e.getIndividual(), e);
        }
        try {
            ontology.writeIndividualDataProperty(relationIndividual,
                    df.getOWLDataProperty(relationStrengthIRI),
                    df.getOWLLiteral(strength));
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", e.getIndividual(), e);
        }

//        Write the relation to the concept
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
            try {
                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(relatedToIRI),
                        relationIndividual,
                        conceptIndividual));
            } catch (MissingOntologyEntity e) {
                logger.error("Individual {} does not exist", e.getIndividual());
            }
        }
        this.ontology.returnAndCommitTransaction(trestleTransaction);
    }

    /**
     * Write a relationship between two objects.
     * If one or both of those objects do not exist, create them.
     *
     * @param subject  - Java object to write as subject of relationship
     * @param object   - Java object to write as object of relationship
     * @param relation - ObjectRelation between the two object
     */
    public void writeObjectRelationship(Object subject, Object object, ObjectRelation relation) {
        this.writeObjectProperty(subject, object, df.getOWLObjectProperty(relation.getIRI()));
    }

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject - Java object to write as subject of relationship
     * @param object  - Java object to write as object of relationship
     * @param wkt     - String of wkt boundary of spatial overlap
     */
    public void writeSpatialOverlap(Object subject, Object object, String wkt) {
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.GetIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.GetIndividual(object);
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

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject         - Java object to write as subject of relationship
     * @param object          - Java object to write as object of relationship
     * @param temporalOverlap - String of temporal overlap between two objects (Not implemented yet)
     */
//    TODO(nrobison): Correctly implement this
    public void writeTemporalOverlap(Object subject, Object object, String temporalOverlap) {
        logger.warn("Temporal overlaps not implemented yet, overlap value has no meaning");
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.GetIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.GetIndividual(object);
        final OWLNamedIndividual overlapIndividual = df.getOWLNamedIndividual(IRI.create(REASONER_PREFIX,
                String.format("overlap:%s:%s",
                        subjectIndividual.getIRI().getShortForm(),
                        objectIndividual.getIRI().getShortForm())));

//        Write the overlap
        final OWLClassAssertionAxiom overlapClassAssertion = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleOverlapIRI), overlapIndividual);
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
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.GetIndividual(object);
        final OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = df.getOWLObjectPropertyAssertionAxiom(property, subject, objectIndividual);
        try {
            this.ontology.writeIndividualObjectProperty(owlObjectPropertyAssertionAxiom);
        } catch (MissingOntologyEntity missingOntologyEntity) {
            logger.debug("Missing individual {}, creating", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            try {
                this.writeAsTrestleObject(object);
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
        final OWLNamedIndividual subjectIndividual = trestleParser.classParser.GetIndividual(subject);
        final OWLNamedIndividual objectIndividual = trestleParser.classParser.GetIndividual(object);
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
                    this.writeAsTrestleObject(subject);
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
                        this.writeAsTrestleObject(object);
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
     * @param factIndividual - OWLNamedIndividual to construct fact from
     * @param transactionObject - TrestleTransaction object that gets passed from the parent function
     * @return - TrestleFact
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<TrestleFact> buildTrestleFact(OWLNamedIndividual factIndividual, TrestleTransaction transactionObject) {

        final CompletableFuture<FactPair> factFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getAllDataPropertiesForIndividual(factIndividual);
            this.ontology.returnAndCommitTransaction(tt);
            return allDataPropertiesForIndividual;
        })
//        There's only one data property per fact, so we can do this.
                .thenApply(factProperties -> factProperties.stream().findFirst())
                .thenApply(first -> first.orElseThrow(() -> new TrestleMissingFactException(factIndividual)))
                .thenApply(factAssertion -> {
//                    final Class<?> literalClass = TypeConverter.lookupJavaClassFromOWLDatatype(factAssertion, null);
                    return new FactPair(factAssertion, factAssertion.getObject());
//                    return new FactPair(factAssertion, TypeConverter.extractOWLLiteral(literalClass, factAssertion.getObject()));
                });
//        final Set<OWLDataPropertyAssertionAxiom> factProperties = ontology.getAllDataPropertiesForIndividual(factIndividual);

//        final Optional<OWLDataPropertyAssertionAxiom> first = factProperties.stream().findFirst();
//
//        final OWLDataPropertyAssertionAxiom factAssertion = first.orElseThrow(() -> new TrestleMissingFactException(factIndividual));
//        final Class<?> literalClass = TypeConverter.lookupJavaClassFromOWLDatatype(factAssertion, null);
//        final Object literal = TypeConverter.extractOWLLiteral(literalClass, factAssertion.getObject());

//            Now the temporals

        final CompletableFuture<Optional<TemporalObject>> validFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(factIndividual, validTimeIRI);
            this.ontology.returnAndCommitTransaction(tt);
            return individualObjectProperty;
        })
                .thenApply(temporalProperties -> temporalProperties.orElseThrow(() -> new TrestleMissingFactException(factIndividual, validTimeIRI)).stream().findFirst())
                .thenApply(temporalProperty -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
                    final OWLNamedIndividual temporalIndividual = temporalProperty.orElseThrow(() -> new TrestleMissingFactException(factIndividual, validTimeIRI)).getObject().asOWLNamedIndividual();
                    final Set<OWLDataPropertyAssertionAxiom> temporalProperties = ontology.getAllDataPropertiesForIndividual(temporalIndividual);
                    this.ontology.returnAndCommitTransaction(tt);
                    return new TemporalPropertiesPair(temporalIndividual, temporalProperties);
                })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()));

        final CompletableFuture<Optional<TemporalObject>> databaseFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            final Optional<Set<OWLObjectPropertyAssertionAxiom>> temporalIndividual = ontology.getIndividualObjectProperty(factIndividual, databaseTimeIRI);
            this.ontology.returnAndCommitTransaction(tt);
            return temporalIndividual;
        })
                .thenApply(temporalProperties -> temporalProperties.orElseThrow(() -> new TrestleMissingFactException(factIndividual, databaseTimeIRI)).stream().findFirst())
                .thenApply(temporalProperty -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
                    final OWLNamedIndividual temporalIndividual = temporalProperty.orElseThrow(() -> new TrestleMissingFactException(factIndividual, databaseTimeIRI)).getObject().asOWLNamedIndividual();
                    final Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getAllDataPropertiesForIndividual(temporalIndividual);
                    this.ontology.returnAndCommitTransaction(tt);
                    return new TemporalPropertiesPair(temporalIndividual, allDataPropertiesForIndividual);
                })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()));

//        Sequence the temporals in valid/database order
        final CompletableFuture<TemporalPair> temporalsFuture = validFuture.thenCombine(databaseFuture, (valid, database) -> new TemporalPair(
                valid.orElseThrow(() -> new TrestleMissingFactException(factIndividual, validTimeIRI)),
                 database.orElseThrow(() -> new TrestleMissingFactException(factIndividual, databaseTimeIRI))));
//        final Optional<Set<OWLObjectPropertyAssertionAxiom>> temporalProperties = ontology.getIndividualObjectProperty(factIndividual, validTimeIRI);
//        final Optional<OWLObjectPropertyAssertionAxiom> temporalProperty = temporalProperties.orElseThrow(() -> new TrestleMissingFactException(factIndividual, hasTemporalIRI)).stream().findFirst();
//        final Set<OWLDataPropertyAssertionAxiom> temporalDataProperties = ontology.getAllDataPropertiesForIndividual(temporalProperty.orElseThrow(() -> new TrestleMissingFactException(factIndividual, hasTemporalIRI)).getObject().asOWLNamedIndividual());
//        final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromProperties(temporalDataProperties, null);

//        Database time
//        final Optional<Set<OWLObjectPropertyAssertionAxiom>> databaseTimeProperties = ontology.getIndividualObjectProperty(factIndividual, databaseTimeIRI);
//        final Optional<OWLObjectPropertyAssertionAxiom> databaseTimeProperty = databaseTimeProperties.orElseThrow(() -> new TrestleMissingFactException(factIndividual, hasTemporalIRI)).stream().findFirst();
//        final Set<OWLDataPropertyAssertionAxiom> databaseTemporalDataProperties = ontology.getAllDataPropertiesForIndividual(databaseTimeProperty.orElseThrow(() -> new TrestleMissingFactException(factIndividual, hasTemporalIRI)).getObject().asOWLNamedIndividual());
//        final Optional<TemporalObject> databaseTemporal = TemporalObjectBuilder.buildTemporalFromProperties(databaseTemporalDataProperties, null);

        return temporalsFuture.thenCombine(factFuture, (temporalPair, factPair) -> new TrestleFact<>(factIndividual.getIRI().toString(),
                factPair.getAssertion().getProperty().asOWLDataProperty().getIRI().getShortForm(),
                factPair.getLiteral(),
                temporalPair.getValid(),
                temporalPair.getDatabase()));

//        return new TrestleFact<>(factIndividual.getIRI().toString(),
//                factAssertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
//                literal,
//                validTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual, hasTemporalIRI)),
//                databaseTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual, databaseTimeIRI)));
    }

    public void registerClass(Class inputClass) throws TrestleClassException {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.put(trestleParser.classParser.GetObjectClass(inputClass), inputClass);
    }

    /**
     * Write temporal object into the database, optionally override given scope
     * If no OWLNamedIndividual is given, don't write any association
     * @param temporal- TemporalObject to create
     * @param individual - Optional OWLNamedIndividual to associate with temporal
     * @param overrideTemporalScope - Optionally override scope of temporal object
     * @param overrideTemporalAssociation - Optionally override temporal association
     * @throws MissingOntologyEntity - Throws if it can't find the temporal to write properties on
     */
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
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), TemporalParser.IntervalType.START, temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            temporalValidToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), TemporalParser.IntervalType.END, temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            } else {
//                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsFromIRI,
                        parseTemporalToOntologyDateTime(temporal.asInterval().getFromTime(), TemporalParser.IntervalType.START, temporal.asInterval().getStartTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalExistsToIRI,
                            parseTemporalToOntologyDateTime(toTime.get(), TemporalParser.IntervalType.END, temporal.asInterval().getEndTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            dateTimeDatatypeIRI);
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalValidAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), TemporalParser.IntervalType.END, temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        dateTimeDatatypeIRI);
            } else {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsAtIRI,
                        parseTemporalToOntologyDateTime(temporal.asPoint().getPointTime(), TemporalParser.IntervalType.END, temporal.asPoint().getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
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

    /**
     * Get a list of currently registered datasets
     * Only returns datasets currently registered with the reasoner.
     *
     * @return - Set of Strings representing the registered datasets
     */
    public Set<String> getAvailableDatasets() {

        final String datasetQuery = qb.buildDatasetQuery();
        final TrestleResultSet resultSet = ontology.executeSPARQLTRS(datasetQuery);
        List<OWLClass> datasetsInOntology = resultSet
                .getResults()
                .stream()
                .map(result -> df.getOWLClass(result.getIndividual("dataset").toStringID()))
                .collect(Collectors.toList());

        return this.registeredClasses
                .keySet()
                .stream()
                .filter(datasetsInOntology::contains)
                .map(individual -> individual.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Class<?> getDatasetClass(String owlClassString) throws UnregisteredClassException {
        final OWLClass owlClass = df.getOWLClass(parseStringToIRI(REASONER_PREFIX, owlClassString));
        final Class<?> aClass = this.registeredClasses.get(owlClass);
        if (aClass == null) {
            throw new UnregisteredClassException(owlClass);
        }
        return aClass;
    }

    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException {

        //        Build shapefile schema
//        TODO(nrobison): Extract type from wkt
//        FIXME(nrobison): Shapefile schema doesn't support multiple languages. Need to figure out how to flatten
        final ShapefileSchema shapefileSchema = new ShapefileSchema(MultiPolygon.class);
        final Optional<List<OWLDataProperty>> propertyMembers = ClassBuilder.getPropertyMembers(inputClass, true);
        propertyMembers.ifPresent(owlDataProperties -> owlDataProperties.forEach(property -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, property.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, property))));

//        Now the temporals
        final Optional<List<OWLDataProperty>> temporalProperties = trestleParser.temporalParser.GetTemporalsAsDataProperties(inputClass);
        temporalProperties.ifPresent(owlDataProperties -> owlDataProperties.forEach(temporal -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, temporal.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, temporal))));


        final List<CompletableFuture<Optional<TSIndividual>>> completableFutures = objectID
                .stream()
                .map(id -> IRIUtils.parseStringToIRI(REASONER_PREFIX, id))
//                .map(id -> readAsObject(inputClass, id))
                .map(id -> CompletableFuture.supplyAsync(() -> readAsObject(inputClass, id, false)))
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
            e.printStackTrace();
        }

//        Build the shapefile exporter
        throw new RuntimeException("Problem constructing object");
    }

    private <T> Optional<TSIndividual> parseIndividualToShapefile(T object, ShapefileSchema shapefileSchema) {
//        if (objectOptional.isPresent()) {
//            final T object = objectOptional.get();
        final Class<?> inputClass = object.getClass();
        final Optional<OWLDataPropertyAssertionAxiom> spatialProperty = trestleParser.classParser.GetSpatialProperty(object);
        if (!spatialProperty.isPresent()) {
            logger.error("Individual is not a spatial object");
            return Optional.empty();
        }
        final TSIndividual individual = new TSIndividual(spatialProperty.get().getObject().getLiteral(), shapefileSchema);
//                    Data properties, filtering out the spatial members
        final Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = trestleParser.classParser.GetDataProperties(object, true);
        owlDataPropertyAssertionAxioms.ifPresent(owlDataPropertyAssertionAxioms1 -> owlDataPropertyAssertionAxioms1.forEach(property -> {
            final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, object.getClass());
            final Object literal = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
            individual.addProperty(ClassParser.matchWithClassMember(inputClass, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                    literal);
        }));
//                    Temporals
        final Optional<List<TemporalObject>> temporalObjects = trestleParser.temporalParser.GetTemporalObjects(object);
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
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> relatedToProperties = ontology.getIndividualObjectProperty(firstIndividual, hasRelationIRI);
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

    @EnsuresNonNullIf(expression = "this.trestleCache", result = true)
    private boolean isCachingEnabled() {
        return this.trestleCache != null;
    }
}
