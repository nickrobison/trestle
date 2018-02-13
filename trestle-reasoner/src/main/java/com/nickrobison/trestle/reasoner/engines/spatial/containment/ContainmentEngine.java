package com.nickrobison.trestle.reasoner.engines.spatial.containment;

import com.codahale.metrics.annotation.Timed;
import com.esri.core.geometry.SpatialReference;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface ContainmentEngine {
    /**
     * Compares objectA and objectB to determine if one is contained, approximately, within the other
     * A threshold value is used to specify how close the approximation must be.
     *
     * @param objectA   - first input object
     * @param objectB   - Second input object
     * @param inputSR   - {@link SpatialReference} of objects
     * @param threshold - threshold value which determines 'approximately contained in'
     * @param <T>       - Type parameter
     * @return - {@link ContainmentDirection#CONTAINS} if objectA contains a percentage of objectB greater than or equal to the
     * threshold. {@link ContainmentDirection#WITHIN} if objectB contains a percentage of objectA greater than or
     * equal to the threshold. ContainmentDirection.NONE if neither is true.
     */
    @Timed
    <T extends @NonNull Object> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold);

    public enum ContainmentDirection {
        CONTAINS, WITHIN, NONE
    }
}
