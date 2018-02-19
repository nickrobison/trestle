package com.nickrobison.trestle.reasoner;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.concept.ITrestleConceptEngine;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.spatial.ITrestleSpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalEngine;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.TypeConstructor;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ConceptRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 1/30/17.
 */
public interface TrestleReasoner extends ITrestleObjectReader, ITrestleObjectWriter, ITrestleSpatialEngine, ITrestleConceptEngine {
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
     * * Register custom constructor function for a given java class/OWLDataType intersection
     * Note: It's advisable to use the {@link java.util.ServiceLoader} functionality instead of manually registering constructors
     *
     * @param typeConstructor - {@link TypeConstructor} to register with reasoner
     * @param <C>             - generic type parameter
     */
    <C extends TypeConstructor> void registerTypeConstructor(C typeConstructor);

    //    When you get the ontology, the ownership passes away, so then the reasoner can't perform any more queries.
    ITrestleOntology getUnderlyingOntology();

    /**
     * Get the underlying metrics engine
     *
     * @return - {@link Metrician} metrics engine
     */
    Metrician getMetricsEngine();

    /**
     * Get the underlying {@link TrestleMergeEngine}
     *
     * @return - Get {@link TrestleMergeEngine}
     */
    TrestleMergeEngine getMergeEngine();

    /**
     * Get the underlying {@link EqualityEngine}
     *
     * @return - {@link EqualityEngine}
     */
    EqualityEngine getEqualityEngine();

    /**
     * Get underlying {@link SpatialEngine}
     *
     * @return - {@link SpatialEngine}
     * @deprecated - As of 0.8.1 all applicable methods can be called directly from the {@link TrestleReasoner} interface
     */
    @Deprecated
    SpatialEngine getSpatialEngine();

    /**
     * Return the underlying {@link TemporalEngine}
     *
     * @return - {@link TemporalEngine}
     */
    TemporalEngine getTemporalEngine();

    /**
     * Get the underlying {@link ContainmentEngine}
     *
     * @return - {@link ContainmentEngine}
     */
    ContainmentEngine getContainmentEngine();

    /**
     * Get the underlying object/individual cache
     *
     * @return - {@link TrestleCache}
     */
    TrestleCache getCache();

    /**
     * Get the currently registered prefixes and URIs
     *
     * @return - {@link Map} of prefixes and their corresponding URIs
     */
    Map<String, String> getReasonerPrefixes();

    /**
     * Get the underlying parser used by the reasoner
     *
     * @return - {@link TrestleParser}
     */
    public TrestleParser getUnderlyingParser();

    /**
     * Execute SPARQL select query
     *
     * @param queryString - Query String
     * @return - {@link TrestleResultSet}
     */
    TrestleResultSet executeSPARQLSelect(String queryString);

    Set<OWLNamedIndividual> getInstances(Class inputClass);

