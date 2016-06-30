package com.nickrobison.trestle.common;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.TemporalProperty;
import com.nickrobison.trestle.types.*;
import com.nickrobison.trestle.types.temporal.Temporal;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.exists;
import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.valid;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings("initialization")
public class ClassParser {

    private static final Logger logger = LoggerFactory.getLogger(ClassParser.class);

    private ClassParser() {
    }
//
//    public static void ParseClass(Object inputObject) {
//
////        Construct the OWL classes to add
//
//
////        Get all the fields
//        final Field[] declaredFields = clazz.getDeclaredFields();
//
////        For each field, figure out if it has an annotation
//        Arrays.stream(declaredFields)
//                .forEach(field -> {
////                    Is it an object property?
//                    if (field.isAnnotationPresent(ObjectProperty.class)) {
//
//                    }
//                });
//
//
//    }

    public static OWLClass GetObjectClass(Object inputObject) {

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

    public static OWLNamedIndividual GetIndividual(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        String identifier = UUID.randomUUID().toString();
//            Loop through the fields and figure out which one has the identifier

        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(IndividualIdentifier.class)) {
                try {
//                        We only grab the first
                    identifier = classField.get(inputObject).toString();
                    break;
                } catch (IllegalAccessException e) {
                    logger.error("Cannot access field {}", classField.getName(), e);
                }
            }
        }

        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        return df.getOWLNamedIndividual(IRI.create("trestle:", identifier));
    }

    //    TODO(nrobison): Implement this
    public static Optional<List<OWLObjectProperty>> GetObjectProperties(Object inputObject) {
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

    public static Optional<List<OWLDataPropertyAssertionAxiom>> GetDataProperties(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();
        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        final List<OWLDataPropertyAssertionAxiom> axioms = new ArrayList<>();

        final OWLNamedIndividual owlNamedIndividual = GetIndividual(inputObject);

        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(Ignore.class)) {
                continue;
            } else if (classField.isAnnotationPresent(ObjectProperty.class)) {
                continue;
            } else if (classField.isAnnotationPresent(TemporalProperty.class)) {
                continue;
            } else if (classField.isAnnotationPresent(DataProperty.class)) {
                final DataProperty annotation = classField.getAnnotation(DataProperty.class);
                final IRI iri = IRI.create("trestle:", annotation.name());
                final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                String fieldValue = null;

                try {
                    fieldValue = classField.get(inputObject).toString();
                } catch (IllegalAccessException e) {
                    logger.error("Cannot access field {}", classField.getName(), e);
                    continue;
                }
                final OWLLiteral owlLiteral = df.getOWLLiteral(fieldValue, annotation.datatype());
                axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, owlLiteral));
            } else {
                final IRI iri = IRI.create("trestle:", classField.getName());
                final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                String fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject).toString();
                } catch (IllegalAccessException e) {
                    logger.error("Cannot access field {}", classField.getName(), e);
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

    public static Optional<List<Temporal>> GetTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<Temporal> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = GetIndividual(inputObject);
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(TemporalProperty.class)) {
                final TemporalProperty annotation = classField.getAnnotation(TemporalProperty.class);
//                Try to get the value
                Object fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject);
                } catch (IllegalAccessException e) {
                    logger.error("Cannot access field {}", classField.getName(), e);
//                    should this be here?
                    continue;
                }

                if (!(fieldValue instanceof java.time.temporal.Temporal)) {
                    throw new RuntimeException("Not a temporal field");
                }

//                final TemporalObject temporalObject;
                final Temporal temporalObject;

                switch (annotation.type()) {
                    case POINT: {
                      switch (annotation.scope()) {
                          case VALID: {
                              temporalObject = valid().at(LocalDateTime.from((java.time.temporal.Temporal) fieldValue)).withRelations(owlNamedIndividual);
//                              temporalObject = new TemporalObject
//                                      .Builder()
//                                      .withValidAt(LocalDateTime.from((java.time.temporal.Temporal) fieldValue))
//                                      .withTemporalOf(owlNamedIndividual)
//                                      .build();
                              break;
                          }
                          case EXISTS: {
                              temporalObject = exists().at(LocalDateTime.from((java.time.temporal.Temporal) fieldValue)).withRelations(owlNamedIndividual);
//                              temporalObject = new TemporalObject
//                                      .Builder()
//                                      .withExistsAt(LocalDateTime.from((java.time.temporal.Temporal) fieldValue))
//                                      .withTemporalOf(owlNamedIndividual)
//                                      .build();
                              break;
                          }

                          default: throw new RuntimeException("Cannot initialize temporal object");
                      }
                        break;
                    }

                    case INTERVAL: {
                        final LocalDateTime from = LocalDateTime.from((java.time.temporal.Temporal) fieldValue);
                        LocalDateTime to = null;
                        if (annotation.duration() > 0) {
                            to = from.plus(annotation.duration(), annotation.unit());
                        }
                        switch (annotation.scope()) {
                            case VALID: {
                                temporalObject = valid().from(from).to(to).withRelations(owlNamedIndividual);
//                                temporalObject = new TemporalObject
//                                        .Builder()
//                                        .withValidInterval(from, to)
//                                        .withTemporalOf(owlNamedIndividual)
//                                        .build();
                                break;
                            }

                            case EXISTS: {
                                temporalObject = exists().from(from).to(to).withRelations(owlNamedIndividual);
//                                temporalObject = new TemporalObject
//                                        .Builder()
//                                        .withExistsInterval(from, to)
//                                        .withTemporalOf(owlNamedIndividual)
//                                        .build();
                                break;
                            }

                            default: throw new RuntimeException("Cannot initialize temporal object");
                        }
                        break;
                    }

                    default: throw new RuntimeException("Cannot initialize temporal object");
                }

//                TODO(nrobison): All of this is gross
                if (temporalObject != null) {
                    temporalObjects.add(temporalObject);
                }
            }
        }

        if (temporalObjects.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(temporalObjects);
    }
}
