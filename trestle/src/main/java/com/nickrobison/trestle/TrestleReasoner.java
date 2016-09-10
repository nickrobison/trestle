package com.nickrobison.trestle;

import com.nickrobison.trestle.caching.TrestleCache;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.exceptions.*;
import com.nickrobison.trestle.ontology.*;
import com.nickrobison.trestle.parser.*;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.isFullIRI;
import static com.nickrobison.trestle.common.LambdaExceptionUtil.rethrowFunction;
import static com.nickrobison.trestle.common.StaticIRI.*;

/**
 * Created by nrobison on 5/17/16.
 */
@SuppressWarnings({"methodref.inference.unimplemented"})
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);
    public static final String DEFAULTNAME = "trestle";

    private final ITrestleOntology ontology;
    private final Set<Class> registeredClasses = new HashSet<>();
    private final OWLDataFactory df;
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(IRI.create("trestle:", "Dataset"));
    private final QueryBuilder qb;
    private final QueryBuilder.DIALECT spatialDalect;
    private boolean cachingEnabled = true;
    private @Nullable TrestleCache trestleCache = null;

    @SuppressWarnings("dereference.of.nullable")
    TrestleReasoner(TrestleBuilder builder) throws OWLOntologyCreationException {

        df = OWLManager.getOWLDataFactory();

        final URL ontologyResource = TrestleReasoner.class.getClassLoader().getResource("trestle.owl");
        final InputStream ontologyIS = TrestleReasoner.class.getClassLoader().getResourceAsStream("trestle.owl");

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
                this.registeredClasses.add(clazz);
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
        try {
            ontology.writeOntology(IRI.create(filePath), validate);
        } catch (OWLOntologyStorageException e) {
            logger.error("Could not write ontology to {}", filePath, e);
        }
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

    public CompletableFuture<Void> writeObjectAsync(Object inputObject, TemporalScope scope) {

        return CompletableFuture.runAsync(() -> {
//            ontology.openAndLock(true);
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
            final CompletableFuture<Void> temporalFutures = CompletableFuture.runAsync(() -> {
                final Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(inputObject);
                if (temporalObjects.isPresent()) {
                    temporalObjects.get().forEach(temporal -> {
                        try {
                            writeTemporalWithAssociation(temporal, owlNamedIndividual, scope);
                        } catch (MissingOntologyEntity e) {
                            logger.error("Individual {} missing in ontology", owlNamedIndividual, e);
                            throw new CompletionException(e);
                        }
                    });
                }
            });

//        Write the data properties
            final Optional<List<OWLDataPropertyAssertionAxiom>> dataProperties = ClassParser.GetDataProperties(inputObject);
            final CompletableFuture<Void> propertiesFutures = CompletableFuture.runAsync(() -> {
                if (dataProperties.isPresent()) {
                    dataProperties.get().forEach(property -> {
                        try {
                            ontology.writeIndividualDataProperty(property);
                        } catch (MissingOntologyEntity e) {
                            logger.error("Individual {} missing in ontology", property.getSubject(), e);
                            throw new CompletionException(e);
                        }
                    });
                }
            });
            final CompletableFuture<Void> objectFutures = CompletableFuture.allOf(propertiesFutures, temporalFutures);
            try {
                objectFutures.get();
            } catch (InterruptedException e) {
                logger.error("Object futures interrupted", e);
            } catch (ExecutionException e) {
                logger.error("Object futures exception", e);
            }

//        Write the object properties
//            ontology.unlockAndCommit();
        });
    }

    void writeObject(Object inputObject, TemporalScope scope) throws TrestleClassException {
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
        ontology.openAndLock(true);
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
            ontology.unlockAndCommit();
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
        this.ontology.unlockAndCommit();
    }

    @SuppressWarnings({"argument.type.incompatible", "dereference.of.nullable"})
    public <T> @NonNull T readAsObject(Class<@NonNull T> clazz, @NonNull String objectID) throws TrestleClassException, MissingOntologyEntity {

        final @NonNull IRI individualIRI;
//        Check to see if the objectID is an expanded IRI
        if (isFullIRI(objectID)) {
            individualIRI = IRI.create(objectID);
        } else {
            individualIRI = IRI.create(PREFIX, objectID.replaceAll("\\s+", "_"));
        }
//        Check cache first
        if (cachingEnabled) {
            logger.debug("Retrieving {} from cache", individualIRI);
            return clazz.cast(trestleCache.ObjectCache().get(individualIRI, rethrowFunction(iri -> readAsObject(clazz, individualIRI, true))));
        } else {
            logger.debug("Bypassing cache and directly retrieving object");
            return readAsObject(clazz, individualIRI, false);
        }
    }

    private void checkRegisteredClass(Class<?> clazz) throws UnregisteredClassException {
        if (!this.registeredClasses.contains(clazz)) {
            throw new UnregisteredClassException(clazz);
        }
    }

    private void checkExists(IRI individualIRI) throws MissingOntologyEntity {
        if (!ontology.containsResource(individualIRI)) {
            throw new MissingOntologyEntity("Can't find individual ", individualIRI);
        }
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
            try {
                checkExists(individualIRI);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", individualIRI);
                return Optional.empty();
            }


            final ConstructorArguments constructorArguments = new ConstructorArguments();
            final Optional<List<OWLDataProperty>> dataProperties = ClassBuilder.getPropertyMembers(clazz);
            if (dataProperties.isPresent()) {
                final Set<OWLDataPropertyAssertionAxiom> propertiesForIndividual = ontology.getDataPropertiesForIndividual(individualIRI, dataProperties.get());
                propertiesForIndividual.forEach(property -> {
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
                    throw new RuntimeException("Cannot restore temporal from ontology");
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

    /**
     * Spatial Intersect Object with most recent records in the database
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param inputObject - Object to intersect
     * @param buffer - Additional buffer (in meters)
     * @param <T> - Type to specialize method
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
     * @param buffer - Additional buffer to build around object (in meters)
     * @param temporalAt - Temporal of intersecting time point
     * @param <T> - Type to specialize method
     * @return - An Optional List of Object T
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer, @Nullable Temporal temporalAt) {
        this.ontology.openAndLock(false);
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);
        final Optional<String> wktString = SpatialParser.GetSpatialValue(inputObject);

        if (wktString.isPresent()) {
            return spatialIntersect((Class<T>) inputObject.getClass(), wktString.get(), buffer, temporalAt);
        }

        logger.info("{} doesn't have a spatial component", owlNamedIndividual);
        return Optional.empty();
    }

    /**
     Find objects of a given class that intersect with a specific WKT boundary.
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param clazz - Class of object to return
     * @param wkt - WKT of spatial boundary to intersect with
     * @param buffer - Double buffer to build around wkt (in meters)
     * @param <T> - Type to specialize method
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
                    final LocalDateTime atLDTime = TemporalParser.parseTemporalToLocalDateTime(atTemporal);
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
            logger.debug("Spatial query returned in {} ms", Duration.between(start, end));
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
        this.registeredClasses.add(inputClass);
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
                        StaticIRI.temporalValidFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        temporal.getBaseTemporalTypeIRI());

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalValidToIRI,
                            toTime.get().toString(),
                            temporal.getBaseTemporalTypeIRI());
                }
            } else {
                //                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        temporal.getBaseTemporalTypeIRI());

//                Write to, if exists
                final Optional<Temporal> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalExistsToIRI,
                            toTime.get().toString(),
                            temporal.getBaseTemporalTypeIRI());
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalValidAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        temporal.getBaseTemporalTypeIRI());
            } else {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        temporal.getBaseTemporalTypeIRI());
            }
        }

//        Associate with individual
        ontology.writeIndividualObjectProperty(
                individual.getIRI(),
                StaticIRI.hasTemporalIRI,
                temporalIRI);
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

            if (isRelated.isPresent()) {
                return true;
            }
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
