package com.nickrobison.trestle.reasoner.engines.events;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

public class TrestleEventException extends RuntimeException {

    private final OWLNamedIndividual subject;

    public TrestleEventException(OWLNamedIndividual subject, String errorMessage) {
        super(errorMessage);
        this.subject = subject;
    }

    public OWLNamedIndividual getSubject() {
        return this.subject;
    }


}