    void writeOntology(URI filePath, boolean validate);

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz      - Class of object to get events for
     * @param individual - {@link String} ID of the individual to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, String individual);

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz      - Class of object get get event for
     * @param individual - {@link OWLNamedIndividual} to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual);

    /**
     * Add {@link TrestleEvent} to individual
     * This method cannot be used to add {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} events because those require additional information.
     * Use the {@link TrestleReasoner#addTrestleObjectSplitMerge(TrestleEventType, Object, List, double)} for those event types
     *
     * @param type          - {@link TrestleEventType} to add to individual
     * @param individual    - {@link String} ID of individual
     * @param eventTemporal - {@link Temporal} temporal to use for event
     */
    void addTrestleObjectEvent(TrestleEventType type, String individual, Temporal eventTemporal);

    /**
     * Add {@link TrestleEvent} to individual
     * This method cannot be used to add {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} events because those require additional information.
     * Use the {@link TrestleReasoner#addTrestleObjectSplitMerge(TrestleEventType, Object, List, double)} for those event types
     *
     * @param type          - {@link TrestleEventType} to add to individual
     * @param individual    - {@link OWLNamedIndividual} individual to add event to
     * @param eventTemporal - {@link Temporal} temporal to use for event
     */
    void addTrestleObjectEvent(TrestleEventType type, OWLNamedIndividual individual, Temporal eventTemporal);

    /**
     * Add a SPLIT or MERGE {@link TrestleEventType} to a given {@link OWLNamedIndividual}
     * Events are oriented subject to object, so A splits_into [B,C,D] and H merged_from [E,F,G]
     * Individuals are not created if they don't already exist
     * throws {@link IllegalArgumentException} if something other than {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} is passed
     *
     * @param <T>      - Generic type parameter of Trestle Object
     * @param type     {@link TrestleEventType} to add
     * @param subject  - {@link OWLNamedIndividual} subject of Event
     * @param objects  - {@link Set} of {@link OWLNamedIndividual} that are the objects of the event
     * @param strength - {@link Double} Strength of union association
     */
    <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength);

    /**
     * Get a map of related objects and their relative strengths
     *
     * @param clazz    - Java class of object to serialize to
     * @param objectID - Object ID to retrieve related objects
     * @param cutoff   - Double of relation strength cutoff
     * @param <T>      - Type to specialize return with
     * @return - Optional Map of related java objects and their corresponding relational strength
     * @deprecated This is an old method, we don't use it anymore
     */
    //    TODO(nrobison): Get rid of this, no idea why this method throws an error when the one above does not.
    @SuppressWarnings("return.type.incompatible")
    @Deprecated
    <T extends @NonNull Object> Optional<Map<T, Double>> getRelatedObjects(Class<T> clazz, String objectID, double cutoff);

//    /**
//     * For a given individual, get all related concepts and the IRIs of all members of those concepts,
//     * that have a relation strength above the given cutoff value
//     *
//     * @param individual       - String of individual IRI to return relations for
//     * @param conceptID        - Nullable String of concept IRI to filter members of
//     * @param relationStrength - Cutoff value of minimum relation strength
//     * @return - {@link Optional} {@link Map} of String IRI representations of related concepts
//     */
//    Optional<Map<String, List<String>>> getRelatedConcepts(String individual, @Nullable String conceptID, double relationStrength);

    /**
     * Get a {@link List} of objects that are equivalent to given individual at the given time point
     * If no objects satisfy the equality constraints and an empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of input individuals
     * @param individual    - Individual {@link IRI}
     * @param queryTemporal - {@link Temporal} of query point
     * @param <T>           - Type parameter
     * @return - {@link Optional} {@link List} of {@link T} objects
     */
    <T extends @NonNull Object> Optional<List<T>> getEquivalentObjects(Class<T> clazz, IRI individual, Temporal queryTemporal);

    /**
     * Get a {@link List} of objects that are equivalent to given {@link List} of individuals at the given time point
     * If no objects satisfy the equality constraints and an empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of input individuals
     * @param individuals   - {@link List} of individual {@link IRI}
     * @param queryTemporal - {@link Temporal} of query point
     * @param <T>           - Type parameter
     * @return - {@link Optional} {@link List} of {@link T} objects
     */
    <T extends @NonNull Object> Optional<List<T>> getEquivalentObjects(Class<T> clazz, List<IRI> individuals, Temporal queryTemporal);

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
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - {@link TrestleIndividual}
     */
    TrestleIndividual getTrestleIndividual(String individualIRI);

//    /**
//     * Return a set of Trestle_Concepts that intersect with the given WKT
//     * The temporal parameters allow for additional specificity on the spatio-temporal intersection
//     *
//     * @param wkt      - String of WKT to intersect with
//     * @param buffer   - double buffer to draw around WKT
//     * @param strength - strength parameter to filter weak associations
//     * @param validAt  - {@link Temporal} of validAt time
//     * @param dbAt     - Optional {@link Temporal} of dbAt time   @return - Optional Set of String URIs for intersected concepts
//     * @return - {@link Optional} {@link Set} of {@link String} Concept IDs
//     */
//    Optional<Set<String>> STIntersectConcept(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt);

