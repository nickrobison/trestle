package com.nickrobison.trestle.types;

/**
 * Created by nrobison on 2/20/17.
 */

import java.time.temporal.Temporal;

/**
 * Return type of the ReadTrestleObject implementation, provides both the object as well as the minimum valid/database state it's valid for
 */
public class TrestleObjectResult<T> {
    private final T object;
    private final Temporal validFrom;
    private final Temporal validTo;
    private final Temporal dbFrom;
    private final Temporal dbTo;

    public T getObject() {
        return object;
    }

    public Temporal getValidFrom() {
        return validFrom;
    }

    public Temporal getValidTo() {
        return validTo;
    }

    public Temporal getDbFrom() {
        return dbFrom;
    }

    public Temporal getDbTo() {
        return dbTo;
    }

    public TrestleObjectResult(T object, Temporal validFrom, Temporal validTo, Temporal dbFrom, Temporal dbTo) {
        this.object = object;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.dbFrom = dbFrom;
        this.dbTo = dbTo;
    }
}
