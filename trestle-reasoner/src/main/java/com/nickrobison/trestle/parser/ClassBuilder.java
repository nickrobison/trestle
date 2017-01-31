package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.parser.ClassParser.*;

/**
 * Created by nrobison on 7/28/16.
 */
@SuppressWarnings("Duplicates")
public class ClassBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClassBuilder.class);

    /**
     * Get a list of data properties from a given class
     *
     * @param clazz - Class to parse for data property members
     * @return - Optional list of OWLDataProperty from given class
     */
    public static Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz) {
        return getPropertyMembers(clazz, false);
    }

    /**
     * Parses out the data properties fof a given input class
     * Only returns the property axioms, not the values themselves
     * @param clazz - Input class to parse
     * @param filterSpatial - Boolean to filter out the spatial properties
     * @return - Optional List of OWLDataProperties
     */
    public static Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz, boolean filterSpatial) {

        List<OWLDataProperty> classFields = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> filterFactField(f, filterSpatial))
                .forEach(field -> classFields.add(dfStatic.getOWLDataProperty(SpatialParser.filterDataSpatialName(field))));

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> filterFactMethod(m, filterSpatial))
                .forEach(method -> classFields.add(dfStatic.getOWLDataProperty(SpatialParser.filterDataSpatialName(method))));

        if (classFields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(classFields);
    }

    //    FIXME(nrobison): I think these warnings are important.
    @SuppressWarnings({"type.argument.type.incompatible", "assignment.type.incompatible"})
    public static <T> T ConstructObject(Class<T> clazz, Class<?>[] inputClasses, Object[] inputObjects) {

        final Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(inputClasses);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot get constructor matching params: {}", inputClasses, e);
            throw new RuntimeException("Can't get constructor", e);
        }

        try {
            return (T) constructor.newInstance(inputObjects);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    //    FIXME(nrobison): I think these warnings are important.
    @SuppressWarnings({"type.argument.type.incompatible", "assignment.type.incompatible", "method.invocation.invalid", "argument.type.incompatible"})
    public static <T> T ConstructObject(Class<T> clazz, ConstructorArguments arguments) throws MissingConstructorException {
        Constructor<?> declaredConstructor = findTrestleConstructor(clazz).orElseThrow(MissingConstructorException::new);

//        Get the list of parameters
        final Parameter[] parameters = declaredConstructor.getParameters();
        List<String> parameterNames = Arrays.stream(parameters)
                .map(Parameter::getName)
                .collect(Collectors.toList());

//        Get sorted types and values
        final Class<?>[] sortedTypes = arguments.getSortedTypes(parameterNames);
        final Object[] sortedValues = arguments.getSortedValues(parameterNames);
        if ((sortedTypes.length != parameterNames.size()) | (sortedValues.length != parameterNames.size())) {
            logger.error("Constructor for class {} has parameters {}, but we have {}", clazz.getSimpleName(), parameterNames, arguments.getNames());

            final List<? extends Class<?>> types = Arrays.stream(parameters)
                    .map(Parameter::getType)
                    .sorted(Comparator.comparing(Class::getTypeName))
                    .collect(Collectors.toList());
            logger.error("Constructor has parameter types {}, but we have {}", types, arguments.getTypes());
            throw new RuntimeException("Missing parameters required for constructor generation");
        }

        return ConstructObject(clazz, sortedTypes, sortedValues);

    }

    //    I don't like suppressing the @UnknownInitialization warning, but I can't figure out when it would cause an error
    @SuppressWarnings("initialization")
    static Optional<Constructor<?>> findTrestleConstructor(Class<?> clazz) {
        @MonotonicNonNull Constructor<?> declaredConstructor = null;
        final Optional<? extends Constructor<?>> specifiedConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(TrestleCreator.class))
                .findFirst();

        if (specifiedConstructor.isPresent()) {
            declaredConstructor = specifiedConstructor.get();
        } else {
//            If there's no constructor, look for one that has more than 0 arguments
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (constructor.isAnnotationPresent(TrestleCreator.class)) {
                    declaredConstructor = constructor;
                    break;
                } else {
                    if (constructor.getParameters().length > 0) {
                        declaredConstructor = constructor;
                        break;
                    }
                }
            }
        }

        if (declaredConstructor != null) {
            return Optional.of(declaredConstructor);
        }
        return Optional.empty();
    }

    /**
     * Determines if a given argument name/type pair matches the declared TrestleConstructor
     * @param clazz - Java class to aprse
     * @param argumentName - Argument name to match
     * @param argumentType - Nullable argument type to match
     * @return - Boolean if name/type pair matches
     * @throws MissingConstructorException
     */
    static boolean isConstructorArgument(Class<?> clazz, String argumentName, @Nullable Class<?> argumentType) throws MissingConstructorException {
        final Optional<Constructor<?>> trestleConstructor = findTrestleConstructor(clazz);

        final Optional<Parameter> matchingParam = Arrays.stream(trestleConstructor.orElseThrow(MissingConstructorException::new).getParameters())
                .filter(p -> p.getName().equals(argumentName))
                .findFirst();

        return matchingParam.map(parameter -> argumentType == null || parameter.getType().equals(argumentType)).orElse(false);
    }


}
