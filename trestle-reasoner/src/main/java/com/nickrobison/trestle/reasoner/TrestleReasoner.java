package com.nickrobison.trestle.reasoner;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeEngine;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ConceptRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by nrobison on 1/30/17.
 */
public interface TrestleReasoner {
    /**
     * Shutdown the reasoner
     */
    void shutdown();

    /**
     * Shutdown the ontology and potentially delete
     *
     * @param delete - delete the ontology on shutdown?
     */
    void shutdown(boolean delete);

    /**
     * Register custom constructor function for a given java class/OWLDataType intersection
     *
     * @param clazz           - Java class to construct
     * @param datatype        - OWLDatatype to match with Java class
     * @param constructorFunc - Function lambda function to take OWLLiteral and generate given java class
     */
    void registerTypeConstructor(Class<?> clazz, OWLDatatype datatype, Function constructorFunc);

    //    When you get the ontology, the ownership passes away, so then the reasoner can't perform any more queries.
    ITrestleOntology getUnderlyingOntology();

    /**
     * Get the underlying metrics engine
     * @return - {@link Metrician} metrics engine
     */
    Metrician getMetricsEngine();

    /**
     * Get the underlying {@link TrestleMergeEngine}
     * @return - Get {@link TrestleMergeEngine}
     */
    TrestleMergeEngine getMergeEngine();

    /**
     * Get the currently registered prefixes and URIs
     * @return - {@link Map} of prefixes and their corresponding URIs
     */
    Map<String, String> getReasonerPrefixes();

    /**
     * Execute SPARQL select query
     * @param queryString - Query String
     * @return - {@link TrestleResultSet}
     */
    TrestleResultSet executeSPARQLSelect(String queryString);

    Set<OWLNamedIndividual> getInstances(Class inputClass);

    void writeOntology(URI filePath, boolean validate);

