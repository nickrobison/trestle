package com.nickrobison.trestle.reasoner.engines.merge;

/**
 * Created by nrobison on 6/28/17.
 */
public class TrestleMergeException extends RuntimeException {

    public TrestleMergeException(Throwable t) {
        super(t);
    }

    public TrestleMergeException(String message) {
        super(message);
    }
}
