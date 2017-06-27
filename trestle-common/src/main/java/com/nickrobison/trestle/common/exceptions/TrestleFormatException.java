package com.nickrobison.trestle.common.exceptions;

/**
 * Created by nrobison on 6/27/17.
 */

/**
 * Exception type thrown when an input cannot be formatted to the given type
 */
public class TrestleFormatException extends RuntimeException {
    public TrestleFormatException(String message) {
        super(message);
    }
}
