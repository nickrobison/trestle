package com.nickrobison.trestle.reasoner.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.equality.union.UnionEqualityResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

public interface EqualityEngine {
    /**
     * Determine if a spatial union exists between any combination of the provided objects, which exceeds the given confidence threshold
     *
     * @param inputObjects   - {@link List} of input objects to process
     * @param inputSR        - {@link SpatialReference} of objects
     * @param matchThreshold - {@link Double} confidence threshold to filter results on
     * @param <T>            - generic type parameter
     * @return - {@link Optional} {@link UnionEqualityResult} if a Spatial Union exists within the object set, above the given threshold
     */
    <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold);

    /**
     * Determines if two objects are approximately equal, in spatial area, to each other, given a threshold value.
     *
     * @param <T>         - Generic type parameter
     * @param inputObject - Input object
     * @param matchObject - Object to match against
     * @param inputSR     - {@link SpatialReference} of objects
     * @param threshold   - threshold value which determines 'approximately equal'  @return - {@link boolean} {@code true} objects are approximately equal. {@code false} they are not.
     * @return - Whether or not the spatial equality of the objects exceeds the given threshold
     */
    <T extends @NonNull Object> boolean isApproximatelyEqual(T inputObject, T matchObject, SpatialReference inputSR, double threshold);

    /**
     * Return a {@link List} of {@link OWLNamedIndividual} that are equivalent to the given individual at the specific point in time
     * If no objects satisfy the equality constraint, and empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of generic type
     * @param individual    - {@link OWLNamedIndividual} individual to determine equality for
     * @param queryTemporal - {@link Temporal} point in time to determine equality
     * @param <T>           - Generic type parameter
     * @return - {@link List} of {@link OWLNamedIndividual}
     */
    <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, OWLNamedIndividual individual, Temporal queryTemporal);

    /**
     * Return a {@link List} of {@link OWLNamedIndividual} that are equivalent to the given {@link List} of individuals at the specific point in time
     * If no objects satisfy the equality constraint, and empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of generic type
     * @param individual    - {@link List} of {@link OWLNamedIndividual} individuals to determine equality for
     * @param queryTemporal - {@link Temporal} point in time to determine equality
     * @param <T>           - Generic type parameter
     * @return - {@link List} of {@link OWLNamedIndividual}
     */
    <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, List<OWLNamedIndividual> individual, Temporal queryTemporal);
}
