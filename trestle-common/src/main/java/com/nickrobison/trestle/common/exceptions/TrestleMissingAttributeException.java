package com.nickrobison.trestle.common.exceptions;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleMissingAttributeException extends RuntimeException {

    public TrestleMissingAttributeException(OWLNamedIndividual individual, IRI attribute) {
        super(String.format("Missing attribute %s on individual %s", attribute.toString(), individual.getIRI().toString()));
    }

    public TrestleMissingAttributeException(OWLNamedIndividual attribute) {
        super(String.format("Missing attribute %s", attribute.toString()));
    }
}
