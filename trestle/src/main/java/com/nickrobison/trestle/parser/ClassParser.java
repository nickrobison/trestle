package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.exceptions.MissingConstructorException;
import com.nickrobison.trestle.types.ObjectRestriction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.*;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings("initialization")
public class ClassParser {

    enum AccessType {
        FIELD,
        METHOD
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassParser.class);
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
        final IRI iri = IRI.create(PREFIX, className);
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
//                    FIXME(nrobison): This should be broken into its own function
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
        return df.getOWLNamedIndividual(IRI.create(PREFIX, identifier));
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
                & ((objectMember.getParameters().length == 0)
                & !(objectMember.getName() == "hashCode")
                & !(objectMember.getName() == "equals")
                & !(objectMember.getName() == "toString")
//                Need to filter out random ebean methods
                & !(objectMember.getName().contains("_ebean"))
                & !(objectMember.getReturnType() == void.class));
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
                    final OWLLiteral owlLiteral = df.getOWLLiteral(fieldValue, TypeConverter.getDatatypeFromAnnotation(annotation, classField.getType()));
                    axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, owlLiteral));
                } else if (classField.isAnnotationPresent(Spatial.class)) {
//                    FIXME(nrobison): Make this prefix a constant
                    final IRI iri = IRI.create("geosparql:", "asWKT");
                    final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
                    String fieldValue = null;

                    try {
                        fieldValue = classField.get(inputObject).toString();
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    final OWLDatatype wktDatatype = df.getOWLDatatype(IRI.create(GEOSPARQLPREFIX, "wktLiteral"));
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
                        final OWLLiteral owlLiteral = df.getOWLLiteral(methodValue.get().toString(), TypeConverter.getDatatypeFromAnnotation(annotation, classMethod.getReturnType()));
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
                        final OWLLiteral owlLiteral = df.getOWLLiteral(methodValue.get().toString(), TypeConverter.getDatatypeFromJavaClass(classMethod.getReturnType()));
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

    public static Optional<String> GetSpatialValue(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();

//        Methods first
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedMethod.isPresent()) {
            final Optional<Object> methodValue = accessMethodValue(matchedMethod.get(), inputObject);

            if (methodValue.isPresent()) {
                return Optional.of(methodValue.get().toString());
            }
        }

//        Now fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedField.isPresent()) {
            String fieldValue = null;
            try {
                fieldValue = matchedField.get().get(inputObject).toString();
            } catch (IllegalAccessException e) {
                logger.debug("Cannot access field {}", matchedField.get().getName(), e);
            }

            if (fieldValue != null) {
                return Optional.of(fieldValue);
            }
        }

        return Optional.empty();
    }

    static String filterMethodName(Method classMethod) {
        String name = classMethod.getName();
//        remove get and lowercase the first letter
        if (name.startsWith("get")) {
            final String firstLetter = name.substring(3,4).toLowerCase();
            final String restOfLetters = name.substring(4);
            return firstLetter + restOfLetters;
        }

        return name;
    }

    public static String matchWithClassMember(Class<?> clazz, String classMember) {
//        Check for a matching field
        Field classField = null;
        try {
            classField = clazz.getDeclaredField(classMember);
        } catch (NoSuchFieldException e) {

        }

//        See if the member directly matches an existing constructor argument
        try {
            if (ClassBuilder.isConstructorArgument(clazz, classMember, null)) {
                return classMember;
            }
        } catch (MissingConstructorException e) {
            throw new RuntimeException("Cannot get constructor", e);
        }

        if (classField == null) {
            final Optional<Field> dataField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(DataProperty.class))
                    .filter(f -> f.getAnnotation(DataProperty.class).name().equals(classMember))
                    .findFirst();

            if (dataField.isPresent()) {
                return dataField.get().getName();
            }

        } else {
            return classField.getName();
        }

//        Check for a matching method
        final Optional<String> matchingMethod = Arrays.stream(clazz.getDeclaredMethods())
                .map(ClassParser::filterMethodName)
                .filter(name -> name.equals(classMember))
                .findFirst();

        if (matchingMethod.isPresent()) {
            return matchingMethod.get();
        }

        final Optional<Method> annotatedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DataProperty.class))
                .filter(m -> m.getAnnotation(DataProperty.class).name().equals(classMember))
                .findFirst();
        if (annotatedMethod.isPresent()) {
            return filterMethodName(annotatedMethod.get());
        }

