package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.TemporalProperty;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TemporalType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nrobison on 7/26/16.
 */
public class ClassRegister {

    private ClassRegister() {
    }

    public static void RegisterClass(Class clazz) {

        final Class<? extends Class> aClass = clazz.getClass();

        //        Check for class name
        try {
            checkForClassName(aClass);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }

//        Check for individual identifier
        try {
            checkIndividualIdentifier(aClass);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }

//        Check for valid temporals
//        if (aClass.isAnnotationPresent(TemporalProperty.class)) {
//            for (final Field field : aClass.getDeclaredFields()) {
//                if (field.isAnnotationPresent(TemporalProperty.class)) {
//                    final TemporalProperty declaredAnnotation = field.getDeclaredAnnotation(TemporalProperty.class);
//
////                    Check for property built StartTemporal
//                    if (declaredAnnotation instanceof StartTemporalProperty) {
//                        if (((StartTemporalProperty) declaredAnnotation).type() == TemporalType.INTERVAL) {
////                            If it's an interval, we need to look for its
//                        }
//                    }
//                }
//            }
//        }
    }

    static void checkIndividualIdentifier(Class aClass) throws InvalidClassException {

            List<Field> identifierFields = new ArrayList<>();
            List<Method> identifierMethods = new ArrayList<>();
//            Check to see if there are more than one
            for (final Field field : aClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(IndividualIdentifier.class)) {
                    identifierFields.add(field);
                }
            }

            for (final Method method : aClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(IndividualIdentifier.class)) {
                    identifierMethods.add(method);
                }
            }

            if ((identifierFields.size() + identifierMethods.size()) > 1) {
                throw new InvalidClassException(IndividualIdentifier.class.toString(), TrestleClassException.State.EXCESS);
            } else if ((identifierFields.size() + identifierMethods.size()) == 0) {
                throw new InvalidClassException(IndividualIdentifier.class.toString(), TrestleClassException.State.MISSING);
        }
    }

    static void checkForClassName(Class aClass) throws InvalidClassException {
        if (aClass.isAnnotationPresent(OWLClassName.class)) {
//            I don't think I need this check, because a blank className just means take the name of the java class
//            final OWLClassName className = aClass.getAnnotation(OWLClassName.class);
//            if (className.className().equals("")) {
//                throw new InvalidClassException(OWLClassName.class.toString(), TrestleClassException.State.INCOMPLETE, "className")
//            }
        } else {
            throw new InvalidClassException(OWLClassName.class.toString(), TrestleClassException.State.MISSING);
        }
    }
}
