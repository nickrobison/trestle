package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.DataProperty;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.PREFIX;
import static com.nickrobison.trestle.parser.ClassParser.*;

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
                .forEach(field -> classFields.add(df.getOWLDataProperty(filterDataSpatialName(field))));

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(ClassParser::filterDataPropertyMethod)
                .forEach(method -> classFields.add(df.getOWLDataProperty(filterDataSpatialName(method))));

        if (classFields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(classFields);
    }

    private static IRI filterDataSpatialName(Field classField) {
        if (classField.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classField.getAnnotation(DataProperty.class).name());
        } else if (classField.isAnnotationPresent(Spatial.class)) {
            return IRI.create("geosparql:", "asWKT");
        } else {
            return IRI.create(PREFIX, classField.getName());
        }
    }

    private static IRI filterDataSpatialName(Method classMethod) {
        if (classMethod.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classMethod.getAnnotation(DataProperty.class).name());
        } else if (classMethod.isAnnotationPresent(Spatial.class)) {
            return IRI.create("geosparql:", "asWKT");
        } else {
            return IRI.create(PREFIX, filterMethodName(classMethod));
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
            logger.error("Constructor has parameters {}, but we have {}", parameterNames, arguments.getNames());

            final List<? extends Class<?>> types = Arrays.stream(parameters)
                    .map(Parameter::getType)
                    .collect(Collectors.toList());
            logger.error("Constructor has parameter types {}, but we have {}", types, arguments.getTypes());
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

    static boolean isConstructorArgument(Class<?> clazz, String argumentName, @Nullable Class<?> argumentType) throws MissingConstructorException {
        final Optional<Constructor<?>> trestleConstructor = findTrestleConstructor(clazz);

        final Optional<Parameter> matchingParam = Arrays.stream(trestleConstructor.orElseThrow(MissingConstructorException::new).getParameters())
                .filter(p -> p.getName().equals(argumentName))
                .findFirst();

        if (matchingParam.isPresent()) {
            if (argumentType != null) {

                if (matchingParam.get().getAnnotatedType().equals(argumentType)) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

//    I need the unchecked casts in order to get the correct primitives for the constructor generation
    @SuppressWarnings({"unchecked"})
    public static <T> @NonNull T extractOWLLiteral(Class<@NonNull T> javaClass, OWLLiteral literal) {

        switch (javaClass.getTypeName()) {

            case "int": {
//                return javaClass.cast(literal.parseInteger());
                return (@NonNull T) (Object) literal.parseInteger();
            }

            case "java.lang.Integer": {
                return javaClass.cast(literal.parseInteger());
//                return (T) (Object) literal.parseInteger();
            }

            case "long": {
//                return javaClass.cast(Long.parseLong(literal.getLiteral()));
                return (@NonNull T) (Object) Long.parseLong(literal.getLiteral());
            }

            case "java.lang.Long": {
                return javaClass.cast(Long.parseLong(literal.getLiteral()));
//                return (T) (Object) Long.parseLong(literal.getLiteral());
            }

            case "java.lang.LocalDateTime": {
                return javaClass.cast(LocalDateTime.parse(literal.getLiteral()));
//                return (T) (Object) LocalDateTime.parse(literal.getLiteral());
            }

            case "java.lang.String": {
                return javaClass.cast(literal.getLiteral());
//                return (T) (Object) literal.getLiteral();
            }

            case "java.lang.Double": {
                return javaClass.cast(literal.parseDouble());
//                return (T) (Object) literal.parseDouble();
            }

            case "java.lang.Boolean": {
                return javaClass.cast(literal.parseBoolean());
//                return (T) (Object) literal.parseBoolean();
            }

            default: {
                throw new RuntimeException(String.format("Unsupported cast %s", javaClass));
            }
        }
    }

    @SuppressWarnings("dereference.of.nullable")
    public static Class<?> lookupJavaClassFromOWLDatatype(OWLDataPropertyAssertionAxiom dataproperty, @Nullable Class<?> classToVerify) {
        final Class<?> javaClass;
        final OWLDatatype datatype = dataproperty.getObject().getDatatype();
        if (datatype.isBuiltIn()) {

//            Check with the class to make sure the types are correct
            OWL2Datatype dataTypeToLookup = null;
            if (classToVerify != null) {
                dataTypeToLookup = verifyOWLType(classToVerify, dataproperty.getProperty().asOWLDataProperty());
            }
            if (dataTypeToLookup == null) {
                dataTypeToLookup = datatype.getBuiltInDatatype();
            }
            javaClass = datatypeMap.get(dataTypeToLookup);
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

    private static @Nullable OWL2Datatype verifyOWLType(Class<?> classToVerify, OWLDataProperty property) {

        //        Check to see if it matches any annotated data methods
        final Optional<Method> matchedMethod = Arrays.stream(classToVerify.getDeclaredMethods())
                .filter(m -> getMethodName(m).equals(property.getIRI().getShortForm()))
                .findFirst();

        if (matchedMethod.isPresent()) {
            return ClassParser.getDatatypeFromJavaClass(matchedMethod.get().getReturnType());
        }

//        Fields
        final Optional<Field> matchedField = Arrays.stream(classToVerify.getDeclaredFields())
                .filter(f -> getFieldName(f).equals(property.getIRI().getShortForm()))
                .findFirst();

        if (matchedField.isPresent()) {
            return ClassParser.getDatatypeFromJavaClass(matchedField.get().getType());
        }

        return null;
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
