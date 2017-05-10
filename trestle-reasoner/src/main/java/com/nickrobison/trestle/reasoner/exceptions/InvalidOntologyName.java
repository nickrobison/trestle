package com.nickrobison.trestle.reasoner.exceptions;

/**
 * Created by nrobison on 9/6/16.
 */
public class InvalidOntologyName extends Exception {

    InvalidOntologyName() {
        super("Invalid Ontology Name");
    }

    /**
     * Throws invalid ontology exception with the invalid character
     * @param invalidCharacter - Unsupported character
     */
    public InvalidOntologyName(String invalidCharacter) {
        super(String.format("Ontology name cannot contain %s", invalidCharacter));
    }

    /**
     * Exception thrown if the string is too long
     * @param length - Length of invalid string
     */
    public InvalidOntologyName(int length) {
        super(String.format("Ontology name is too long (%d)", length));
    }
}
