package com.nickrobison.trestle;

import com.nickrobison.trestle.caching.TrestleCache;
import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.exceptions.*;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.exporter.ShapefileExporter;
import com.nickrobison.trestle.exporter.ShapefileSchema;
import com.nickrobison.trestle.exporter.TSIndividual;
import com.nickrobison.trestle.ontology.*;
import com.nickrobison.trestle.parser.*;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
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
import java.sql.SQLException;
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
            if (ontology instanceof OracleOntology) {
                try {
                    logger.info("Updating Oracle inference model");
                    ((OracleOntology) ontology).runInference();
                } catch (SQLException e) {
                    logger.error("Could not update inference model", e);
                }
            }
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
        qb = new QueryBuilder(ontology.getUnderlyingPrefixManager());
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
     * @param inputObject
     * @throws TrestleClassException
     */
    public void writeObjectAsConcept(Object inputObject) throws TrestleClassException {

        writeObject(inputObject, TemporalScope.EXISTS);
    }

    /**
     * Write a java object as an TS_Fact
     *
     * @param inputObject
     * @throws TrestleClassException
     */
    public void writeObjectAsFact(Object inputObject) throws TrestleClassException {
        writeObject(inputObject, TemporalScope.VALID);
    }

    private CompletableFuture<Void> writeObjectAsync(Object inputObject, TemporalScope scope) {

        return CompletableFuture.runAsync(() -> {

            //        Is class in registry?
            final Class aClass = inputObject.getClass();
            try {
                checkRegisteredClass(aClass);
            } catch (UnregisteredClassException e) {
                throw new CompletionException(e);
            }

            final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);

//        Write the class
            final OWLClass owlClass = ClassParser.GetObjectClass(inputObject);
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
                            writeTemporalWithAssociation(temporal, owlNamedIndividual, scope);
                        } catch (MissingOntologyEntity e) {
                            logger.error("Individual {} missing in ontology", e.getIndividual(), e);
                            throw new CompletionException(e);
                        }
                    });
                }
//            });

//        Write the data properties
            final Optional<List<OWLDataPropertyAssertionAxiom>> dataProperties = ClassParser.GetDataProperties(inputObject);
            final CompletableFuture<Void> propertiesFutures = CompletableFuture.runAsync(() -> {
                if (dataProperties.isPresent()) {
                    writeDataPropertyAsObject(owlNamedIndividual, dataProperties.get(), temporalObjects.get().get(0));
//                    dataProperties.get().stream().map(property -> writeDataPropertyAsObject(owlNamedIndividual, property, temporalObjects.get().get(0)));
//                    dataProperties.get().forEach(property -> {
//                        try {
//                            ontology.writeIndividualDataProperty(property);
//                        } catch (MissingOntologyEntity e) {
//                            logger.error("Individual {} missing in ontology", e.getIndividual(), e);
//                            throw new CompletionException(e);
//                        }
//                    });
                }
            });
            final CompletableFuture<Void> objectFutures = CompletableFuture.allOf(propertiesFutures);
            try {
                objectFutures.get();
            } catch (InterruptedException e) {
                logger.error("Object futures interrupted", e);
            } catch (ExecutionException e) {
                logger.error("Object futures exception", e);
            }

