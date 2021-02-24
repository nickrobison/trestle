package com.nickrobison.trestle.reasoner.engines.spatial.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import io.reactivex.rxjava3.core.Flowable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

public interface EqualityEngine {
    /**
     * Determine if a spatial union exists between any combination of the provided objects, which exceeds the given confidence threshold
     *
     * @param <T>            - generic type parameter
     * @param inputObjects   - {@link List} of input objects to process
     * @param inputSRID        - {@link SpatialReference} of objects
     * @param matchThreshold - {@link Double} confidence threshold to filter results on
     * @return - {@link Optional} {@link UnionEqualityResult} if a Spatial Union exists within the object set, above the given threshold
     */
    <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, int inputSRID, double matchThreshold);

    /**
     * Calculate the object contributions of the members of the given {@link UnionEqualityResult}
     *
     * @param <T> - generic type parameter
     * @param result - {@link UnionEqualityResult}
     * @param inputSRID - {@link SpatialReference} of objects
     * @return - {@link UnionContributionResult}
     */
    <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, int inputSRID);

    /**
     * Determines if two objects are approximately equal, in spatial area, to each other, given a threshold value.
     *
     * @param <A>         - Generic type parameter of input object
     * @param <B>         - Generic type parameter of match object
     * @param inputObject - Input object
     * @param matchObject - Object to match against
     * @param threshold   - threshold value which determines 'approximately equal'  @return - {@link boolean} {@code true} objects are approximately equal. {@code false} they are not.
     * @return - Whether or not the spatial equality of the objects exceeds the given threshold
     */
    <A extends @NonNull Object, B extends @NonNull Object> boolean isApproximatelyEqual(A inputObject, B matchObject, double threshold);

    /**
     * Calculate Spatial Equality between the two objects
     * @param inputObject - {@link Object} to match against
     * @param matchObject - {@link Object} to match
     * @param <A> - Type parameter of input object
     * @param <B> - Type parameter of match object
     * @return - {@link Double} percent spatial equality between the objects
     */
    <A extends @NonNull Object, B extends @NonNull Object> double calculateSpatialEquals(A inputObject, B matchObject);

    /**
     * Return a {@link List} of {@link OWLNamedIndividual} that are equivalent to the given individual at the specific point in time
     * If no objects satisfy the equality constraint, and empty {@link List} is returned
     *
     * @param <T>           - Generic type parameter
     * @param clazz         - {@link Class} of generic type
     * @param individual    - {@link OWLNamedIndividual} individual to determine equality for
     * @param queryTemporal - {@link Temporal} point in time to determine equality
     * @return - {@link List} of {@link OWLNamedIndividual}
     */
    <T extends @NonNull Object> Flowable<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, OWLNamedIndividual individual, Temporal queryTemporal);

    /**
     * Return a {@link List} of {@link OWLNamedIndividual} that are equivalent to the given {@link List} of individuals at the specific point in time
     * If no objects satisfy the equality constraint, and empty {@link List} is returned
     *
     * @param <T>           - Generic type parameter
     * @param clazz         - {@link Class} of generic type
     * @param individual    - {@link List} of {@link OWLNamedIndividual} individuals to determine equality for
     * @param queryTemporal - {@link Temporal} point in time to determine equality
     * @param transaction
     * @return - {@link List} of {@link OWLNamedIndividual}
     */
    <T extends @NonNull Object> Flowable<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, List<OWLNamedIndividual> individual, Temporal queryTemporal, @Nullable TrestleTransaction transaction);
}
