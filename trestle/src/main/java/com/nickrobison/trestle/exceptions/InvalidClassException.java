package com.nickrobison.trestle.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 7/26/16.
 */
public class InvalidClassException extends TrestleClassException {

    private final State problemState;

    public InvalidClassException(String className, State problemState) {
        super(parseException(className, problemState, null));
        this.problemState = problemState;
    }

    public InvalidClassException(String className, State problemState, String member) {
        super(parseException(className, problemState, member));
        this.problemState = problemState;
    }

    public State getProblemState() {
        return this.problemState;
    }



    private static String parseException(String className, State problemState, @Nullable String member) {

        switch (problemState) {
            case MISSING: {
                return String.format("Missing %s", className);
            }
            case INVALID: {
                if (member == null) {
                    return String.format("Invalid configuration for %s", className);
                } else {
                    return String.format("Invalid configuration for %s, regarding %s", className, member);
                }
            }
            case INCOMPLETE: {
                if (member == null) {
                    return String.format("Incomplete configuration of class %s", className);
                } else {
                    return String.format("Missing field %s on class %s", member, className);
                }
            }

            case EXCESS: {
                if (member == null) {
                    return String.format("Excessive number of class %s", className);
                } else {
                    return String.format("Excessive field %s on class %s", member, className);
                }
            }
            default: return "";
        }
    }
}
