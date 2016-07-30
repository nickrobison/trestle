package com.nickrobison.trestle.exceptions;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Created by nrobison on 6/30/16.
 */
public class MissingOntologyEntity extends Exception {

    public MissingOntologyEntity(String message, OWLObject owlObject) {
        super(message + owlObject.toString());
    }

    public MissingOntologyEntity(String message, IRI individualIRI) {
        super(message + individualIRI.toString());
    }
}
