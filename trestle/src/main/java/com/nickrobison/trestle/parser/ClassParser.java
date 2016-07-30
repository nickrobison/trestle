package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.types.ObjectRestriction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings("initialization")
public class ClassParser {

    public static final String PREFIX = "trestle:";

    enum AccessType {
        FIELD,
        METHOD
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassParser.class);
    private static final Map<Class<?>, OWL2Datatype> owlDatatypeMap = buildClassMap();
    static final OWLDataFactory df = OWLManager.getOWLDataFactory();


    private ClassParser() {
    }

    public static OWLClass GetObjectClass(Object inputObject) {
        //        Get the class name, from the annotation, if possible;
        final Class<?> clazz = inputObject.getClass();
        return GetObjectClass(clazz);
    }

    public static OWLClass GetObjectClass(Class<?> clazz) {

        final String className;
//        final OWLClassName declaredAnnotation = clazz.getDeclaredAnnotation(OWLClassName.class);
        if (clazz.isAnnotationPresent(OWLClassName.class)) {
            className = clazz.getDeclaredAnnotation(OWLClassName.class).className();
        } else {
            className = clazz.getName();
        }
        final IRI iri = IRI.create("trestle:", className);
        return df.getOWLClass(iri);
    }

    public static OWLNamedIndividual GetIndividual(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        String identifier = UUID.randomUUID().toString();
//            Loop through the fields and figure out which one has the identifier

//        Try for fields
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(IndividualIdentifier.class)) {
                try {
//                        We only grab the first
//                    Replace the spaces with underscores
                    identifier = classField.get(inputObject).toString().replaceAll("\\s+", "_");
                    break;
                } catch (IllegalAccessException e) {
                    logger.error("Cannot access field {}", classField.getName(), e);
                }
            }
        }

