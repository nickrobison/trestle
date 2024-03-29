package com.nickrobison.trestle.reasoner.exceptions;

/**
 * Created by nrobison on 7/29/16.
 */
public class MissingConstructorException extends TrestleClassException {

    public MissingConstructorException(String message) {
        super(message);
    }

    public MissingConstructorException() {
        super("Can't find constructor");
    }
}