//        Spatial
        if (classMember.equals("asWKT")) {

//            Check for specified argument name
            final Optional<String> methodArgName = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Spatial.class))
                    .map(m -> m.getAnnotation(Spatial.class).name())
                    .findFirst();

            if (!methodArgName.orElse("").equals("")) {
                return methodArgName.orElse("");
            }

            final Optional<String> fieldArgName = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Spatial.class))
                    .map(f -> f.getAnnotation(Spatial.class).name())
                    .findFirst();

            if (!fieldArgName.orElse("").equals("")) {
                return  fieldArgName.orElse("");
            }

//            TODO(nrobison): I think these things can go away.
            final Optional<Field> spatialField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Spatial.class))
                    .findFirst();

            if (spatialField.isPresent()) {
                return spatialField.get().getName();
            }

            final Optional<Method> spatialMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Spatial.class))
                    .findFirst();

            if (spatialMethod.isPresent()) {
                return filterMethodName(spatialMethod.get());
            }
        }

//        Temporal
//        Default
        if (TemporalParser.IsDefault(clazz)) {
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(DefaultTemporalProperty.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
//                Check to see if we have a given temporal property
                final String annotationName = temporalField.get().getAnnotation(DefaultTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(DefaultTemporalProperty.class)))
                    .findFirst();


            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(DefaultTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return annotationName;
                }
            }
//        TODO(nrobison): This should be better. String matching is nasty.
        } else if (classMember.toLowerCase().contains("start")) {
//            Check for start/end temporal names
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(StartTemporalProperty.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
                final String annotationName = temporalField.get().getAnnotation(StartTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(StartTemporalProperty.class)))
                    .findFirst();
            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(StartTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return annotationName;
                }
            }

        } else {
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(EndTemporalProperty.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
                final String annotationName = temporalField.get().getAnnotation(EndTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(EndTemporalProperty.class)))
                    .findFirst();
            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(EndTemporalProperty.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return annotationName;
                }
            }
        }

        throw new RuntimeException("Cannot match field or method");
    }

    static Optional<Object> accessMethodValue(Method classMethod, Object inputObject) {
        @Nullable Object castReturn = null;
        try {
            final Class<?> returnType = TypeConverter.parsePrimitiveClass(classMethod.getReturnType());
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

    /**
     * Parse the name of a given field and return either the name, or the one declared in the annotation
     * @param field - Field to parse name from
     * @return - String of parsed field name
     */
    static String getFieldName(Field field) {

//        Iterate through the various annotations and figure out if we need to get an annotated values
        if (field.isAnnotationPresent(DataProperty.class)) {
            final String fieldName = field.getAnnotation(DataProperty.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(Spatial.class)) {
            final String fieldName = field.getAnnotation(Spatial.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(DefaultTemporalProperty.class)) {
            final String fieldName = field.getAnnotation(DefaultTemporalProperty.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(StartTemporalProperty.class)) {
            final String fieldName = field.getAnnotation(StartTemporalProperty.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(EndTemporalProperty.class)) {
            final String fieldName = field.getAnnotation(EndTemporalProperty.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else {
            return field.getName();
        }
    }

    /**
     * Parse the name of a given method and return either the filtered name, or the one declared in the annotation
     * @param method - Method to parse name from
     * @return - String of filtered method name
     */
    static String getMethodName(Method method) {
        //        Iterate through the various annotations and figure out if we need to get an annotated values
        if (method.isAnnotationPresent(DataProperty.class)) {
            final String methodName = method.getAnnotation(DataProperty.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(Spatial.class)) {
            final String methodName = method.getAnnotation(Spatial.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(DefaultTemporalProperty.class)) {
            final String methodName = method.getAnnotation(DefaultTemporalProperty.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(StartTemporalProperty.class)) {
            final String methodName = method.getAnnotation(StartTemporalProperty.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(EndTemporalProperty.class)) {
            final String methodName = method.getAnnotation(EndTemporalProperty.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else {
            return filterMethodName(method);
        }
    }
}
