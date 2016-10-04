package com.nickrobison.trestle.exceptions;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Created by nrobison on 6/30/16.
 */
public class MissingOntologyEntity extends Exception {

    private final String individual;

    public MissingOntologyEntity(String message, OWLObject owlObject) {
        super(message + owlObject.toString());
        this.individual = owlObject.toString();
    }

    public MissingOntologyEntity(String message, IRI individualIRI) {
        super(message + individualIRI.toString());
        this.individual = individualIRI.toString();
    }

    /**
     * Return the fully expanded string of the missing individual
     * @return - IRI String of individual
     */
    public String getIndividual() {
        return this.individual;
    }
}
