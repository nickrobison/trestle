package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.DataProperty;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.parser.ClassParser.df;
import static com.nickrobison.trestle.common.StaticIRI.PREFIX;

/**
 * Created by nrobison on 7/28/16.
 */
@SuppressWarnings("Duplicates")
public class ClassBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClassBuilder.class);
    private static final Map<OWL2Datatype, Class<?>> datatypeMap = buildDatatype2ClassMap();

    /**
     * Get a list of data properties from a given class
     *
     * @param clazz - Class to parse for data property members
     * @return - Optional list of OWLDataProperty from given class
     */
    public static Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz) {

        List<OWLDataProperty> classFields = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields())
                .filter(ClassParser::filterDataPropertyField)
                .forEach(field -> classFields.add(df.getOWLDataProperty(filterName(field))));

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(ClassParser::filterDataPropertyMethod)
                .forEach(method -> classFields.add(df.getOWLDataProperty(filterName(method))));

        if (classFields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(classFields);
    }

    private static IRI filterName(Field classField) {
        if (classField.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classField.getAnnotation(DataProperty.class).name());
        } else if (classField.isAnnotationPresent(Spatial.class)) {
            return IRI.create("geosparql:", "asWKT");
        } else {
            return IRI.create(PREFIX, classField.getName());
        }
    }

    private static IRI filterName(Method classMethod) {
        if (classMethod.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classMethod.getAnnotation(DataProperty.class).name());
        } else if (classMethod.isAnnotationPresent(Spatial.class)) {
            return IRI.create("geosparql:", "asWKT");
        } else {
            return IRI.create(PREFIX, classMethod.getName());
        }
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
            throw new RuntimeException("Missing parameters required for constructor generation");
        }

        return ConstructObject(clazz, sortedTypes, sortedValues);

    }

//    I don't like suppressing the @UnknownInitialization warning, but I can't figure out when it would case an error
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

    @SuppressWarnings({"unchecked", "type.argument.type.incompatible", "assignment.type.incompatible"})
//    FIXME(nrobison): Fix the object casts
    public static <T> T extractOWLLiteral(Class<T> javaClass, OWLLiteral literal) {

        switch (javaClass.getTypeName()) {

            case "int": {
                return (T) (Object) literal.parseInteger();
            }
            case "java.lang.Integer": {
                return (T) (Object) literal.parseInteger();
            }

            case "java.lang.LocalDateTime": {
                return (T) (Object) LocalDateTime.parse(literal.getLiteral());
            }

            case "java.lang.String": {
                return (T) (Object) literal.getLiteral();
            }

            case "java.lang.Double": {
                return (T) (Object) literal.parseDouble();
            }

            case "java.lang.Boolean": {
                return (T) (Object) literal.parseBoolean();
            }

            default: {
                throw new RuntimeException(String.format("Unsupported cast %s", javaClass));
            }
        }
    }

    @SuppressWarnings("dereference.of.nullable")
    public static Class<?> lookupJavaClassFromOWLDatatype(OWLDatatype datatype) {
        final Class<?> javaClass;
        if (datatype.isBuiltIn()) {
             javaClass = datatypeMap.get(datatype.getBuiltInDatatype());
            if (javaClass == null) {
                throw new RuntimeException(String.format("Unsupported OWL2Datatype %s", datatype));
            }
        } else if (datatype.getIRI().getScheme().equals("geosparql")) {
//            If it's from the geosparql group, we can just treat it as a string
            javaClass = String.class;
        } else {
//            String as a last resort.
            javaClass = String.class;
        }

        return javaClass;
    }


    private static Map<OWL2Datatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWL2Datatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER, Integer.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_INT, int.class);
        datatypeMap.put(OWL2Datatype.XSD_LONG, long.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE, Double.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT, double.class);
        datatypeMap.put(OWL2Datatype.XSD_DECIMAL, Double.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME, LocalDateTime.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN, Boolean.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_STRING, String.class);

        return datatypeMap;
    }
}
