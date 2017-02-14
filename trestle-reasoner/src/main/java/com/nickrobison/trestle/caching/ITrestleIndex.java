package com.nickrobison.trestle.caching;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 2/13/17.
 */
public interface ITrestleIndex<Value> {
    /**
     * Insert a key/value pair with an open interval
     *
     * @param objectID  - String object key
     * @param startTime - Long temporal of start temporal
     * @param value     - Value
     */
    void insertValue(String objectID, long startTime, Value value);

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void insertValue(String objectID, long startTime, long endTime, Value value);

    @SuppressWarnings("Duplicates")
    @Nullable Value getValue(String objectID, long atTime);

    void deleteValue(String objectID, long atTime);

    void updateValue(String objectID, long atTime, Value value);

    void replaceKeyValue(String objectID, long atTime, long startTime, long endTime, Value value);

    void setKeyTemporals(String objectID, long atTime, long startTime);

    void setKeyTemporals(String objectID, long atTime, long startTime, long endTime);
}
