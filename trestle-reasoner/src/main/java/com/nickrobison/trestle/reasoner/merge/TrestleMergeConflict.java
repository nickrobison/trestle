package com.nickrobison.trestle.reasoner.merge;

/**
 * Created by nrobison on 6/13/17.
 */
public class TrestleMergeConflict extends TrestleMergeException {

    public TrestleMergeConflict(Throwable t) {
        super(t);
    }

    public TrestleMergeConflict(String message) {
        super(message);
    }
}