//        Try for methods
        for (Method classMethod : clazz.getMethods()) {
            if (classMethod.isAnnotationPresent(IndividualIdentifier.class)) {

                final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);
                if (methodValue.isPresent()) {
                    identifier = methodValue.get().toString().replaceAll("\\s+", "_");
                }
            }
        }

        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        return df.getOWLNamedIndividual(IRI.create("trestle:", identifier));
    }

    //    TODO(nrobison): Implement this
    static Optional<List<OWLObjectProperty>> GetObjectProperties(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();

        if (clazz.isAnnotationPresent(ObjectProperty.class)) {
            for (Field classField : clazz.getDeclaredFields()) {
                if (classField.isAnnotationPresent(ObjectProperty.class)) {
                    final ObjectProperty fieldAnnotation = classField.getAnnotation(ObjectProperty.class);
                    final ObjectRestriction restriction = fieldAnnotation.restriction();
                    switch (restriction) {
                        case SOME: {

                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    static boolean filterDataPropertyField(Field objectMember) {
//        Check first to ignore the field
        return (!objectMember.isAnnotationPresent(Ignore.class)
//                Only access it if it's public
                & Modifier.isPublic(objectMember.getModifiers())
                & (
                objectMember.isAnnotationPresent(DataProperty.class)
                        | objectMember.isAnnotationPresent(Spatial.class)
                        | objectMember.isAnnotationPresent(IndividualIdentifier.class)
                        | (objectMember.getAnnotations().length == 0)));
    }

    static boolean filterDataPropertyMethod(Method objectMember) {
//        Check first to ignore the field
        return (!objectMember.isAnnotationPresent(Ignore.class)
//                Only access it if it's public
                & Modifier.isPublic(objectMember.getModifiers())
                & (
                objectMember.isAnnotationPresent(DataProperty.class)
                        | objectMember.isAnnotationPresent(Spatial.class)
                        | objectMember.isAnnotationPresent(IndividualIdentifier.class)
                        | (objectMember.getAnnotations().length == 0)))
//                We need this to filter out setters and equals/hashcode stuff
                & ( objectMember.getParameters().length == 0
                | !(objectMember.getName().equals("hashCode"))
                | !(objectMember.getReturnType() == void.class));
    }


    public static Optional<List<OWLDataPropertyAssertionAxiom>> GetDataProperties(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();
        final List<OWLDataPropertyAssertionAxiom> axioms = new ArrayList<>();

        final OWLNamedIndividual owlNamedIndividual = GetIndividual(inputObject);

//        Fields:
        for (Field classField : clazz.getDeclaredFields()) {
            if (filterDataPropertyField(classField)) {
                if (classField.isAnnotationPresent(DataProperty.class)) {
                    final DataProperty annotation = classField.getAnnotation(DataProperty.class);
                    final IRI iri = IRI.create(PREFIX, annotation.name());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    String fieldValue = null;

                    try {
                        fieldValue = classField.get(inputObject).toString();
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    final OWLLiteral owlLiteral = df.getOWLLiteral(fieldValue, getDatatypeFromAnnotation(annotation, classField.getType()));
                    axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, owlLiteral));
                } else if (classField.isAnnotationPresent(Spatial.class)) {
                    final IRI iri = IRI.create("geosparql:", "asWKT");
                    final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
                    String fieldValue = null;

                    try {
                        fieldValue = classField.get(inputObject).toString();
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    final OWLDatatype wktDatatype = df.getOWLDatatype(IRI.create("http://www.opengis.net/ont/geosparql#", "wktLiteral"));
//                    Since it's a literal, we need to strip out the double quotes.
                    final OWLLiteral wktLiteral = df.getOWLLiteral(fieldValue.replace("\"", ""), wktDatatype);
                    axioms.add(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, wktLiteral));
                } else {
                    final IRI iri = IRI.create(PREFIX, classField.getName());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    String fieldValue = null;
                    try {
                        fieldValue = classField.get(inputObject).toString();
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, fieldValue));
                }
            }
        }

//        Methods
        for (Method classMethod : clazz.getDeclaredMethods()) {
            if (filterDataPropertyMethod(classMethod)) {
                if (classMethod.isAnnotationPresent(DataProperty.class)) {
                    final DataProperty annotation = classMethod.getAnnotation(DataProperty.class);
                    final IRI iri = IRI.create(PREFIX, annotation.name());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);

                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                    if (methodValue.isPresent()) {
                        final OWLLiteral owlLiteral = df.getOWLLiteral(methodValue.get().toString(), getDatatypeFromAnnotation(annotation, classMethod.getReturnType()));
                        axioms.add(df.getOWLDataPropertyAssertionAxiom(
                                owlDataProperty,
                                owlNamedIndividual,
                                owlLiteral
                        ));
                    }
                } else if (classMethod.isAnnotationPresent(Spatial.class)) {
                    final IRI iri = IRI.create("geosparql:", "asWKT");
                    final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                    if (methodValue.isPresent()) {
                        final OWLDatatype wktDatatype = df.getOWLDatatype(IRI.create("http://www.opengis.net/ont/geosparql#", "wktLiteral"));
//                    Since it's a literal, we need to strip out the double quotes.
                        final OWLLiteral wktLiteral = df.getOWLLiteral(methodValue.get().toString().replace("\"", ""), wktDatatype);
                        axioms.add(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, wktLiteral));
                    }
                } else {
                    final IRI iri = IRI.create(PREFIX, filterMethodName(classMethod));
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);
                    if (methodValue.isPresent()) {
                        final OWLLiteral owlLiteral = df.getOWLLiteral(methodValue.get().toString(), getDatatypeFromJavaClass(classMethod.getReturnType()));
                        axioms.add(df.getOWLDataPropertyAssertionAxiom(
                                owlDataProperty,
                                owlNamedIndividual,
                                owlLiteral));
                    }
                }
            }
        }

        if (axioms.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(axioms);
    }

    private static String filterMethodName(Method classMethod) {
        String name = classMethod.getName();
//        remove get and
        if (name.startsWith("get")) {
            final String firstLetter = name.substring(3,4).toLowerCase();
            final String restOfLetters = name.substring(4);
            return firstLetter + restOfLetters;
        }

        return name;
    }

    private static OWL2Datatype getDatatypeFromAnnotation(DataProperty annotation, Class<?> objectClass) {
//        I don't think this will ever be true
        if (annotation.datatype().toString().equals("")) {
            return getDatatypeFromJavaClass(objectClass);
        } else {
            return annotation.datatype();
        }
    }

    private static
    @NotNull
    OWL2Datatype getDatatypeFromJavaClass(Class<?> javaTypeClass) {
        final OWL2Datatype owl2Datatype = owlDatatypeMap.get(javaTypeClass);
        if (owl2Datatype == null) {
            throw new RuntimeException(String.format("Unsupported Java type %s", javaTypeClass));
        }
        return owl2Datatype;
    }

    private static Map<Class<?>, OWL2Datatype> buildClassMap() {
        Map<Class<?>, OWL2Datatype> types = new HashMap<>();
        types.put(Integer.TYPE, OWL2Datatype.XSD_INTEGER);
        types.put(int.class, OWL2Datatype.XSD_INT);
        types.put(Double.TYPE, OWL2Datatype.XSD_DOUBLE);
        types.put(double.class, OWL2Datatype.XSD_FLOAT);
        types.put(Float.TYPE, OWL2Datatype.XSD_DOUBLE);
        types.put(float.class, OWL2Datatype.XSD_FLOAT);
        types.put(Boolean.TYPE, OWL2Datatype.XSD_BOOLEAN);
        types.put(boolean.class, OWL2Datatype.XSD_BOOLEAN);
        types.put(Long.TYPE, OWL2Datatype.XSD_LONG);
        types.put(long.class, OWL2Datatype.XSD_LONG);
        types.put(String.class, OWL2Datatype.XSD_STRING);
        types.put(LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME);

        return types;
    }

    static Optional<Object> accessMethodValue(Method classMethod, Object inputObject) {
        @Nullable Object castReturn = null;
        try {
            final Class<?> returnType = parsePrimitiveClass(classMethod.getReturnType());
            final Object invokedObject;
            invokedObject = classMethod.invoke(inputObject);
            logger.debug("Method {} has return type {}", classMethod.getName(), returnType);
            castReturn = returnType.cast(invokedObject);
        } catch (IllegalAccessException e) {
            logger.debug("Cannot access method {}", classMethod.getName(), e);
        } catch (InvocationTargetException e) {
            logger.error("Invocation failed on method {}", classMethod.getName(), e);
        } catch (ClassCastException e) {
            logger.error("Cannot cast method", e);
        }

        if (castReturn == null) {
            return Optional.empty();
        }

        return Optional.of(castReturn);
    }

    private static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            logger.debug("Converting primitive type {} to object", returnClass.getTypeName());
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return boolean.class;
                }
                default: {
                    throw new RuntimeException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }
}
