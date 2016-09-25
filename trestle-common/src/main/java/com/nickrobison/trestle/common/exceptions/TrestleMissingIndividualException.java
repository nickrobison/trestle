package com.nickrobison.trestle.common.exceptions;

/**
 * Created by nrobison on 9/25/16.
 */
public class TrestleMissingIndividualException extends Exception {

    private final String individual;

    public TrestleMissingIndividualException(String identifier) {
        super(String.format("Cannot find individual %s", identifier));
        this.individual = identifier;
    }

    public String getIndividual() {
        return this.individual;
    }
}
