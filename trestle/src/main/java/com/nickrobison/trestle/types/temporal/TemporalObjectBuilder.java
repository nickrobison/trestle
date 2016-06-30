package com.nickrobison.trestle.types.temporal;

/**
 * Created by nrobison on 6/30/16.
 */
public class TemporalObjectBuilder {

    private TemporalObjectBuilder() {}

    public static ValidTemporal.Builder valid() {
        return new ValidTemporal.Builder();
    }

    public static ExistsTemporal.Builder exists() {
        return new ExistsTemporal.Builder();
    }
}
