package com.nickrobison.trestle.reasoner;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.engines.collection.ITrestleCollectionEngine;
import com.nickrobison.trestle.reasoner.engines.exporter.ITrestleDataExporter;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.spatial.ITrestleSpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Computable;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Filterable;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.net.URI;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 1/30/17.
 */
public interface TrestleReasoner extends ITrestleObjectReader, ITrestleObjectWriter, ITrestleSpatialEngine, ITrestleCollectionEngine, ITrestleDataExporter {
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
    List<TrestleResult> executeSPARQLSelect(String queryString);

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

    @Override
    <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength);

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
     * Compute the spatial and temporal relationships between the given individual and any other individuals which intersect the individual.
     * The intersection boundary is currently set to 50 {@link si.uom.SI#METRE}.
     *
     * @param <T>        - Java class to return
     * @param clazz      - Java {@link Class} of type {@link T} to query
     * @param individual - {@link String} ID of individual
     * @param validAt    - {@link Temporal} optional temporal to specify when to compute the spatial relationships
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T> void calculateSpatialAndTemporalRelationships(Class<T> clazz, String individual, @Nullable Temporal validAt) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Build the spatial adjacency graph for a given class.
     *
     * @param clazz       - Java {@link Class} of objects to retrieve
     * @param objectID    - {@link String} ID of object to begin graph computation with
     * @param edgeCompute - {@link Computable} function to use for computing edge weights
     * @param filter      - {@link Filterable} function to use for determining whether or not to compute the given node
     * @param validAt     - {@link Temporal} optional validAt restriction
     * @param dbAt        - {@link Temporal} optional dbAt restriction
     * @param <T>         - {@link T} type parameter for object class
     * @param <B>         - {@link B} type parameter for return type from {@link Computable} function
     * @return - {@link com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine.AdjacencyGraph} spatial adjacency graph
     */
    <T extends @NonNull Object, B extends Number> AggregationEngine.AdjacencyGraph<T, B> buildSpatialGraph(Class<T> clazz, String objectID, Computable<T, T, B> edgeCompute, Filterable<T> filter, @Nullable Temporal validAt, @Nullable Temporal dbAt);

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

    /**
     * Get a sorted list of all data properties registered with the reasoner for the given {@link Class}
     *
     * @param clazz - {@link Class} to get properties for
     * @return - {@link List} of {@link String} IDs of {@link OWLDataProperty}
     * @throws IllegalStateException if unable to get properties for the given {@link Class}
     */
    List<String> getDatasetProperties(Class<?> clazz);

    /**
     * Get all the Individuals of the given dataset
     * Class must be registered with the reasoner
     *
     * @param clazz - {@link Class} Java class to retrieve members of
     * @return - {@link List} of {@link String} IDs of dataset class memberss
     */
    List<String> getDatasetMembers(Class<?> clazz);
}
