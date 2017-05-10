package com.nickrobison.trestle.iri.exceptions;

import org.semanticweb.owlapi.model.IRI;

/**
 * Created by nrobison on 1/23/17.
 */
public class IRIParseException extends RuntimeException {

    public IRIParseException(IRI iri) {
        super(String.format("Unable to parse IRI %s", iri.getShortForm()));
    }

    public IRIParseException(String fragment) {
        super(String.format("Unable to parse IRI fragment: %s", fragment));
    }
}
