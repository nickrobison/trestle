package com.nickrobison.trestle.reasoner.engines.spatial;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLClass;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

/**
 * Created by nickrobison on 2/14/18.
 */
public interface ITrestleSpatialEngine extends EqualityEngine, ContainmentEngine {
    /**
     * Performs a spatial intersection on a given dataset without considering any temporal constraints
     * This will return all intersecting individuals, in their latest DB state
     * Returns an optional list of {@link TrestleIndividual}s
     * This method will return the individual represented by the input WKT, so it may need to be filtered out
     *
     * @param datasetClassID - {@link String} ID of dataset {@link OWLClass}
     * @param wkt            - {@link String} WKT boundary
     * @param buffer         - {@link Double} buffer to extend around buffer. 0 is no buffer
     * @return - {@link Optional} {@link List} of {@link TrestleIndividual}
     */
    Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer);

    /**
     * Performs a spatial intersection on a given dataset with a specified spatio-temporal restriction
     * Returns an optional list of {@link TrestleIndividual}s
     * If no valid temporal is specified, performs a spatial intersection with no temporal constraints
     * This method will return the individual represented by the input WKT, so it may need to be filtered out
     *
     * @param datasetClassID - {@link String} ID of dataset {@link OWLClass}
     * @param wkt            - {@link String} WKT boundary
     * @param buffer         - {@link Double} buffer to extend around buffer. 0 is no buffer
     * @param atTemporal     - {@link Temporal} valid at restriction
     * @param dbTemporal     - {@link Temporal} database at restriction
     * @return - {@link Optional} {@link List} of {@link TrestleIndividual}
     */
    Optional<List<TrestleIndividual>> spatialIntersectIndividuals(String datasetClassID, String wkt, double buffer, @Nullable Temporal atTemporal, @Nullable Temporal dbTemporal);

    /**
     * Performs a spatial intersection on a given dataset with a specified spatio-temporal restriction
     * Returns an optional list of {@link TrestleIndividual}s
     * If no valid temporal is specified, performs a spatial intersection with no temporal constraints
     * This method will return the individual represented by the input WKT, so it may need to be filtered out
     *
     * @param clazz   - {@link Class} of dataset {@link OWLClass}
     * @param wkt     - {@link String} WKT boundary
     * @param buffer  - {@link Double} buffer to extend around buffer. 0 is no buffer
     * @param validAt - {@link Temporal} valid at restriction
     * @param dbAt    - {@link Temporal} database at restriction
     * @return - {@link Optional} {@link List} of {@link TrestleIndividual}
     */
    Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, @Nullable Temporal validAt, @Nullable Temporal dbAt);

    /**
     * Spatial Intersect Object with most recent records in the database
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param inputObject - Object to intersect
     * @param buffer      - Additional buffer (in meters)
     * @param <T>         - Type to specialize method
     * @return - An Optional List of Object T
     */
    <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer);

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
    <T extends @NonNull Object> Optional<List<T>> spatialIntersectObject(T inputObject, double buffer, @Nullable Temporal temporalAt);

    /**
     * * Spatial Intersect Object with most recent records in the database
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param clazz- Input {@link Class} of type {@link T}
     * @param wkt    - WKT {@link String} to intersect objects with
     * @param buffer - {@link Double} of buffer around WKT string
     * @param <T>    - {@link T} generic type parameter
     * @return - {@link Optional} {@link List} of intersected objects of type {@link T}
     */
    <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer);

    /**
     * * Spatial Intersect Object with most recent records in the database
     * An empty Optional means an error, an Optional of an empty List means no intersected objects
     *
     * @param clazz-  Input {@link Class} of type {@link T}
     * @param wkt     - WKT {@link String} to intersect objects with
     * @param buffer  - {@link Double} of buffer around WKT string
     * @param validAt - Optional {@link Temporal} to specify intersection time
     * @param <T>     - {@link T} generic type parameter
     * @return - {@link Optional} {@link List} of intersected objects of type {@link T}
     */
    <T extends @NonNull Object> Optional<List<T>> spatialIntersect(Class<T> clazz, String wkt, double buffer, @Nullable Temporal validAt);

    /**
     * Calculate {@link UnionEqualityResult} for the given {@link List} of individual IRIs
     *
     * @param datasetClassID - {@link String} {@link OWLClass} string reference
     * @param individualIRIs - {@link List} of Individual IRIs
     * @param inputSR        - EPSG code to determine union projection
     * @param matchThreshold - {@link Double} cutoff to determine minimum match percentage
     * @return - {@link Optional} {@link UnionEqualityResult}
     */
    Optional<UnionContributionResult> calculateSpatialUnionWithContribution(String datasetClassID, List<String> individualIRIs, int inputSR, double matchThreshold);

    /**
     * Perform spatial comparison between two input objects
     * Object relations unidirectional are A -&gt; B. e.g. contains(A,B)
     *
     * @param objectA        - {@link Object} to compare against
     * @param objectB        - {@link Object} to compare with
     * @param matchThreshold - {@link Double} cutoff for all fuzzy matches
     * @param <A> - {@link A} type of object A
     * @param <B> - {@link B} type of object B
     @return - {@link SpatialComparisonReport}
     */
    <A extends @NonNull Object, B extends @NonNull Object> SpatialComparisonReport compareTrestleObjects(A objectA, B objectB, double matchThreshold);

    /**
     * Perform spatial comparison between two input objects
     * Object relations unidirectional are A -&gt; B. e.g. contains(A,B)
     *
     * @param datasetID           - {@link String} representation of {@link OWLClass}
     * @param objectAID           - {@link String} ID of ObjectA
     * @param comparisonObjectIDs - @{link List} of {@link String} IDs of comparison objects
     * @param inputSR             - {@link SpatialReference} input spatial reference
     * @param matchThreshold      - {@link Double} cutoff for all fuzzy matches
     * @return - {@link Optional} {@link List} of {@link SpatialComparisonReport}
     */
    Optional<List<SpatialComparisonReport>> compareTrestleObjects(String datasetID, String objectAID, List<String> comparisonObjectIDs, int inputSR, double matchThreshold);
}
