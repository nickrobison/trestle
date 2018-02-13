package com.nickrobison.trestle.common.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;

public class TrestleInvalidDataException extends RuntimeException {

    private final Object value;

    public TrestleInvalidDataException(@Nullable String message, Object value) {
        super(String.format("%s. Type: %s", message == null ? "" : message, value.getClass()));
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "TrestleInvalidDataException{" +
                "value=" + value +
                '}';
    }
}
