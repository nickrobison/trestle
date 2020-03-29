package com.nickrobison.trestle.types;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.time.temporal.Temporal;

/**
 * Created by nickrobison on 8/1/18.
 */

/**
 * Header of a TrestleObject. Defines the object's ID and exists temporals.
 */
public class TrestleObjectHeader implements Serializable {
    public static final long serialVersionUID = 42L;

    private final String id;
    private final Temporal existsFrom;
    private final @Nullable Temporal existsTo;

    public TrestleObjectHeader(String id, Temporal existsFrom, @Nullable Temporal existsTo) {
        this.id = id;
        this.existsFrom = existsFrom;
        this.existsTo = existsTo;
    }

    @EnsuresNonNullIf(expression = "this.existsTo", result=false)
    public boolean continuing() {
        return this.existsTo == null;
    }

    public String getId() {
        return id;
    }

    public Temporal getExistsFrom() {
        return existsFrom;
    }


    public Temporal getExistsTo() {
        return existsTo;
    }
}
