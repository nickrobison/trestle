package com.nickrobison.trestle.exceptions;

/**
 * Created by nrobison on 7/27/16.
 */
public class UnregisteredClassException extends TrestleClassException {

    public UnregisteredClassException(Class missingClass) {
        super(String.format("Class %s is not registered with the reasoner", missingClass.getSimpleName()));
    }
}