//    /**
//     * Retrieve all members of a specified concept that match a given class
//     * If the {@code spatialIntersection} parameter occurs outside of the exists range of the target TrestleObjects, the intersection point is adjusted, in order to return a valid object
//     * If the intersection point occurs before the TrestleObject, the earliest version of that object is returned
//     * If the intersection point occurs after the TrestleObject, the latest version of the object is returned
//     *
//     * @param <T>                  - Generic type T of returned object
//     * @param clazz                - Input class to retrieve from concept
//     * @param conceptID            - String ID of concept to retrieve
//     * @param strength             - Strength parameter to filter weak associations
//     * @param spatialIntersection  - Optional spatial intersection to restrict results
//     * @param temporalIntersection - Optional temporal intersection to restrict results   @return - Optional Set of T objects
//     * @return - {@link Optional} {@link List} of Objects
//     */
//    <T> Optional<List<T>> getConceptMembers(Class<T> clazz, String conceptID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection);

//    /**
//     * Write an object into the database, as a member of a given concept
//     *
//     * @param conceptIRI   - String of
//     * @param inputObject  - Object to write into databse
//     * @param relationType - ConceptRelationType
//     * @param strength     - Strength parameter of relation
//     */
//    void addObjectToConcept(String conceptIRI, Object inputObject, ConceptRelationType relationType, double strength);

//    /**
//     * Write a relationship between two objects.
//     * If one or both of those objects do not exist, create them.
//     *
//     * @param subject  - Java object to write as subject of relationship
//     * @param object   - Java object to write as object of relationship
//     * @param relation - ObjectRelation between the two object
//     */
//    void writeObjectRelationship(Object subject, Object object, ObjectRelation relation);

//    /**
//     * Create a spatial overlap association between two objects.
//     * If one or both of the object do not exist, create them.
//     *
//     * @param subject - Java object to write as subject of relationship
//     * @param object  - Java object to write as object of relationship
//     * @param wkt     - String of wkt boundary of spatial overlap
//     */
//    void writeSpatialOverlap(Object subject, Object object, String wkt);

//    /**
//     * Create a spatial overlap association between two objects.
//     * If one or both of the object do not exist, create them.
//     *
//     * @param subject         - Java object to write as subject of relationship
//     * @param object          - Java object to write as object of relationship
//     * @param temporalOverlap - String of temporal overlap between two objects (Not implemented yet)
//     */
//    void writeTemporalOverlap(Object subject, Object object, String temporalOverlap);

    /**
     * Register dataset class with Reasoner
     *
     * @param inputClass - {@link Class} class to parse and register
     * @throws TrestleClassException - throws if class definition is ¬invalid
     */
    void registerClass(Class inputClass) throws TrestleClassException;

    /**
     * Remove class from registry
     * This will no longer allow read/write access to the class and will throw a {@link UnregisteredClassException} on future access
     *
     * @param inputClass - {@link Class} to deregister
     */
    void deregisterClass(Class inputClass);

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
     *
     * @param inputClass - Class to parse
     * @param objectID   - {@link List} of objectID strings to return
     * @param validAt    - {@link Temporal} of validAt time
     * @param databaseAt - {@link Temporal} of databaseAt time
     * @param exportType - {@link ITrestleExporter.DataType} export datatype of file
     * @param <T>        - Generic type parameter
     * @return - {@link File} of type {@link ITrestleExporter.DataType}
     * @throws IOException - Throws if it can't create the file
     */
    <T> File exportDataSetObjects(Class<T> inputClass, List<String> objectID, @Nullable Temporal validAt, @Nullable Temporal databaseAt, ITrestleExporter.DataType exportType) throws IOException;
}
