package com.nickrobison.trestle.reasoner.engines.merge;

/**
 * Strategy for handling TrestleObject existence when merging facts.
 * Specifying {@link ExistenceStrategy#Default} will default to the strategy defined in the configuration file
 */
@SuppressWarnings({"squid:S00115"})
public enum ExistenceStrategy {
    /**
     * Ignores object existence when merging facts
     */
    Ignore,
    /**
     * Only merges facts which have a validity interval that is fully within the object's existence interval
     */
    During,
    /**
     * Extends the object's existence interval to encompass the newly merged facts.
     */
    Extend,
    /**
     * Defaults to the strategy specified in the TrestleConfig
     */
    Default
}
