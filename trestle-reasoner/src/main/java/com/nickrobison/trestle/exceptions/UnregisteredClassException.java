package com.nickrobison.trestle.exceptions;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * Created by nrobison on 7/27/16.
 */
public class UnregisteredClassException extends TrestleClassException {

    public UnregisteredClassException(Class missingClass) {
        super(String.format("Class %s is not registered with the reasoner", missingClass.getSimpleName()));
    }

    public UnregisteredClassException(OWLClass owlClass) {
        super(String.format("No registered Java class for OWLClass %s", owlClass.getIRI().getShortForm()));
    }
}
