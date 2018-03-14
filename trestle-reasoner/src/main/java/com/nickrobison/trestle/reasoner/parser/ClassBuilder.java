package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.reasoner.annotations.TrestleCreator;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.*;

/**
 * Created by nrobison on 7/28/16.
 */
@SuppressWarnings("Duplicates")
public class ClassBuilder implements IClassBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClassBuilder.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    ClassBuilder() {
//        Not used
    }

    @Override
    public OWLLiteral getProjectedWKT(Class<?> clazz, Object spatialObject, @Nullable Integer srid) {
        logger.warn("No fully implemented, returning toString value");
        return df.getOWLLiteral(spatialObject.toString(), df.getOWLDatatype(StaticIRI.WKTDatatypeIRI));
    }

    @Override
    public Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz) {
        return getPropertyMembers(clazz, false, TRESTLE_PREFIX);
    }

    @Override
    public Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz, boolean filterSpatial) {
        return getPropertyMembers(clazz, filterSpatial, TRESTLE_PREFIX);
    }

    /**
     * Parses out the data properties fof a given input class
     * Only returns the property axioms, not the values themselves
     *
     * @param clazz         - Input class to parse
     * @param filterSpatial - Boolean to filter out the spatial properties
     * @param prefix        - Prefix to use when building the data properties
     * @return - Optional list of {@link OWLDataProperty} for given class
     */
    private static Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz, boolean filterSpatial, String prefix) {

        List<OWLDataProperty> classFields = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> filterFactField(f, filterSpatial))
                .forEach(field -> classFields.add(dfStatic.getOWLDataProperty(SpatialParser.filterDataSpatialName(field, prefix))));

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> filterFactMethod(m, filterSpatial))
                .forEach(method -> classFields.add(dfStatic.getOWLDataProperty(SpatialParser.filterDataSpatialName(method, prefix))));

        if (classFields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(classFields);
    }

    //    FIXME(nrobison): I think these warnings are important.
    @SuppressWarnings({"type.argument.type.incompatible", "assignment.type.incompatible", "unchecked"})
    private static <T> T constructObject(Class<T> clazz, Class<?>[] inputClasses, Object[] inputObjects) throws MissingConstructorException {

        final Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(inputClasses);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot get constructor matching params: {}", inputClasses, e);
            throw new MissingConstructorException(String.format("Can't get constructor for %s", clazz.getName()));
        }

        try {
            return (T) constructor.newInstance(inputObjects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //    FIXME(nrobison): I think these warnings are important.
    @SuppressWarnings({"type.argument.type.incompatible", "assignment.type.incompatible", "method.invocation.invalid", "argument.type.incompatible"})
    @Override
    public <T> T constructObject(Class<T> clazz, ConstructorArguments arguments) throws MissingConstructorException {
        Constructor<?> declaredConstructor = findTrestleConstructor(clazz).orElseThrow(MissingConstructorException::new);

//        Get the list of parameters
        final Parameter[] parameters = declaredConstructor.getParameters();
        List<String> parameterNames = Arrays.stream(parameters)
                .map(Parameter::getName)
                .collect(Collectors.toList());

//        Get sorted types and values
        final Class<?>[] sortedTypes = arguments.getSortedTypes(parameterNames);
        final Object[] sortedValues = arguments.getSortedValues(parameterNames);
        if ((sortedTypes.length != parameterNames.size()) || (sortedValues.length != parameterNames.size())) {
            logger.error("Wrong number of constructor arguments, need {} have {}", parameterNames.size(), sortedValues.length);
            logger.error("Constructor for class {} has parameters {}, but we have {}", clazz.getSimpleName(), parameterNames, arguments.getNames());

//            Class::getTypeName works fine here
            @SuppressWarnings("methodref.receiver.invalid")
            final List<? extends Class<?>> types = Arrays.stream(parameters)
                    .map(Parameter::getType)
                    .sorted(Comparator.comparing(Class::getTypeName))
                    .collect(Collectors.toList());
            logger.error("Constructor has parameter types {}, but we have {}", types, arguments.getTypes());
            throw new MissingConstructorException("Missing parameters required for constructor generation");
        }

        return constructObject(clazz, sortedTypes, sortedValues);

    }

    //    I don't like suppressing the @UnknownInitialization warning, but I can't figure out when it would cause an error
    @SuppressWarnings("initialization")
// TODO(nrobison): Make this Class<T>?
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
        return Optional.ofNullable(declaredConstructor);
    }

    /**
     * Determines if a given argument name/type pair matches the declared TrestleConstructor
     *
     * @param clazz        - Java class to parse
     * @param argumentName - Argument name to match
     * @param argumentType - Nullable argument type to match
     * @return - Boolean if name/type pair matches
     * @throws MissingConstructorException - throws if cannot match class constructor with provided argument
     */
    public static boolean isConstructorArgument(Class<?> clazz, String argumentName, @Nullable Class<?> argumentType) throws MissingConstructorException {
        final Optional<Constructor<?>> trestleConstructor = findTrestleConstructor(clazz);

        final Optional<Parameter> matchingParam = Arrays.stream(trestleConstructor.orElseThrow(MissingConstructorException::new).getParameters())
                .filter(p -> p.getName().equals(argumentName))
                .findFirst();

        return matchingParam.map(parameter -> argumentType == null || parameter.getType() == argumentType).orElse(Boolean.FALSE);
    }


}
