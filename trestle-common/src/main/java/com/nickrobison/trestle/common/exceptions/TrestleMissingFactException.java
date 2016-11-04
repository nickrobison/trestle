package com.nickrobison.trestle.common.exceptions;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleMissingFactException extends RuntimeException {

    public TrestleMissingFactException(OWLNamedIndividual individual, IRI fact) {
        super(String.format("Missing fact %s on individual %s", fact.toString(), individual.getIRI().toString()));
    }

    public TrestleMissingFactException(OWLNamedIndividual fact) {
        super(String.format("Missing fact %s", fact.toString()));
    }
}
