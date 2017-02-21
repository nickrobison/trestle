package com.nickrobison.trestle.types;

import com.nickrobison.trestle.parser.ConstructorArguments;

import java.time.temporal.Temporal;

/**
 * Created by nrobison on 2/20/17.
 */

/**
 * Simple class that provides the minimum amount of metadata necessary to build and cache a TrestleObject
 * Defines both the constructor parameters as well as the minimum valid/database interval that that object state is valid for
 */
public class TrestleObjectState {
    private final ConstructorArguments arguments;
    private final Temporal minValidFrom;
    private final Temporal minValidTo;
    private final Temporal minDatabaseFrom;
    private final Temporal minDatabaseTo;

    public TrestleObjectState(ConstructorArguments arguments, Temporal minValidFrom, Temporal minValidTo, Temporal minDatabaseFrom, Temporal minDatabaseTo) {
        this.arguments = arguments;
        this.minValidFrom = minValidFrom;
        this.minValidTo = minValidTo;
        this.minDatabaseFrom = minDatabaseFrom;
        this.minDatabaseTo = minDatabaseTo;
    }

    public ConstructorArguments getArguments() {
        return this.arguments;
    }

    public Temporal getMinValidFrom() {
        return minValidFrom;
    }

    public Temporal getMinValidTo() {
        return minValidTo;
    }

    public Temporal getMinDatabaseFrom() {
        return minDatabaseFrom;
    }

    public Temporal getMinDatabaseTo() {
        return minDatabaseTo;
    }
}
