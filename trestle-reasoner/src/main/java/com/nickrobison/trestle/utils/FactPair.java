package com.nickrobison.trestle.utils;

import com.nickrobison.trestle.parser.TypeConverter;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Created by nrobison on 1/9/17.
 */

/**
 * Simple class that pairs an OWLDataPropertyAssertionAxiom with its OWLLiteral mate
 * Used mostly to get a single return type from an async function
 */
public class FactPair {

    private final OWLDataPropertyAssertionAxiom assertion;
    private final OWLLiteral literal;

    public FactPair(OWLDataPropertyAssertionAxiom assertion, OWLLiteral literal) {
        this.assertion = assertion;
        this.literal = literal;
    }

    public OWLDataPropertyAssertionAxiom getAssertion() {
        return this.assertion;
    }

    public OWLLiteral getLiteral() {
        return literal;
    }

    /**
     * Extract Java object from OWLLiteral
     * @return - Java object
     */
    public Object extractLiteral() {
        final Class<?> datatype = TypeConverter.lookupJavaClassFromOWLDatatype(this.assertion, null);
        return TypeConverter.extractOWLLiteral(datatype, this.literal);
    }
}
