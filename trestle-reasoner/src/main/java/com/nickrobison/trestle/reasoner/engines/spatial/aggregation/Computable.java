package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

/**
 * Created by nickrobison on 7/25/18.
 */
@FunctionalInterface
public interface Computable<A, B, C extends Number> {

    C compute(A nodeA, B nodeB);
}
