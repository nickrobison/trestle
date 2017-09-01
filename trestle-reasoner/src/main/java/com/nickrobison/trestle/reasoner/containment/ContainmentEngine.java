package com.nickrobison.trestle.reasoner.containment;

import com.esri.core.geometry.SpatialReference;
import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * Approximate containment is currently defined as (area of intersection)/(area of smaller region) greater than threshold
 */
public interface ContainmentEngine {
    public enum ContainmentDirection {
        CONTAINS, WITHIN, NONE
    }

    /**
     * Compares objectA and objectB to determine if one is contained, approximately, within the other
     * A threshold value is used to specify how close the approximation must be.
     *
     * @param <T>         - Generic type parameter
     * @param objectA     - first input object
     * @param objectB     - Second input object
     * @param inputSR     - {@link SpatialReference} of objects
     * @param threshold   - threshold value which determines 'approximately contained in'
     * @return - ContainmentDirection.CONTAINS if objectA contains a percentage of objectB greater than or equal to the
     *           threshold. ContainmentDirection.WITHIN if objectB contains a percentage of objectA greater than or
     *           equal to the threshold. ContainmentDirection.NONE if neither is true.
     */
    <T extends @NonNull Object> ContainmentDirection getApproximateContainment(T objectA, T objectB, SpatialReference inputSR, double threshold);

}
