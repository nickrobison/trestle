package com.nickrobison.trestle.reasoner.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 7/26/16.
 */
public class InvalidClassException extends TrestleClassException {

    public enum State {
        INVALID,
        MISSING,
        INCOMPLETE,
        //        I don't know what else to call this, it's when you have too many of something
        EXCESS
    }

    private final State problemState;
    private final @Nullable String member;

    public InvalidClassException(Class<?> clazz, String member, String problem) {
        super(String.format("%s:%s %s", clazz, member, problem));
        this.problemState = State.INVALID;
        this.member = member;
    }


    public InvalidClassException(String className, State problemState) {
        super(parseException(className, problemState, null));
        this.problemState = problemState;
        this.member = null;
    }

    public InvalidClassException(String className, State problemState, String member) {
        super(parseException(className, problemState, member));
        this.problemState = problemState;
        this.member = member;
    }

    public InvalidClassException(Class<?> clazz, State problemState) {
        super(parseException(clazz.getSimpleName(), problemState, null));
        this.problemState = problemState;
        this.member = null;
    }

    public InvalidClassException(Class<?> clazz, State problemState, String member) {
        super(parseException(clazz.getSimpleName(), problemState, member));
        this.problemState = problemState;
        this.member = member;
    }

    public State getProblemState() {
        return this.problemState;
    }

    public @Nullable  String getMember() {
        return member;
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
