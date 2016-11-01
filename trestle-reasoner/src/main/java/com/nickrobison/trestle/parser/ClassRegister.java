package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exceptions.UnsupportedTypeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.parser.ClassParser.filterMethodName;

/**
 * Created by nrobison on 7/26/16.
 */
public class ClassRegister {

    private ClassRegister() {
    }

    public static void ValidateClass(Class<?> clazz) throws TrestleClassException {

//        final Class aClass = clazz.getClass();

        //        Check for class name
        checkForClassName(clazz);

//        Check for individual identifier
        checkIndividualIdentifier(clazz);

//        Check for constructor
        checkForConstructor(clazz);

//        Check for valid spatial
        checkForSpatial(clazz);

//        Check for temporals
        checkForTemporals(clazz);

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
            final String typeName = spatialField.getType().getTypeName();
//            Ensure that the spatial field points to a supported type class
            if (!typeName.contains("java.lang.String")
                    && !typeName.contains("com.vividsolutions.jts")
                    && !typeName.contains("com.esri.core.geometry")
                    && !typeName.contains("org.opengis.geometry")) {
                throw new UnsupportedTypeException(Spatial.class, spatialField.getGenericType());
            }

//            Check to ensure it matches a constructor argument
            final Spatial annotation = spatialField.getAnnotation(Spatial.class);
            if (!annotation.name().equals("")) {
                matchConstructorArgument(aClass, annotation.name());
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
                final String typeName = spatialMethod.getReturnType().getTypeName();
                //            Ensure that the spatial field points to a supported type class
                if (!typeName.contains("java.lang.String")
                        && !typeName.contains("com.vividsolutions.jts")
                        && !typeName.contains("com.esri.core.geometry")
                        && !typeName.contains("org.opengis.geometry")) {
                    throw new UnsupportedTypeException(Spatial.class, spatialMethod.getGenericReturnType());
                }
                final Spatial annotation = spatialMethod.getAnnotation(Spatial.class);
                if (!annotation.name().equals("")) {
                    matchConstructorArgument(aClass, annotation.name());
                }
            }
        }
    }

    //    TODO(nrobison): Make sure the spatial annotations have matching constructor arguments
    static void checkForTemporals(Class<?> aClass) throws TrestleClassException {
        int temporalCount = 0;
        final List<Method> methods = Arrays.asList(aClass.getDeclaredMethods());
        final List<Field> fields = Arrays.asList(aClass.getDeclaredFields());

//        Default Temporal
//        Methods
        final List<Method> defaultMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(DefaultTemporalProperty.class))
                .collect(Collectors.toList());
        if (defaultMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "DefaultTemporal");
        }

        if (defaultMethods.size() == 1) {
            temporalCount = temporalCount + defaultMethods.size();
//        See if it matches a constructor argument
//            Take the property name, if it exists
            final Method defaultMethod = defaultMethods.get(0);
            if (!defaultMethod.getAnnotation(DefaultTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, defaultMethod.getAnnotation(DefaultTemporalProperty.class).name());
            } else {
                matchConstructorArgument(aClass, filterMethodName(defaultMethod));
            }
            //        Check for time zone
            verifyTimeZone(aClass, defaultMethod.getAnnotation(DefaultTemporalProperty.class).timeZone(), DefaultTemporalProperty.class);
        }

//        Fields
        final List<Field> defaultFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(DefaultTemporalProperty.class))
                .collect(Collectors.toList());
        if (defaultFields.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "DefaultTemporal");
        }
        temporalCount = temporalCount + defaultFields.size();
        if (temporalCount > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "DefaultTemporal");
        }
//        See if it matches a constructor argument
        if (defaultFields.size() == 1) {
            final Field defaultField = defaultFields.get(0);
            if (defaultField.getAnnotation(DefaultTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, defaultField.getName());
            } else {
                matchConstructorArgument(aClass, defaultField.getAnnotation(DefaultTemporalProperty.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, defaultField.getAnnotation(DefaultTemporalProperty.class).timeZone(), DefaultTemporalProperty.class);
        }

//        Start Temporal
        //        Methods
        final List<Method> startMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(StartTemporalProperty.class))
                .collect(Collectors.toList());
        if (startMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "StartTemporal");
        }

        if (startMethods.size() == 1) {
            temporalCount = temporalCount + startMethods.size();
//        See if it matches a constructor argument
            final Method startMethod = startMethods.get(0);
            if (startMethod.getAnnotation(StartTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, filterMethodName(startMethod));
            } else {
                matchConstructorArgument(aClass, startMethod.getAnnotation(StartTemporalProperty.class).name());
            }
            //        Check for time zone
            verifyTimeZone(aClass, startMethod.getAnnotation(StartTemporalProperty.class).timeZone(), StartTemporalProperty.class);
        }

//        Fields
        final List<Field> startFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(StartTemporalProperty.class))
                .collect(Collectors.toList());
        if (startFields.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "StartTemporal");
        }
        temporalCount = temporalCount + startFields.size();
        if (temporalCount > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "StartTemporal");
        }
//        See if it matches a constructor argument
        if (startFields.size() == 1) {
            final Field startField = startFields.get(0);
            if (startField.getAnnotation(StartTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, startField.getName());
            } else {
                matchConstructorArgument(aClass, startField.getAnnotation(StartTemporalProperty.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, startField.getAnnotation(StartTemporalProperty.class).timeZone(), StartTemporalProperty.class);
        }

//        End Temporal
        //        Methods
        final List<Method> endMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(EndTemporalProperty.class))
                .collect(Collectors.toList());
        if (endMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "EndTemporal");
        }

        if (endMethods.size() == 1) {
            temporalCount = temporalCount + endMethods.size();
//        See if it matches a constructor argument
            final Method endMethod = endMethods.get(0);
            if (endMethod.getAnnotation(EndTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, filterMethodName(endMethod));
            } else {
                matchConstructorArgument(aClass, endMethod.getAnnotation(EndTemporalProperty.class).name());
            }
            //        Check for time zone
            verifyTimeZone(aClass, endMethod.getAnnotation(EndTemporalProperty.class).timeZone(), EndTemporalProperty.class);
        }

//        Fields
        final List<Field> endFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(EndTemporalProperty.class))
                .collect(Collectors.toList());
        if (endFields.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "EndTemporal");
        }
        temporalCount = temporalCount + endFields.size();
        if (temporalCount > 2) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "EndTemporal");
        }
//        See if it matches a constructor argument
        if (endFields.size() == 1) {
            final Field endField = endFields.get(0);
            if (endField.getAnnotation(EndTemporalProperty.class).name().equals("")) {
                matchConstructorArgument(aClass, endField.getName());
            } else {
                matchConstructorArgument(aClass, endField.getAnnotation(EndTemporalProperty.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, endField.getAnnotation(EndTemporalProperty.class).timeZone(), EndTemporalProperty.class);
        }

    }

    private static void verifyTimeZone(Class<?> clazz, String zoneID, Class<? extends Annotation> annotation) throws InvalidClassException {
        if (!zoneID.equals("")) {
            try {
                ZoneId.of(zoneID);
            } catch (DateTimeException e) {
                throw new InvalidClassException(clazz, InvalidClassException.State.INVALID, annotation.toString());
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
