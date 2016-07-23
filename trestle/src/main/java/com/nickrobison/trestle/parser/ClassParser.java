package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.ObjectRestriction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private ClassParser() {
    }

    static OWLClass GetObjectClass(Object inputObject) {

        //        Get the class name, from the annotation, if possible;
        final Class<?> clazz = inputObject.getClass();
        final String className;
//        final OWLClassName declaredAnnotation = clazz.getDeclaredAnnotation(OWLClassName.class);
        if (clazz.isAnnotationPresent(OWLClassName.class)) {
            className = clazz.getDeclaredAnnotation(OWLClassName.class).className();
        } else {
            className = clazz.getName();
        }

        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        final IRI iri = IRI.create("trestle:", className);
        return df.getOWLClass(iri);
    }

    static OWLNamedIndividual GetIndividual(Object inputObject) {

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
        final OWLDataFactory df = OWLManager.getOWLDataFactory();

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

    static Optional<List<OWLDataPropertyAssertionAxiom>> GetDataProperties(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();
        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        final List<OWLDataPropertyAssertionAxiom> axioms = new ArrayList<>();

        final OWLNamedIndividual owlNamedIndividual = GetIndividual(inputObject);

        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(Ignore.class)) {
            } else if (classField.isAnnotationPresent(ObjectProperty.class)) {
            } else if (classField.isAnnotationPresent(DefaultTemporalProperty.class) | classField.isAnnotationPresent(StartTemporalProperty.class) | classField.isAnnotationPresent(EndTemporalProperty.class)) {
            } else if (classField.isAnnotationPresent(DataProperty.class) | classField.isAnnotationPresent(Spatial.class)) {
                if (classField.isAnnotationPresent(DataProperty.class)) {
                    final DataProperty annotation = classField.getAnnotation(DataProperty.class);
                    final IRI iri = IRI.create("trestle:", annotation.name());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    String fieldValue = null;

                    try {
                        fieldValue = classField.get(inputObject).toString();
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    final OWLLiteral owlLiteral = df.getOWLLiteral(fieldValue, annotation.datatype());
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
                }
            } else {
                final IRI iri = IRI.create("trestle:", classField.getName());
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

        if (axioms.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(axioms);
    }


    static Optional<Object> accessMethodValue(Method classMethod, Object inputObject) {
        @Nullable Object castReturn = null;
        try {
            final Class<?> returnType = classMethod.getReturnType();
            final Object invokedObject = classMethod.invoke(inputObject);
            logger.debug("Method {} has return type {}", classMethod.getName(), returnType);
            castReturn = returnType.cast(invokedObject);
        } catch (IllegalAccessException e) {
            logger.debug("Cannot access method {}", classMethod.getName(), e);
        } catch (InvocationTargetException e) {
            logger.error("Invocation failed on method {}", classMethod.getName(), e);
        }

        if (castReturn == null) {
            return Optional.empty();
        }

        return Optional.of(castReturn);
    }
}
