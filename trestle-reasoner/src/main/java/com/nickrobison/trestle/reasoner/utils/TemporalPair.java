package com.nickrobison.trestle.reasoner.utils;

import com.nickrobison.trestle.reasoner.types.temporal.TemporalObject;

/**
 * Created by nrobison on 1/9/17.
 */

/**
 * Simple class to pair a TrestleFact's valid temporal with its database temporal
 * Used mostly to get a single return type from an async function
 */
public class TemporalPair {

    private final TemporalObject valid;
    private final TemporalObject database;

    /**
     * Default constructor for TemporalPair
     * @param valid - ValidTemporal
     * @param database - DatabaseTemporal
     */
    public TemporalPair(TemporalObject valid, TemporalObject database) {
        this.valid = valid;
        this.database = database;
    }

    public TemporalObject getValid() {
        return valid;
    }

    public TemporalObject getDatabase() {
        return database;
    }
}