    /**
     * Write a Java object as a Trestle_Object
     *
     * @param inputObject - Input object to write as fact
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     * @throws MissingOntologyEntity - Throws if the individual doesn't exist in the ontology
     */
    void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Write a Java object as a Trestle_Object
     * Use the provided temporals to setup the database time
     *
     * @param inputObject   - Object to write into the ontology
     * @param startTemporal - Start {@link Temporal} of database time interval
     * @param endTemporal   - Nullable {@link Temporal} of ending interval time
     * @throws MissingOntologyEntity      - Throws if the individual doesn't exist in the ontology
     * @throws UnregisteredClassException - Throws if the object class isn't registered with the reasoner
     */
    @SuppressWarnings("unchecked")
    void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException;

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity point
     *
     * @param clazz        - Java class to parse
     * @param individual   - Individual ID
     * @param factName     - Fact name
     * @param value        - Fact value
     * @param validAt      - validAt Temporal
     * @param databaseFrom - Optional databaseFrom Temporal
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom);

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity interval
     *
     * @param clazz        - Java class to parse
     * @param individual   - Individual ID
     * @param factName     - Fact name
     * @param value        - Fact value
     * @param validFrom    - validFrom Temporal
     * @param validTo      - validTo Temporal
     * @param databaseFrom - Optional databaseFrom Temporal
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom);

    /**
     * Returns an object, from the database, using the provided class definition.
     * Returns the currently valid facts, at the current database time
     *
     * @param clazz    - Java class definition of return object
     * @param objectID - IRI string of individual
     * @param <T>      - Java class to return
     * @return - Java object of type T
     * @throws TrestleClassException - exception
     * @throws MissingOntologyEntity - exception
     */
    <T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull String objectID) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Returns an object, from the database, looking up the class definition from the registry
     *
     * @param datasetClassID - String of class name to retrieve from the class registry
     * @param objectID       - IRI string of individual
     * @param <T>            - Java class to return
     * @return - Java object of type T
     * @throws MissingOntologyEntity - exception
     * @throws TrestleClassException - exception
     */
    <T> @NonNull T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Returns an object, from the database, using the provided class definition.
     * Allows the user to specify a valid/database pair to specified desired object state
     *
     * @param clazz            - Java class definition of return object
     * @param objectID         - IRI string of individual
     * @param validTemporal    - Temporal to denote the ValidAt point
     * @param databaseTemporal - Optional Temporal to denote the DatabaseAt point
     * @param <T>              - Java class to return
     * @return - Java object of type T
     * @throws TrestleClassException - exception
     * @throws MissingOntologyEntity - exception
     */
    @SuppressWarnings({"argument.type.incompatible", "dereference.of.nullable"})
    <T> @NonNull T readTrestleObject(Class<@NonNull T> clazz, @NonNull String objectID, Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Returns an object, from the database, looking up the class definition from the registry
     *
     * @param <T>              - Java class to return
     * @param datasetClassID   - String of class name to retrieve from the class registry
     * @param objectID         - IRI string of individual
     * @param validTemporal    - Temporal to denote the ValidAt point
     * @param databaseTemporal - Optional Temporal to denote the DatabaseAt point
     * @return - Java object of type T
     * @throws MissingOntologyEntity - exception
     * @throws TrestleClassException - exception
     */
    @SuppressWarnings("unchecked")
    <T> @NonNull T readTrestleObject(String datasetClassID, String objectID, Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java class to parse
     * @param individual       - Individual ID
     * @param factName         - Name of Fact
     * @param validStart       - Optional Temporal setting the start of the temporal filter
     * @param validEnd         - Optional Temporal setting the end of the temporal filter
     * @param databaseTemporal - Optional temporal filtering results to only certain fact versions
     * @return - Optional list of Java Objects
     */
    Optional<List<Object>> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java class to parse
     * @param individual       - {@link OWLNamedIndividual} of individual ID
     * @param factName         - {@link OWLDataProperty}
     * @param validStart       - Optional Temporal setting the start of the temporal filter
     * @param validEnd         - Optional Temporal setting the end of the temporal filter
     * @param databaseTemporal - Optional temporal filtering results to only certain fact versions
     * @return - Optional List of Java Objects
     */
    Optional<List<Object>> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     *
     * @param clazz
     * @param individual - {@link String} ID of the individual to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, String individual);

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     *
     * @param clazz
     * @param individual - {@link OWLNamedIndividual} to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual);

    /**
     * Add {@link TrestleEvent} to individual
     * This method cannot be used to add {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} events because those require additional information.
     * Use the {@link TrestleReasoner#addTrestleObjectSplitMerge(TrestleEventType, Object, List)} for those event types
     *
     * @param type - {@link TrestleEventType} to add to individual
     * @param individual - {@link String} ID of individual
     * @param eventTemporal - {@link Temporal} temporal to use for event
     */
    void addTrestleObjectEvent(TrestleEventType type, String individual, Temporal eventTemporal);

    /**
     * Add {@link TrestleEvent} to individual
     * This method cannot be used to add {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} events because those require additional information.
     * Use the {@link TrestleReasoner#addTrestleObjectSplitMerge(TrestleEventType, Object, List)} for those event types
     *
     * @param type - {@link TrestleEventType} to add to individual
     * @param individual - {@link OWLNamedIndividual} individual to add event to
     * @param eventTemporal - {@link Temporal} temporal to use for event
     */
    void addTrestleObjectEvent(TrestleEventType type, OWLNamedIndividual individual, Temporal eventTemporal);

    /**
     * Add a SPLIT or MERGE {@link TrestleEventType} to a given {@link OWLNamedIndividual}
     * Events are oriented subject -> object, so A splits_into [B,C,D] and H merged_from [E,F,G]
     * Individuals are not created if they don't already exist
     * throws {@link IllegalArgumentException} if something other than {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} is passed
     *
     * @param type    {@link TrestleEventType} to add
     * @param subject - {@link OWLNamedIndividual} subject of Event
     * @param objects - {@link Set} of {@link OWLNamedIndividual} that are the objects of the event
     * @param <T> - Generic type parameter of Trestle Object
     */
    <@NonNull T> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects);

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
    <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer);

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
    <T> Optional<List<T>> spatialIntersectObject(@NonNull T inputObject, double buffer, @Nullable Temporal temporalAt);

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
    <T> Optional<List<T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer);

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
    <T> Optional<List<T>> spatialIntersect(Class<@NonNull T> clazz, String wkt, double buffer, @Nullable Temporal atTemporal);

    /**
     * Get a map of related objects and their relative strengths
     *
     * @deprecated This is an old method, we don't use it anymore
     * @param clazz    - Java class of object to serialize to
     * @param objectID - Object ID to retrieve related objects
     * @param cutoff   - Double of relation strength cutoff
     * @param <T>      - Type to specialize return with
     * @return - Optional Map of related java objects and their corresponding relational strength
     */
    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @SuppressWarnings("return.type.incompatible")
    @Deprecated
    <T> Optional<Map<@NonNull T, Double>> getRelatedObjects(Class<@NonNull T> clazz, String objectID, double cutoff);

    /**
     * For a given individual, get all related concepts and the IRIs of all members of those concepts,
     * that have a relation strength above the given cutoff value
     *
     * @param individual       - String of individual IRI to return relations for
     * @param conceptID        - Nullable String of concept IRI to filter members of
     * @param relationStrength - Cutoff value of minimum relation strength
     * @return - {@link Optional} {@link Map} of String IRI representations of related concepts
     */
    Optional<Map<String, List<String>>> getRelatedConcepts(String individual, @Nullable String conceptID, double relationStrength);

    /**
     * Search the ontology for individuals with IRIs that match the given search string
     *
     * @param individualIRI - String to search for matching IRI
     * @return - List of Strings representing IRIs of matching individuals
     */
    List<String> searchForIndividual(String individualIRI);

    /**
     * Search the ontology for individuals with IRIs that match the given search string
     *
     * @param individualIRI - String to search for matching IRI
     * @param datasetClass  - Optional datasetClass to restrict search to
     * @param limit         - Optional limit to returned results
     * @return - List of Strings representing IRIs of matching individuals
     */
    List<String> searchForIndividual(String individualIRI, @Nullable String datasetClass, @Nullable Integer limit);

    /**
     * Return a TrestleIndividual with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - TrestleIndividual
     */
    TrestleIndividual getTrestleIndividual(String individualIRI);

    /**
     * Return a set of Trestle_Concepts that intersect with the given WKT
     * The temporal parameters allow for additional specificity on the spatio-temporal intersection
     * @param wkt     - String of WKT to intersect with
     * @param buffer  - double buffer to draw around WKT
     * @param validAt - {@link Temporal} of validAt time
     * @param dbAt    - Optional {@link Temporal} of dbAt time
     * @return - Optional Set of String URIs for intersected concepts
     */
    Optional<Set<String>> STIntersectConcept(String wkt, double buffer, Temporal validAt, @Nullable Temporal dbAt);

    /**
     * Retrieve all members of a specified concept that match a given class
     * If the {@code spatialIntersection} parameter occurs outside of the exists range of the target TrestleObjects, the intersection point is adjusted, in order to return a valid object
     * If the intersection point occurs before the TrestleObject, the earliest version of that object is returned
     * If the intersection point occurs after the TrestleObject, the latest version of the object is returned
     * @param <T>                  - Generic type T of returned object
     * @param clazz                - Input class to retrieve from concept
     * @param conceptID            - String ID of concept to retrieve
     * @param spatialIntersection  - Optional spatial intersection to restrict results
     * @param temporalIntersection - Optional temporal intersection to restrict results
     * @return - Optional Set of T objects
     */
    <T> Optional<List<T>> getConceptMembers(Class<T> clazz, String conceptID, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection);

    /**
     * Write an object into the database, as a member of a given concept
     *
     * @param conceptIRI   - String of
     * @param inputObject  - Object to write into databse
     * @param relationType - ConceptRelationType
     * @param strength     - Strength parameter of relation
     */
    void addObjectToConcept(String conceptIRI, Object inputObject, ConceptRelationType relationType, double strength);

    /**
     * Write a relationship between two objects.
     * If one or both of those objects do not exist, create them.
     *
     * @param subject  - Java object to write as subject of relationship
     * @param object   - Java object to write as object of relationship
     * @param relation - ObjectRelation between the two object
     */
    void writeObjectRelationship(Object subject, Object object, ObjectRelation relation);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject - Java object to write as subject of relationship
     * @param object  - Java object to write as object of relationship
     * @param wkt     - String of wkt boundary of spatial overlap
     */
    void writeSpatialOverlap(Object subject, Object object, String wkt);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject         - Java object to write as subject of relationship
     * @param object          - Java object to write as object of relationship
     * @param temporalOverlap - String of temporal overlap between two objects (Not implemented yet)
     */
//    TODO(nrobison): Correctly implement this
    void writeTemporalOverlap(Object subject, Object object, String temporalOverlap);

    void registerClass(Class inputClass) throws TrestleClassException;

    /**
     * Get a list of currently registered datasets
     * Only returns datasets currently registered with the reasoner.
     *
     * @return - Set of Strings representing the registered datasets
     */
    Set<String> getAvailableDatasets();

    Class<?> getDatasetClass(String owlClassString) throws UnregisteredClassException;

    <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, ITrestleExporter.DataType exportType) throws IOException;

    /**
     * Export TrestleObject at the specified valid/database temporal
     * @param inputClass - Class to parse
     * @param objectID - {@link List} of objectID strings to return
     * @param validAt - {@link Temporal} of validAt time
     * @param databaseAt - {@link Temporal} of databaseAt time
     * @param exportType - {@link ITrestleExporter.DataType} export datatype of file
     * @param <T> - Generic type parameter
     * @return - {@link File} of type {@link ITrestleExporter.DataType}
     * @throws IOException - Throws if it can't create the file
     */
    <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException;
}
