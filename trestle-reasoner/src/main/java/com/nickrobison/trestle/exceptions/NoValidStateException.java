package com.nickrobison.trestle.exceptions;

import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

/**
 * Created by nrobison on 1/26/17.
 */
public class NoValidStateException extends RuntimeException {

    private final IRI individualIRI;
    private final Temporal validTemporal;
    private final Temporal dbTemporal;

    public NoValidStateException(IRI individual, Temporal validTemporal, Temporal dbTemporal) {
        super(String.format("No valid state found for %s at valid: %s and db %s", individual.getIRIString(), validTemporal, dbTemporal));
        this.individualIRI = individual;
        this.validTemporal = validTemporal;
        this.dbTemporal = dbTemporal;
    }

    public IRI getIndividual() {
        return this.individualIRI;
    }

    public Temporal getValidTemporal() {
        return  this.validTemporal;
    }

    public Temporal getDBTemporal() {
        return this.dbTemporal;
    }
}
