package com.nickrobison.trestle.common.exceptions;

public class TrestleInvalidDataException extends RuntimeException {

    private final Object value;
    private final String message;

    public TrestleInvalidDataException(String message, Object value) {
        super(String.format("%s. Type: %s", message, value.getClass()));
        this.value = value;
        this.message = message;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "TrestleInvalidDataException{" +
                "value=" + value +
                ", message='" + message + '\'' +
                '}';
    }
}
