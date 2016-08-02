package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exceptions.UnsupportedTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 7/26/16.
 */
public class ClassRegister {

    private ClassRegister() {
    }

    public static void ValidateClass(Class<?> clazz) {

//        final Class aClass = clazz.getClass();

        //        Check for class name
        try {
            checkForClassName(clazz);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }

//        Check for individual identifier
        try {
            checkIndividualIdentifier(clazz);
        } catch (InvalidClassException e) {
            throw new RuntimeException(e);
        }

//        Check for constructor
        try {
            checkForConstructor(clazz);
        } catch (TrestleClassException e) {
            throw new RuntimeException(e);
        }

//        Check for valid spatial
        try {
            checkForSpatial(clazz);
        } catch (TrestleClassException e) {
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
            throw new InvalidClassException(IndividualIdentifier.class.toString(), InvalidClassException.State.EXCESS);
        } else if ((identifierFields.size() + identifierMethods.size()) == 0) {
            throw new InvalidClassException(IndividualIdentifier.class.toString(), InvalidClassException.State.MISSING);
        }
    }

    static void checkForClassName(Class aClass) throws InvalidClassException {
        if (!aClass.isAnnotationPresent(OWLClassName.class)) {
//            I don't think I need this check, because a blank className just means take the name of the java class
//            final OWLClassName className = aClass.getAnnotation(OWLClassName.class);
//            if (className.className().equals("")) {
//                throw new InvalidClassException(OWLClassName.class.toString(), TrestleClassException.State.INCOMPLETE, "className")
//            }
            throw new InvalidClassException(OWLClassName.class.toString(), InvalidClassException.State.MISSING);
        }
    }

    static void checkForConstructor(Class<?> aClass) throws TrestleClassException {
        final List<Constructor<?>> validConstructors = new ArrayList<>();
        final Constructor<?>[] declaredConstructors = aClass.getDeclaredConstructors();
        for (Constructor<?> constructor : declaredConstructors) {
            if (constructor.isAnnotationPresent(TrestleCreator.class)) {
                validConstructors.add(constructor);
            }
        }
        if (validConstructors.size() > 1) {
            throw new InvalidClassException(aClass.getName(), InvalidClassException.State.EXCESS, "constructor");
        } else if (validConstructors.size() == 1) {
        } else {
//            If no constructors are declared as default, make sure there is only no-arg and arg
            if (declaredConstructors.length > 2) {
                throw new InvalidClassException(aClass.getName(), InvalidClassException.State.EXCESS, "default constructors");
            } else {
//                Make sure we can read the argument names
                for (Constructor<?> constructor : declaredConstructors) {
                    if (constructor.getParameters().length > 0) {
                        if (!constructor.getParameters()[0].isNamePresent()) {
                            throw new MissingConstructorException("Cannot read parameters names from constructor");
                        }
                    }
                }
            }
        }
    }

    static void checkForSpatial(Class<?> aClass) throws TrestleClassException {

//        Check fields
        final List<Field> spatialFields = Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .collect(Collectors.toList());

//        Can have a maximum of 1 spatial field
        if (spatialFields.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "Spatial");
        } else if (spatialFields.size() == 1) {
//            Check to make sure the field is a type we can handle
            final Field spatialField = spatialFields.get(0);
            switch (spatialField.getType().getTypeName()) {
                case "java.lang.String": {
                    break;
                }
//                ESRI
//                Geotools
                default:
                    throw new UnsupportedTypeException(Spatial.class, spatialField.getGenericType());
            }

//            Check to ensure it matches a constructor argument
            final Spatial annotation = spatialField.getAnnotation(Spatial.class);
            if (!annotation.argName().equals("")) {
                matchConstructorArgument(aClass, annotation.argName());
            }
        }

//        Check methods, if we haven't found any fields
        if (spatialFields.size() == 0) {
            final List<Method> spatialMethods = Arrays.stream(aClass.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Spatial.class))
                    .collect(Collectors.toList());

            if (spatialMethods.size() > 1) {
                throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "Spatial");
            } else if (spatialMethods.size() == 1) {
                final Method spatialMethod = spatialMethods.get(0);
                switch (spatialMethod.getReturnType().getTypeName()) {
                    case "java.lang.String": {
                        break;
                    }
//                ESRI
//                Geotools
                    default:
                        throw new UnsupportedTypeException(Spatial.class, spatialMethod.getGenericReturnType());
                }
                final Spatial annotation = spatialMethod.getAnnotation(Spatial.class);
                if (!annotation.argName().equals("")) {
                    matchConstructorArgument(aClass, annotation.argName());
                }
            }
        }
    }

    private static void matchConstructorArgument(Class<?> clazz, String argName) throws TrestleClassException {
        final Constructor<?> constructor = ClassBuilder.findTrestleConstructor(clazz).orElseThrow(MissingConstructorException::new);
        final Optional<String> matchingArgument = Arrays.stream(constructor.getParameters())
                .map(Parameter::getName)
                .filter(n -> n.equals(argName))
                .findFirst();
        matchingArgument.orElseThrow(() -> new InvalidClassException(clazz, InvalidClassException.State.MISSING));
    }
}
