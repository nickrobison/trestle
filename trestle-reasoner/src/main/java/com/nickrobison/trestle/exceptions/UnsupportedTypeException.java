package com.nickrobison.trestle.exceptions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by nrobison on 8/2/16.
 */
public class UnsupportedTypeException extends TrestleClassException {
    public UnsupportedTypeException(Class<? extends Annotation> annotation, Type invalidType) {
        super(String.format("Annotation %s does not support type %s", annotation.toString(), invalidType.getTypeName()));
    }
}
