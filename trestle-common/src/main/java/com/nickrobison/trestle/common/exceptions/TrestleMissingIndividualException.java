package com.nickrobison.trestle.common.exceptions;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Created by nrobison on 9/25/16.
 */
public class TrestleMissingIndividualException extends RuntimeException {

    private final String individual;

    public TrestleMissingIndividualException(String identifier) {
        super(String.format("Cannot find individual %s", identifier));
        this.individual = identifier;
    }

    public TrestleMissingIndividualException(OWLNamedIndividual individual) {
        super(String.format("Cannot find individual %s", individual.toString()));
        this.individual = individual.toString();
    }

    public String getIndividual() {
        return this.individual;
    }
}
