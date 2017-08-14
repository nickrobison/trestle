package com.nickrobison.trestle.reasoner.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.equality.union.UnionEqualityResult;

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
    <T> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold);
}
