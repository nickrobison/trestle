package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.temporal.TemporalObject;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleAttribute<T> {

    private final String name;
    private final T value;
    private final TemporalObject validTemporal;
    private final TemporalObject databaseTemporal;

    public TrestleAttribute(String name, T value, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        this.name = name;
        this.value = value;
        this.validTemporal = validTemporal;
        this.databaseTemporal = databaseTemporal;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public TemporalObject getValidTemporal() {
        return validTemporal;
    }

    public TemporalObject getDatabaseTemporal() {
        return databaseTemporal;
    }
}
