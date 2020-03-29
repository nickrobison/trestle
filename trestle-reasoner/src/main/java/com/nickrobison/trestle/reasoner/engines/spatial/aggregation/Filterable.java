package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

/**
 * Created by nickrobison on 7/25/18.
 */
@FunctionalInterface
public interface Filterable<A> {
    boolean filter(A nodeA);
}
