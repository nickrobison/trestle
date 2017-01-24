package com.nickrobison.trestle.iri.exceptions;

import com.nickrobison.trestle.iri.IRIVersion;

/**
 * Created by nrobison on 1/23/17.
 */
public class IRIVersionException extends RuntimeException {

    public IRIVersionException(IRIVersion version) {
        super(String.format("IRI version: %s is not supported", version));
    }
}
