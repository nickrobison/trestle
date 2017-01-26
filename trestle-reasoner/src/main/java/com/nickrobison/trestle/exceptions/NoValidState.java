package com.nickrobison.trestle.exceptions;

import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;

/**
 * Created by nrobison on 1/26/17.
 */
public class NoValidState extends RuntimeException {

    private final IRI individualIRI
    private final OffsetDateTime validTemporal;
    private final OffsetDateTime dbTemporal;

    public NoValidState(IRI individual, OffsetDateTime validTemporal, OffsetDateTime dbTemporal) {
        super(String.format("No valid state found for %s at valid: %s and db %s", individual.getIRIString(), validTemporal, dbTemporal));
        this.individualIRI = individual;
        this.validTemporal = validTemporal;
        this.dbTemporal = dbTemporal;
    }

    public IRI getIndividual() {
        return this.individualIRI;
    }

    public OffsetDateTime getValidTemporal() {
        return  this.validTemporal;
    }

    public OffsetDateTime getDBTemporal() {
        return this.dbTemporal;
    }
}
