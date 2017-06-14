package com.nickrobison.trestle.reasoner.merge;

/**
 * Created by nrobison on 6/13/17.
 */

/**
 * Defines the strategy for merging facts and objects
 * Specifying {@link MergeStrategy#Default} will default to the strategy defined in the configuration file
 */
public enum MergeStrategy {
    /**
     * For each fact which is currently valid, if it is a continuing fact,
     * it will be versioned and a new Fact will be written with an ending temporal equal to the start of the new fact
     * If an existing fact is not continuing, {@link TrestleMergeConflict} will be thrown.
     * {@link com.nickrobison.trestle.reasoner.types.temporal.PointTemporal} are not continuing, thus they will cause an exception to be thrown
     */
    ContinuingFacts,
    ExistingFacts,
    NoMerge,
    Default
}
