package com.nickrobison.trestle;

import com.nickrobison.trestle.caching.TrestleCache;
import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.TrestleMissingAttributeException;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.exceptions.*;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.exporter.ShapefileExporter;
import com.nickrobison.trestle.exporter.ShapefileSchema;
import com.nickrobison.trestle.exporter.TSIndividual;
import com.nickrobison.trestle.ontology.*;
import com.nickrobison.trestle.parser.*;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.TrestleAttribute;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.PointTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaExceptionUtil.rethrowFunction;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Created by nrobison on 5/17/16.
 */
@SuppressWarnings({"methodref.inference.unimplemented"})
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);
    public static final String DEFAULTNAME = "trestle";

    private final ITrestleOntology ontology;
    private final Map<OWLClass, Class<?>> registeredClasses = new HashMap<>();
    private final OWLDataFactory df;
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(datasetClassIRI);
    private final QueryBuilder qb;
    private final QueryBuilder.DIALECT spatialDalect;
    private boolean cachingEnabled = true;
    private @Nullable TrestleCache trestleCache = null;

    @SuppressWarnings("dereference.of.nullable")
    TrestleReasoner(TrestleBuilder builder) throws OWLOntologyCreationException {

        df = OWLManager.getOWLDataFactory();

//        If we have a manually specified ontology, use that.
        final URL ontologyResource;
        final InputStream ontologyIS;
        if (builder.ontologyIRI.isPresent()) {
            try {
                ontologyResource = builder.ontologyIRI.get().toURI().toURL();
                ontologyIS = new FileInputStream(new File(builder.ontologyIRI.get().toURI()));
            } catch (MalformedURLException e) {
                logger.error("Unable to parse IRI to URI", e);
                throw new RuntimeException("Unable to parse IRI to URI", e);
            } catch (FileNotFoundException e) {
                logger.error("Cannot find ontology file");
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

//            validate the classes
        builder.inputClasses.forEach(clazz -> {
            try {
                ClassRegister.ValidateClass(clazz);
                this.registeredClasses.put(ClassParser.GetObjectClass(clazz), clazz);
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
                .name(builder.ontologyName.orElse(DEFAULTNAME));
        if (builder.connectionString.isPresent()) {
            ontologyBuilder = ontologyBuilder.withDBConnection(builder.connectionString.get(),
                    builder.username,
                    builder.password);
        }

        ontology = ontologyBuilder.build().orElseThrow(() -> new RuntimeException("Cannot build ontology"));
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
//            TODO(nrobison): This needs to be better
            spatialDalect = QueryBuilder.DIALECT.SESAME;
        }
        logger.debug("Using SPARQL dialect {}", spatialDalect);
        qb = new QueryBuilder(spatialDalect, ontology.getUnderlyingPrefixManager());
        logger.info("Ontology {} ready", builder.ontologyName.orElse(DEFAULTNAME));
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
        final OWLClass owlClass = ClassParser.GetObjectClass(inputClass);
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

    /**
     * Write a java object as a TS_Concept
     *
     * @param inputObject - Input object to write as concept
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     */
    public void writeObjectAsConcept(Object inputObject) throws TrestleClassException, MissingOntologyEntity {

        writeObject(inputObject, TemporalScope.EXISTS, null);
    }

    /**
     * Write a java object as a TS_Fact
     *
     * @param inputObject - Input object to write as fact
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     */
    public void writeObjectAsFact(Object inputObject) throws TrestleClassException, MissingOntologyEntity {
        writeObject(inputObject, TemporalScope.VALID, null);
    }

    /**
     * Write object into the ontology as a Fact
     * Use the provided temporals to setup the database time
     *
     * @param inputObject   - Object to write into the ontology
     * @param startTemporal - Start of database time interval
     * @param endTemporal   - @Nullable Temporal of ending interval time
     */
    @SuppressWarnings("unchecked")
    public void writeObjectAsFact(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException {

        final TemporalObject databaseTemporal;
        if (endTemporal == null) {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).withRelations(ClassParser.GetIndividual(inputObject));
        } else {
            databaseTemporal = TemporalObjectBuilder.valid().from(startTemporal).to(endTemporal).withRelations(ClassParser.GetIndividual(inputObject));
        }
        writeObject(inputObject, TemporalScope.VALID, databaseTemporal);
    }

    /**
     * Writes a data property as an individual object, with relations back to the root dataset individual
     *
     * @param rootIndividual   - OWLNamedIndividual of the dataset individual
     * @param properties       - List of OWLDataPropertyAssertionAxioms to write as objects
     * @param temporal         - Temporal to associate with data property individual
     * @param databaseTemporal - Temporal repsenting database time
     */
    private void writeDataPropertyAsObject(OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject temporal, TemporalObject databaseTemporal) {
        final long now = Instant.now().getEpochSecond();
        final OWLClass factClass = df.getOWLClass(factClassIRI);
        properties.forEach(property -> {

//            TODO(nrobison): We should change this to lookup any existing records to correctly increment the record number
            final OWLNamedIndividual propertyIndividual = df.getOWLNamedIndividual(IRI.create(PREFIX, String.format("%s:%s:%d", rootIndividual.getIRI().getShortForm(), property.getProperty().asOWLDataProperty().getIRI().getShortForm(), now)));
            ontology.createIndividual(propertyIndividual, factClass);
            try {
//                Write the property
                ontology.writeIndividualDataProperty(propertyIndividual, property.getProperty().asOWLDataProperty(), property.getObject());
//                Write the temporal relation
                ontology.writeIndividualObjectProperty(propertyIndividual, validTimeIRI, df.getOWLNamedIndividual(temporal.getIDAsIRI()));
//                Write the relation back to the root individual
                ontology.writeIndividualObjectProperty(propertyIndividual, factOfIRI, rootIndividual);
//                TODO(nrobison): Write the DB access time
//                Write the database time
                ontology.writeIndividualObjectProperty(propertyIndividual, databaseTimeIRI, df.getOWLNamedIndividual(databaseTemporal.getIDAsIRI()));
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
//        try {
        checkRegisteredClass(aClass);
//        } catch (UnregisteredClassException e) {
//            throw new CompletionException(e);
//        }

        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);

//            Create the database time object
        final TemporalObject dTemporal;
        if (databaseTemporal == null) {
            dTemporal = TemporalObjectBuilder.valid().from(OffsetDateTime.now()).withRelations(owlNamedIndividual);
        } else {
            dTemporal = databaseTemporal;
        }

//        Write the class
        final OWLClass owlClass = ClassParser.GetObjectClass(inputObject);
        final TrestleTransaction trestleTransaction = ontology.createandOpenNewTransaction(true);
        ontology.associateOWLClass(owlClass, datasetClass);
//        Write the individual
        ontology.createIndividual(owlNamedIndividual, owlClass);
//            this.ontology.commitTransaction();
//        Write the temporal
//            final CompletableFuture<Void> temporalFutures = CompletableFuture.runAsync(() -> {
        final Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(inputObject);
        if (temporalObjects.isPresent()) {
            temporalObjects.get().forEach(temporal -> {
                try {
                    writeTemporalWithAssociation(temporal, owlNamedIndividual, scope, null);
                } catch (MissingOntologyEntity e) {
                    logger.error("Individual {} missing in ontology", e.getIndividual(), e);
//                    throw new CompletionException(e);
                }
            });
        }

//            Write the database temporal
//        try {
        writeTemporalWithAssociation(dTemporal, owlNamedIndividual, scope, databaseTimeIRI);
//        } catch (MissingOntologyEntity e) {
//            logger.error("Individual {} missing in ontology", e.getIndividual(), e);
//            throw new MissingOntologyEntity(e.g);
//        }
//            });

//        Write the data properties
        final Optional<List<OWLDataPropertyAssertionAxiom>> dataProperties = ClassParser.GetDataProperties(inputObject);
//        final CompletableFuture<Void> propertiesFutures = CompletableFuture.runAsync(() -> {
        if (dataProperties.isPresent()) {
//                final TrestleTransaction threadTransaction = ontology.createandOpenNewTransaction(trestleTransaction, true);
            writeDataPropertyAsObject(owlNamedIndividual, dataProperties.get(), temporalObjects.get().get(0), dTemporal);
//                ontology.returnAndCommitTransaction(threadTransaction);
        }
//        });
//        final CompletableFuture<Void> objectFutures = CompletableFuture.allOf(propertiesFutures);
//        try {
//            objectFutures.get();
//        } catch (InterruptedException e) {
//            logger.error("Object futures interrupted", e);
//        } catch (ExecutionException e) {
//            logger.error("Object futures exception", e);
//        }

//        Write the object properties
        ontology.returnAndCommitTransaction(trestleTransaction);
//        final CompletableFuture<Void> voidCompletableFuture = writeObjectAsync(inputObject, scope, databaseTemporal);
//        try {
//            voidCompletableFuture.get();
//        } catch (InterruptedException e) {
//            logger.error("Object write interrupted", e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
////            logger.error("Object write excepted", e);
//        }
    }

    public void writeFactWithRelation(Object inputFact, double relation, Object relatedFact) {

//        Check to see if both objects exist
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputFact);
        final OWLNamedIndividual relatedFactIndividual = ClassParser.GetIndividual(relatedFact);
        if (!ontology.containsResource(owlNamedIndividual)) {
            logger.debug("Fact {] doesn't exist, adding", owlNamedIndividual);
            try {
                writeObjectAsFact(inputFact);
            } catch (TrestleClassException e) {
                logger.error("Could not write object {}", owlNamedIndividual, e);
            } catch (MissingOntologyEntity e) {
                logger.error("Missing individual {}", e.getIndividual(), e);
            }
        } else {
            logger.debug("Fact {} already exists.", owlNamedIndividual);
        }

        if (!ontology.containsResource(relatedFactIndividual)) {
            logger.debug("Related Fact {} doesn't exist, adding", relatedFactIndividual);
            try {
                writeObjectAsFact(relatedFact);
            } catch (TrestleClassException e) {
                logger.error("Could not write object {}", relatedFactIndividual, e);
            } catch (MissingOntologyEntity e) {
                logger.error("Missing individual {}", e.getIndividual(), e);
            }
        } else {
            logger.debug("Fact {} exists", relatedFactIndividual);
        }

//        See if they're already related
        if (checkObjectRelation(owlNamedIndividual, relatedFactIndividual)) {
//            If they are, move on. We don't support updates, yet.
            logger.info("{} and {} are already related, skipping.", owlNamedIndividual, relatedFactIndividual);
            return;
        }

//        If not, store them.
//        Write the concept relation
        final IRI conceptIRI = IRI.create(PREFIX,
                String.format("related-%s-%s",
                        owlNamedIndividual.getIRI().getShortForm(),
                        relatedFactIndividual.getIRI().getShortForm()));
        ontology.createIndividual(conceptIRI, conceptRelationIRI);

//        Write the relation strength
        try {
            ontology.writeIndividualDataProperty(conceptIRI, relationStrengthIRI, Double.toString(relation), OWL2Datatype.XSD_DOUBLE.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Cannot write property {} in individual {}", relationStrengthIRI, conceptIRI, e);
        }

//        Write the relationOf property
        try {
            ontology.writeIndividualObjectProperty(conceptIRI, relationOfIRI, owlNamedIndividual.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", conceptIRI, e);
        }

//        Write the hasRelation property
        try {
            ontology.writeIndividualObjectProperty(conceptIRI, hasRelationIRI, relatedFactIndividual.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", conceptIRI, e);
        }
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
        final OWLClass datasetClass = df.getOWLClass(parseStringToIRI(datasetClassID));
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

        final IRI individualIRI = parseStringToIRI(objectID);
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
                    final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, clazz);
                    final Object literalValue = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
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
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);
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
            final OWLClass owlClass = ClassParser.GetObjectClass(clazz);

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
                logger.error("Database {] doesn't support spatial intersections.", spatialDalect, e);
                return Optional.empty();
            }

            logger.debug("Executing spatial query");
            final Instant start = Instant.now();
            final ResultSet resultSet = ontology.executeSPARQL(spatialIntersection);
            final Instant end = Instant.now();
            logger.debug("Spatial query returned in {} ms", Duration.between(start, end).toMillis());
//            I think I need to rewind the result set
            ((ResultSetMem) resultSet).rewind();
            Set<IRI> intersectedIRIs = new HashSet<>();
            while (resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                final Resource resource = querySolution.get("m").asResource();
                intersectedIRIs.add(IRI.create(resource.getURI()));
            }
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
    public <T> Optional<Map<@NonNull T, Double>> getRelatedObjects(Class<@NonNull T> clazz, String objectID, double cutoff) {


        final OWLClass owlClass = ClassParser.GetObjectClass(clazz);

        final String relationQuery = qb.buildRelationQuery(df.getOWLNamedIndividual(IRI.create(PREFIX, objectID)), owlClass, cutoff);
        TrestleTransaction transaction = ontology.createandOpenNewTransaction(false);
        final ResultSet resultSet = ontology.executeSPARQL(relationQuery);

        Set<IRI> relatedIRIs = new HashSet<>();
        Map<@NonNull T, Double> relatedObjects = new HashMap<>();
        Map<IRI, Double> relatedObjectResults = new HashMap<>();
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final IRI relatedIRI = IRI.create(next.getResource("f").getURI());
            final double strength = next.getLiteral("s").getDouble();
            relatedObjectResults.put(relatedIRI, strength);
            logger.debug("Has related {}", relatedIRI);
        }

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
     * Remove individuals from the ontology
     *
     * @param inputObject - Individual to remove
     * @param <T>         - Type of individual to remove
     */
    public <T> void removeIndividual(@NonNull T... inputObject) {
        T[] objects = inputObject;
        final List<CompletableFuture<Void>> completableFutures = Arrays.stream(objects)
                .map(object -> CompletableFuture.supplyAsync(() -> ClassParser.GetIndividual(object)))
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
            owlClass = df.getOWLClass(parseStringToIRI(datasetClass));
        }
        final String query = qb.buildIndividualSearchQuery(individualIRI, owlClass, limit);
        List<String> individuals = new ArrayList<>();
        final ResultSet resultSet = ontology.executeSPARQL(query);
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            individuals.add(next.getResource("m").getURI());
        }
        return individuals;
    }

    /**
     * Return a TrestleIndividual with all the available attributes
     * Attempts to retrieve from the cache, if enabled
     *
     * @param individualIRI - String of individual IRI
     * @return - TrestleIndividual
     */
    public TrestleIndividual getIndividualAttributes(String individualIRI) {
        if (cachingEnabled) {
            return trestleCache.IndividualCache().get(individualIRI, iri -> getIndividualAttributes(df.getOWLNamedIndividual(parseStringToIRI(iri))));
        }
        return getIndividualAttributes(df.getOWLNamedIndividual(parseStringToIRI(individualIRI)));
    }

    /**
     * Return a TrestleIndividual with all available attributes
     *
     * @param individual - OWLNamedIndividual to retrieve attributes for
     * @return - TrestleIndividual
     */
    private TrestleIndividual getIndividualAttributes(OWLNamedIndividual individual) {

        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(individual, hasTemporalIRI);
        final OWLObjectPropertyAssertionAxiom temporalProperty = individualObjectProperty.get().stream().findFirst().orElseThrow(() -> new TrestleMissingAttributeException(individual, hasTemporalIRI));
        final Set<OWLDataPropertyAssertionAxiom> temporalDataProperties = ontology.getAllDataPropertiesForIndividual(temporalProperty.getObject().asOWLNamedIndividual());
        final Optional<TemporalObject> temporalObject = TemporalObjectBuilder.buildTemporalFromProperties(temporalDataProperties, null);
        final TrestleIndividual trestleIndividual = new TrestleIndividual(individual.toStringID(), temporalObject.orElseThrow(() -> new TrestleMissingAttributeException(individual, hasTemporalIRI)));

//                Get all the attributes
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty1 = ontology.getIndividualObjectProperty(individual, hasFactIRI);
        final List<TrestleAttribute> attributes = individualObjectProperty1
                .orElseThrow(() -> new TrestleMissingAttributeException(individual, hasFactIRI))
                .stream()
                .map(property -> buildTrestleAttribute(property.getObject().asOWLNamedIndividual()))
                .collect(Collectors.toList());
//                TODO(nrobison): Can we combine these 2 steps? Collect the attributes into the individual?
        attributes.forEach(trestleIndividual::addAttribute);

        return trestleIndividual;
    }

    /**
     * Build a TrestleAttribute from a given OWLIndividual
     * Retrieves all the asserted properties and types of a given Individual, in their native forms.
     *
     * @param attribute - OWLNamedIndividual to construct attribute from
     * @return - TrestleAttribute
     */
    private TrestleAttribute buildTrestleAttribute(OWLNamedIndividual attribute) {
        final Set<OWLDataPropertyAssertionAxiom> attributeProperties = ontology.getAllDataPropertiesForIndividual(attribute);
//        There's only one data property per attribute, so we can do this.
        final Optional<OWLDataPropertyAssertionAxiom> first = attributeProperties.stream().findFirst();

        final OWLDataPropertyAssertionAxiom attributeAssertion = first.orElseThrow(() -> new TrestleMissingAttributeException(attribute));
        final Class<?> literalClass = TypeConverter.lookupJavaClassFromOWLDatatype(attributeAssertion, null);
        final Object literal = TypeConverter.extractOWLLiteral(literalClass, attributeAssertion.getObject());

//            Now the temporals
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> temporalProperties = ontology.getIndividualObjectProperty(attribute, validTimeIRI);
        final Optional<OWLObjectPropertyAssertionAxiom> temporalProperty = temporalProperties.orElseThrow(() -> new TrestleMissingAttributeException(attribute, hasTemporalIRI)).stream().findFirst();
        final Set<OWLDataPropertyAssertionAxiom> temporalDataProperties = ontology.getAllDataPropertiesForIndividual(temporalProperty.orElseThrow(() -> new TrestleMissingAttributeException(attribute, hasTemporalIRI)).getObject().asOWLNamedIndividual());
        final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromProperties(temporalDataProperties, null);

//        Database time
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> databaseTimeProperties = ontology.getIndividualObjectProperty(attribute, databaseTimeIRI);
        final Optional<OWLObjectPropertyAssertionAxiom> databaseTimeProperty = databaseTimeProperties.orElseThrow(() -> new TrestleMissingAttributeException(attribute, hasTemporalIRI)).stream().findFirst();
        final Set<OWLDataPropertyAssertionAxiom> databaseTemporalDataProperties = ontology.getAllDataPropertiesForIndividual(databaseTimeProperty.orElseThrow(() -> new TrestleMissingAttributeException(attribute, hasTemporalIRI)).getObject().asOWLNamedIndividual());
        final Optional<TemporalObject> databaseTemporal = TemporalObjectBuilder.buildTemporalFromProperties(databaseTemporalDataProperties, null);

        return new TrestleAttribute<>(attribute.getIRI().toString(),
                attributeAssertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                literal,
                validTemporal.orElseThrow(() -> new TrestleMissingAttributeException(attribute, hasTemporalIRI)),
                databaseTemporal.orElseThrow(() -> new TrestleMissingAttributeException(attribute, databaseTimeIRI)));
    }

    public void registerClass(Class inputClass) throws TrestleClassException {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.put(ClassParser.GetObjectClass(inputClass), inputClass);
    }

    private void writeTemporalWithAssociation(TemporalObject temporal, OWLNamedIndividual individual, @Nullable TemporalScope overrideTemporalScope, @Nullable IRI overrideTemporalAssociation) throws MissingOntologyEntity {
//        Write the object
        final IRI temporalIRI = temporal.getIDAsIRI();
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

    /**
     * Get a list of currently registered datasets
     * Only returns datasets currently registered with the reasoner.
     *
     * @return - Set of Strings representing the registered datasets
     */
    public Set<String> getAvailableDatasets() {

        final String datasetQuery = qb.buildDatasetQuery();
        final ResultSet resultSet = ontology.executeSPARQL(datasetQuery);
        List<OWLClass> datasetsInOntology = new ArrayList<>();
        while (resultSet.hasNext()) {
            datasetsInOntology.add(df.getOWLClass(IRI.create(resultSet.next().getResource("dataset").getURI())));
        }

        return this.registeredClasses
                .keySet()
                .stream()
                .filter(datasetsInOntology::contains)
                .map(individual -> individual.getIRI().getShortForm())
                .collect(Collectors.toSet());
    }

    public Class<?> getDatasetClass(String owlClassString) throws UnregisteredClassException {
        final OWLClass owlClass = df.getOWLClass(parseStringToIRI(owlClassString));
        final Class<?> aClass = this.registeredClasses.get(owlClass);
        if (aClass == null) {
            throw new UnregisteredClassException(owlClass);
        }
        return aClass;
    }

    public <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException {

        //        Build shapefile schema
//        TODO(nrobison): Extract type from wkt
        final ShapefileSchema shapefileSchema = new ShapefileSchema(MultiPolygon.class);
        final Optional<List<OWLDataProperty>> propertyMembers = ClassBuilder.getPropertyMembers(inputClass, true);
        if (propertyMembers.isPresent()) {
            propertyMembers.get().forEach(property -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, property.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, property)));
        }

//        Now the temporals
        final Optional<List<OWLDataProperty>> temporalProperties = TemporalParser.GetTemporalsAsDataProperties(inputClass);
        if (temporalProperties.isPresent()) {
            temporalProperties.get().forEach(temporal -> shapefileSchema.addProperty(ClassParser.matchWithClassMember(inputClass, temporal.asOWLDataProperty().getIRI().getShortForm()), TypeConverter.lookupJavaClassFromOWLDataProperty(inputClass, temporal)));
        }


        final List<CompletableFuture<Optional<TSIndividual>>> completableFutures = objectID
                .stream()
                .map(IRIUtils::parseStringToIRI)
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

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

//        Build the shapefile exporter
        throw new RuntimeException("Problem constructing object");
    }

    private static <T> Optional<TSIndividual> parseIndividualToShapefile(T object, ShapefileSchema shapefileSchema) {
//        if (objectOptional.isPresent()) {
//            final T object = objectOptional.get();
        final Class<?> inputClass = object.getClass();
        final Optional<OWLDataPropertyAssertionAxiom> spatialProperty = ClassParser.GetSpatialProperty(object);
        if (!spatialProperty.isPresent()) {
            logger.error("Individual is not a spatial object");
            return Optional.empty();
        }
        final TSIndividual individual = new TSIndividual(spatialProperty.get().getObject().getLiteral(), shapefileSchema);
//                    Data properties, filtering out the spatial members
        final Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(object, true);
        if (owlDataPropertyAssertionAxioms.isPresent()) {

            owlDataPropertyAssertionAxioms.get().forEach(property -> {
                final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, object.getClass());
                final Object literal = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
                individual.addProperty(ClassParser.matchWithClassMember(inputClass, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                        literal);
            });
        }
//                    Temporals
        final Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(object);
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
