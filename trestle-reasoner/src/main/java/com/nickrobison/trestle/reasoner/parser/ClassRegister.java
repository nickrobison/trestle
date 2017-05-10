package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.InvalidClassException;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnsupportedTypeException;

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

import static com.nickrobison.trestle.common.LanguageUtils.checkLanguageCodeIsValid;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.filterMethodName;

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
//        checkForTemporals(clazz);

//        Check for language
        checkForLanguage(clazz);

//        Check for disabled language
        checkForDisabledMultiLanguage(clazz);
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
        if (!aClass.isAnnotationPresent(DatasetClass.class)) {
//            I don't think I need this check, because a blank name just means take the name of the java class
//            final DatasetClass name = aClass.getAnnotation(DatasetClass.class);
//            if (name.name().equals("")) {
//                throw new InvalidClassException(DatasetClass.class.toString(), TrestleClassException.State.INCOMPLETE, "name")
//            }
            throw new InvalidClassException(DatasetClass.class.toString(), InvalidClassException.State.MISSING);
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


    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
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
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    static void checkForTemporals(Class<?> aClass) throws TrestleClassException {
        int temporalCount = 0;
        final List<Method> methods = Arrays.asList(aClass.getDeclaredMethods());
        final List<Field> fields = Arrays.asList(aClass.getDeclaredFields());

//        Default Temporal
//        Methods
        final List<Method> defaultMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(DefaultTemporal.class))
                .collect(Collectors.toList());
        if (defaultMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "DefaultTemporal");
        }

        if (defaultMethods.size() == 1) {
            temporalCount = temporalCount + defaultMethods.size();
//        See if it matches a constructor argument
//            Take the property name, if it exists
            final Method defaultMethod = defaultMethods.get(0);
            if (!defaultMethod.getAnnotation(DefaultTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, defaultMethod.getAnnotation(DefaultTemporal.class).name());
            } else {
                matchConstructorArgument(aClass, filterMethodName(defaultMethod));
            }
            //        Check for time zone
            verifyTimeZone(aClass, defaultMethod.getAnnotation(DefaultTemporal.class).timeZone(), DefaultTemporal.class);
        }

//        Fields
        final List<Field> defaultFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(DefaultTemporal.class))
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
            if (defaultField.getAnnotation(DefaultTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, defaultField.getName());
            } else {
                matchConstructorArgument(aClass, defaultField.getAnnotation(DefaultTemporal.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, defaultField.getAnnotation(DefaultTemporal.class).timeZone(), DefaultTemporal.class);
        }

//        Start Temporal
        //        Methods
        final List<Method> startMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(StartTemporal.class))
                .collect(Collectors.toList());
        if (startMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "StartTemporal");
        }

        if (startMethods.size() == 1) {
            temporalCount = temporalCount + startMethods.size();
//        See if it matches a constructor argument
            final Method startMethod = startMethods.get(0);
            if (startMethod.getAnnotation(StartTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, filterMethodName(startMethod));
            } else {
                matchConstructorArgument(aClass, startMethod.getAnnotation(StartTemporal.class).name());
            }
            //        Check for time zone
            verifyTimeZone(aClass, startMethod.getAnnotation(StartTemporal.class).timeZone(), StartTemporal.class);
        }

//        Fields
        final List<Field> startFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(StartTemporal.class))
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
            if (startField.getAnnotation(StartTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, startField.getName());
            } else {
                matchConstructorArgument(aClass, startField.getAnnotation(StartTemporal.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, startField.getAnnotation(StartTemporal.class).timeZone(), StartTemporal.class);
        }

//        End Temporal
        //        Methods
        final List<Method> endMethods = methods
                .stream()
                .filter(m -> m.isAnnotationPresent(EndTemporal.class))
                .collect(Collectors.toList());
        if (endMethods.size() > 1) {
            throw new InvalidClassException(aClass, InvalidClassException.State.EXCESS, "EndTemporal");
        }

        if (endMethods.size() == 1) {
            temporalCount = temporalCount + endMethods.size();
//        See if it matches a constructor argument
            final Method endMethod = endMethods.get(0);
            if (endMethod.getAnnotation(EndTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, filterMethodName(endMethod));
            } else {
                matchConstructorArgument(aClass, endMethod.getAnnotation(EndTemporal.class).name());
            }
            //        Check for time zone
            verifyTimeZone(aClass, endMethod.getAnnotation(EndTemporal.class).timeZone(), EndTemporal.class);
        }

//        Fields
        final List<Field> endFields = fields
                .stream()
                .filter(f -> f.isAnnotationPresent(EndTemporal.class))
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
            if (endField.getAnnotation(EndTemporal.class).name().equals("")) {
                matchConstructorArgument(aClass, endField.getName());
            } else {
                matchConstructorArgument(aClass, endField.getAnnotation(EndTemporal.class).name());
            }
//            Check for time zone
            verifyTimeZone(aClass, endField.getAnnotation(EndTemporal.class).timeZone(), EndTemporal.class);
        }

    }

    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    static void checkForLanguage(Class<?> aClass) throws TrestleClassException {
//        Start with methods
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Language.class)) {
//                Ensure the return type is a string
                if (method.getReturnType() != (String.class)) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, method.getName());
                }

//                Ensure the language points to a correct language code
                final String language = method.getAnnotation(Language.class).language();
                if (!checkLanguageCodeIsValid(language)) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, method.getName());
                }

//                Ensure we're not disabled
                if (method.isAnnotationPresent(NoMultiLanguage.class)) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, method.getName());
                }
            }
        }

        for (Field field : aClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Language.class)) {
//                Ensure the return type is a string
                if (field.getType() != String.class) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, field.getName());
                }

//                Ensure the language points to a correct language code
                final String language = field.getAnnotation(Language.class).language();
                if (!checkLanguageCodeIsValid(language)) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, field.getName());
                }

//                Ensure we're not disabled
                if (field.isAnnotationPresent(NoMultiLanguage.class)) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, field.getName());
                }
            }
        }
    }

    private static void checkForDisabledMultiLanguage(Class<?> aClass) throws TrestleClassException {
        for (Method method: aClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(NoMultiLanguage.class)) {
                if (method.getReturnType() != String.class) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, method.getName());
                }
            }
        }

        for (Field field: aClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(NoMultiLanguage.class)) {
                if (field.getType() != String.class) {
                    throw new InvalidClassException(aClass, InvalidClassException.State.INVALID, field.getName());
                }
            }
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