//        Write the object properties
        });
    }

    /**
     * Writes a data property as an individual object, with relations back to the root dataset individual
     * @param rootIndividual - OWLNamedIndividual of the dataset individual
     * @param properties - List of OWLDataPropertyAssertionAxioms to write as objects
     * @param temporal - Temporal to associate with data property individual
     */
    private void writeDataPropertyAsObject(OWLNamedIndividual rootIndividual, List<OWLDataPropertyAssertionAxiom> properties, TemporalObject temporal) {
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
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            }
        });
    }

    void writeObject(Object inputObject, TemporalScope scope)  {
        final CompletableFuture<Void> voidCompletableFuture = writeObjectAsync(inputObject, scope);
        try {
            voidCompletableFuture.get();
        } catch (InterruptedException e) {
            logger.error("Object write interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
//            logger.error("Object write excepted", e);
        }
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
        return readAsObject(aClass, objectID);


    }


    @SuppressWarnings({"argument.type.incompatible", "dereference.of.nullable"})
    public <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull String objectID) throws TrestleClassException, MissingOntologyEntity {

        final IRI individualIRI = parseStringToIRI(objectID);
//        Check cache first
        if (cachingEnabled) {
            logger.debug("Retrieving {} from cache", individualIRI);
            return clazz.cast(trestleCache.ObjectCache().get(individualIRI, rethrowFunction(iri -> readAsObject(clazz, individualIRI, true))));
        } else {
            logger.debug("Bypassing cache and directly retrieving object");
            return readAsObject(clazz, individualIRI, false);
        }
    }

    @SuppressWarnings({"return.type.incompatible", "argument.type.incompatible"})
    <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull IRI individualIRI, boolean bypassCache) {
        logger.debug("Reading {}", individualIRI);
//        Check for cache hit first, provided caching is enabled and we're not set to bypass the cache
        if (isCachingEnabled() & !bypassCache) {
            logger.debug("Retrieving {} from cache", individualIRI);
            return clazz.cast(trestleCache.ObjectCache().get(individualIRI, rethrowFunction(iri -> readAsObject(clazz, individualIRI, true))));
        } else {
            logger.debug("Bypassing cache and directly retrieving object");
        }

        logger.debug("Running async");
        final CompletableFuture<Optional<@NonNull T>> objectFuture = readAsObjectAsync(clazz, individualIRI);
        try {
            final Optional<@NonNull T> constructedObject = objectFuture.get();
            if (constructedObject.isPresent()) {
                logger.debug("Done with {}", individualIRI);
                return constructedObject.get();
            } else {
                throw new RuntimeException("Problem constructing object");
            }

        } catch (InterruptedException e) {
            logger.error("Interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Execution exception", e);
        }

        throw new RuntimeException("Problem constructing object");
    }

    @SuppressWarnings("Duplicates")
    private <T> CompletableFuture<Optional<T>> readAsObjectAsync(Class<@NonNull T> clazz, @NonNull IRI individualIRI) {

        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Executing async");
//        Contains class?
            try {
                checkRegisteredClass(clazz);
            } catch (UnregisteredClassException e) {
                logger.error("Unregistered class", e);
                throw new CompletionException(e);
            }

//        Figure out its name
            if (!checkExists(individualIRI)) {
                logger.error("Missing individual {}", individualIRI);
                return Optional.empty();
            }

            final ConstructorArguments constructorArguments = new ConstructorArguments();
            final Optional<List<OWLDataProperty>> dataProperties = ClassBuilder.getPropertyMembers(clazz);
            if (dataProperties.isPresent()) {


//                We need to get the properties from the fact relations
                final Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new HashSet<>();
                final Optional<Set<OWLObjectPropertyAssertionAxiom>> factIndividuals = ontology.getIndividualObjectProperty(individualIRI, hasFactIRI);
                if (factIndividuals.isPresent()) {
                    factIndividuals.get().stream()
                            .map(individual -> ontology.getAllDataPropertiesForIndividual(individual.getObject().asOWLNamedIndividual()))
                            .forEach(propertySet -> propertySet.forEach(retrievedDataProperties::add));
                }

//                final Set<OWLDataPropertyAssertionAxiom> propertiesForIndividual = ontology.getDataPropertiesForIndividual(individualIRI, dataProperties.get());
                retrievedDataProperties.forEach(property -> {
                    final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, clazz);
                    final Object literalValue = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                            javaClass,
                            literalValue);
                });
//            Get the temporals
//            Get the temporal objects to figure out the correct return type
                final Optional<List<TemporalObject>> temporalObjectTypes = TemporalParser.GetTemporalObjects(clazz);

                final Class<? extends Temporal> baseTemporalType = TemporalParser.GetTemporalType(clazz);

                final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(individualIRI, hasTemporalIRI);
                Optional<TemporalObject> temporalObject = Optional.empty();
                if (individualObjectProperty.isPresent()) {
//                There can only be 1 temporal, so just grab the first one.
                    final Optional<OWLObjectPropertyAssertionAxiom> first = individualObjectProperty.get().stream().findFirst();
                    if (!first.isPresent()) {
                        throw new RuntimeException(String.format("Missing temporal for individual %s", individualIRI));
                    }
                    final Set<OWLDataPropertyAssertionAxiom> TemporalProperties = ontology.getAllDataPropertiesForIndividual(first.get().getObject().asOWLNamedIndividual());
                    temporalObject = TemporalObjectBuilder.buildTemporalFromProperties(TemporalProperties, TemporalParser.IsDefault(clazz), baseTemporalType);
                }

                if (!temporalObject.isPresent()) {
                    throw new RuntimeException(String.format("Cannot restore temporal from ontology for %s", individualIRI));
                }

