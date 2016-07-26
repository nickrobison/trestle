package com.nickrobison.trestle.exceptions;

/**
 * Created by nrobison on 7/26/16.
 */
public class TrestleClassException extends Exception {

    public enum State {
        INVALID,
        MISSING,
        INCOMPLETE,
        //        I don't know what else to call this, it's when you have too many of something
        EXCESS
    }

    public TrestleClassException(String message) {
        super(message);
    }
}