//            Add the temporal to the constructor args
                final TemporalObject temporal = temporalObject.get();
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
            }
            try {
                final @NonNull T constructedObject = ClassBuilder.ConstructObject(clazz, constructorArguments);
                return Optional.of(constructedObject);
            } catch (MissingConstructorException e) {
                logger.error("Problem with constructor", e);
                return Optional.empty();
            }
        });
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
     * @param clazz  - Class of object to return
     * @param wkt    - WKT of spatial boundary to intersect with
     * @param buffer - Double buffer to build around wkt
     * @param <T>    - Class to specialize method with.
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

    private <T> CompletableFuture<Optional<List<T>>> spatialIntersectAsync(Class<@NonNull T> clazz, String wkt, double buffer, @Nullable Temporal atTemporal) {
        return CompletableFuture.supplyAsync(() -> {
            final OWLClass owlClass = ClassParser.GetObjectClass(clazz);

            String spatialIntersection = null;
            try {
                if (atTemporal == null) {
                    logger.debug("Running generic spatial intersection");
                    spatialIntersection = qb.buildSpatialIntersection(spatialDalect, owlClass, wkt, buffer, QueryBuilder.UNITS.METER);
                } else {
                    final OffsetDateTime atLDTime = parseTemporalToOntologyDateTime(atTemporal, TemporalParser.IntervalType.START, ZoneOffset.UTC);
                    logger.debug("Running spatial intersection at time {}", atLDTime);
                    spatialIntersection = qb.buildTemporalSpatialIntersection(spatialDalect, owlClass, wkt, buffer, QueryBuilder.UNITS.METER, atLDTime);
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
     * @return - Optional Map of related java objects and their corresponding relational strength
     */
    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @SuppressWarnings("return.type.incompatible")
    public <T> Optional<Map<@NonNull T, Double>> getRelatedObjects(Class<@NonNull T> clazz, String objectID, double cutoff) {

        final OWLClass owlClass = ClassParser.GetObjectClass(clazz);

        final String relationQuery = qb.buildRelationQuery(df.getOWLNamedIndividual(IRI.create(PREFIX, objectID)), owlClass, cutoff);
        final ResultSet resultSet = ontology.executeSPARQL(relationQuery);

        Set<IRI> relatedIRIs = new HashSet<>();
        Map<@NonNull T, Double> relatedObjects = new HashMap<>();
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final IRI relatedIRI = IRI.create(next.getResource("f").getURI());
            final double strength = next.getLiteral("s").getDouble();
            logger.debug("Has related {}", relatedIRI);
            try {
                final @NonNull T object = readAsObject(clazz, relatedIRI, false);
                relatedObjects.put(object, strength);
            } catch (Exception e) {
                logger.error("Problem with {}", relatedIRI, e);
            }

        }

        if (relatedObjects.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(relatedObjects);
    }

    public void registerClass(Class inputClass) throws TrestleClassException {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.put(ClassParser.GetObjectClass(inputClass), inputClass);
    }

    private void writeTemporalWithAssociation(TemporalObject temporal, OWLNamedIndividual individual, @Nullable TemporalScope overrideTemporalScope) throws MissingOntologyEntity {
//        Write the object
        final IRI temporalIRI = temporal.getIDAsIRI();
        ontology.createIndividual(temporalIRI, StaticIRI.temporalClassIRI);
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
        ontology.writeIndividualObjectProperty(
                individual.getIRI(),
                StaticIRI.hasTemporalIRI,
                temporalIRI);
    }

    /**
     * Get a list of currently registered datasets
     * Only returns datasets currently registered with the
     * @return
     */
    public Optional<Set<String>> getAvailableDatasets() {
        try {
            return Optional.of(CompletableFuture.supplyAsync(() -> this.registeredClasses
                    .keySet()
                    .stream()
                    .filter(individual -> checkExists(individual.getIRI()))
                    .map(individual ->  individual.getIRI().getShortForm())
                    .collect(Collectors.toSet()))
                    .get());
        } catch (InterruptedException e) {
            logger.error("Interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Excepted", e);
        }

        return Optional.empty();
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
//                .map(id -> readAsObjectAsync(inputClass, id))
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
