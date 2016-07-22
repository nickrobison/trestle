package com.nickrobison.trestle.common;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.ObjectRestriction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings("initialization")
public class ClassParser {

    private static enum AccessType {
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
                    identifier = classField.get(inputObject).toString();
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
                    identifier = methodValue.get().toString();
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

    static Optional<List<TemporalObject>> GetTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<TemporalObject> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = GetIndividual(inputObject);

//        Fields
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(DefaultTemporalProperty.class)) {
                final DefaultTemporalProperty annotation = classField.getAnnotation(DefaultTemporalProperty.class);
//                Try to get the value
                Object fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject);
                } catch (IllegalAccessException e) {
                    logger.debug("Cannot access field {}", classField.getName(), e);
//                    should this be here?
                    continue;
                }

                if (!(fieldValue instanceof java.time.temporal.Temporal)) {
                    throw new RuntimeException("Not a temporal field");
                }

                final Optional<TemporalObject> temporalObject = parseDefaultTemporal(fieldValue, annotation, owlNamedIndividual);



//                TODO(nrobison): All of this is gross
                if (temporalObject.isPresent()) {
                    temporalObjects.add(temporalObject.get());
                }
            } else if (classField.isAnnotationPresent(StartTemporalProperty.class)) {
                final StartTemporalProperty annotation = classField.getAnnotation(StartTemporalProperty.class);
                TemporalObject temporalObject = null;

                Object fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject);
                } catch (IllegalAccessException e) {
                    logger.debug("Cannot access field {}", classField.getName(), e);
//                    should this be here?
                    continue;
                }

                if (!(fieldValue instanceof java.time.temporal.Temporal)) {
                    throw new RuntimeException("Not a temporal field");
                }

                switch (annotation.type()) {
                    case POINT: {
                        temporalObject = buildPointTemporal((Temporal) fieldValue, annotation.scope(), owlNamedIndividual);
                        break;
                    }
                    case INTERVAL: {
//                        Find its matching end field
                        final Optional<TemporalObject> temporalObject1 = parseStartTemporal(annotation, fieldValue, owlNamedIndividual, inputObject, AccessType.FIELD, clazz);
                        if (temporalObject1.isPresent()) {
                            temporalObject = temporalObject1.get();
                        }
//                        Object endingFieldValue = null;
//                        for (Field endingField : clazz.getDeclaredFields()) {
//
//                            if (endingField.isAnnotationPresent(EndTemporalProperty.class)) {
//                                try {
//                                    endingFieldValue = endingField.get(inputObject);
//                                } catch (IllegalAccessException e) {
//                                    logger.debug("Cannot access field {}", classField.getName(), e);
//                                    continue;
//                                }
//                            }
//                        }
//
//                        if ((endingFieldValue != null ) && !(endingFieldValue instanceof java.time.temporal.Temporal)) {
//                            throw new RuntimeException("Not a temporal field");
//                        }
//
//                        temporalObject = buildIntervalTemporal((Temporal) fieldValue, (Temporal) endingFieldValue, annotation.scope(), owlNamedIndividual);
                        break;
                    }

                    default: throw new RuntimeException("Cannot initialize temporal object");
                }

                if (temporalObject != null) {
                    temporalObjects.add(temporalObject);
                }
            }
        }

//        Methods
        for (Method classMethod : clazz.getDeclaredMethods()) {
            if (classMethod.isAnnotationPresent(DefaultTemporalProperty.class)) {

                final DefaultTemporalProperty annotation = classMethod.getAnnotation(DefaultTemporalProperty.class);
                final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                if (methodValue.isPresent()) {

                    if (!(methodValue.get() instanceof java.time.temporal.Temporal)) {
                        throw new RuntimeException("Not a temporal return value");
                    }

                    final Optional<TemporalObject> temporalObject = parseDefaultTemporal(methodValue.get(), annotation, owlNamedIndividual);

                    if (temporalObject.isPresent()) {
                        temporalObjects.add(temporalObject.get());
                    }
                }
            } else if (classMethod.isAnnotationPresent(StartTemporalProperty.class)) {
                final StartTemporalProperty annotation = classMethod.getAnnotation(StartTemporalProperty.class);
                final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                final Optional<TemporalObject> temporalObject = parseStartTemporal(annotation, methodValue.orElseThrow(RuntimeException::new), owlNamedIndividual, inputObject, AccessType.METHOD, clazz);

                if (temporalObject.isPresent()) {
                    temporalObjects.add(temporalObject.get());
                }
            }
        }

        if (temporalObjects.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(temporalObjects);
    }


    private static Optional<TemporalObject> parseDefaultTemporal(Object fieldValue, DefaultTemporalProperty annotation, OWLNamedIndividual owlNamedIndividual) {

        final TemporalObject temporalObject;

        switch (annotation.type()) {
            case POINT: {
                switch (annotation.scope()) {
                    case VALID: {
                        temporalObject = TemporalObjectBuilder.valid().at(LocalDateTime.from((java.time.temporal.Temporal) fieldValue)).withRelations(owlNamedIndividual);
                        break;
                    }
                    case EXISTS: {
                        temporalObject = TemporalObjectBuilder.exists().at(LocalDateTime.from((java.time.temporal.Temporal) fieldValue)).withRelations(owlNamedIndividual);
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
                        temporalObject = TemporalObjectBuilder.valid().from(from).to(to).withRelations(owlNamedIndividual);
                        break;
                    }

                    case EXISTS: {
                        temporalObject = TemporalObjectBuilder.exists().from(from).to(to).withRelations(owlNamedIndividual);
                        break;
                    }

                    default: throw new RuntimeException("Cannot initialize temporal object");
                }
                break;
            }

            default: throw new RuntimeException("Cannot initialize temporal object");
        }

        if (temporalObject == null) {
            return Optional.empty();
        }

        return Optional.of(temporalObject);
    }

//    private static Optional<TemporalObject> parseStartTemporal(Object fieldValue, StartTemporalProperty annotation, OWLNamedIndividual owlNamedIndividual) {
//        TemporalObject temporalObject;
//        switch (annotation.type()) {
//            case POINT: {
//                final LocalDateTime at = LocalDateTime.from((Temporal) fieldValue);
//                switch (annotation.scope()) {
//                    case VALID: {
//                        temporalObject = TemporalObjectBuilder.valid().at(at).withRelations(owlNamedIndividual);
//                    }
//
//                    case EXISTS: {
//                        temporalObject = TemporalObjectBuilder.exists().at(at).withRelations(owlNamedIndividual);
//                    }
//
//                    default: throw new RuntimeException("Cannot initialize temporal object");
//                }
//            }
//
//            case INTERVAL: {
//                switch (annotation.scope()) {
//                    final LocalDateTime from = LocalDateTime.from((Temporal) fieldValue);
//                    case VALID: {
//                        temporalObject = TemporalObjectBuilder.valid().from(from).withRelations(owlNamedIndividual);
//                    }
//
//                    case EXISTS: {
//                        temporalObject = TemporalObjectBuilder.exists().from(from).withRelations(owlNamedIndividual);
//                    }
//
//                    default: throw new RuntimeException("Cannot initialize temporal object");
//                }
//            }
//
//            default: throw new RuntimeException("Cannot initialize temporal object");
//        }
//
//        if (temporalObject == null) {
//            return Optional.empty();
//        }
//
//        return Optional.of(temporalObject);
//    }


    private static Optional<TemporalObject> parseStartTemporal(StartTemporalProperty annotation, Object fieldValue, OWLNamedIndividual owlNamedIndividual, Object inputObject, AccessType access, Class clazz) {
        TemporalObject temporalObject;
        switch (annotation.type()) {
            case POINT: {
                temporalObject = buildPointTemporal((Temporal) fieldValue, annotation.scope(), owlNamedIndividual);
                break;
            }
            case INTERVAL: {
//                        Find its matching end field
                Object endingFieldValue = null;
                switch (access) {
                    case FIELD: {

                        for (Field endingField : clazz.getDeclaredFields()) {

                            if (endingField.isAnnotationPresent(EndTemporalProperty.class)) {
                                try {
                                    endingFieldValue = endingField.get(inputObject);
                                } catch (IllegalAccessException e) {
                                    logger.debug("Cannot access field {}", endingField.getName(), e);
                                    continue;
                                }
                            }
                        }
                        break;
                    }

                    case METHOD: {

                        for (Method endMethod : clazz.getDeclaredMethods()) {
                            if (endMethod.isAnnotationPresent(EndTemporalProperty.class)) {
                                final Optional<Object> methodValue = accessMethodValue(endMethod, inputObject);
                                endingFieldValue = methodValue.orElse(null);
                            }
                        }
                        break;
                    }

                    default: throw new RuntimeException("Not sure what to access");

                }

                if ((endingFieldValue != null ) && !(endingFieldValue instanceof java.time.temporal.Temporal)) {
                    throw new RuntimeException("Not a temporal field");
                }

                temporalObject = buildIntervalTemporal((Temporal) fieldValue, (Temporal) endingFieldValue, annotation.scope(), owlNamedIndividual);
                break;
            }

            default: throw new RuntimeException("Cannot initialize temporal object");
        }

        if (temporalObject == null) {
            return Optional.empty();
        }

        return Optional.of(temporalObject);

    }
    private static TemporalObject buildIntervalTemporal(Temporal start, @Nullable Temporal end, TemporalScope scope, OWLNamedIndividual... relations) {
        final LocalDateTime from = LocalDateTime.from(start);

        if (scope == TemporalScope.VALID) {
            final IntervalTemporal.Builder validBuilder = TemporalObjectBuilder.valid().from(from);
            if (end != null) {
                final LocalDateTime to = LocalDateTime.from(end);
                return validBuilder.to(to).withRelations(relations);
            }

            return validBuilder.withRelations(relations);
        } else {
            final IntervalTemporal.Builder existsBuilder = TemporalObjectBuilder.exists().from(from);
            if (end != null) {
                final LocalDateTime to = LocalDateTime.from(end);
                return existsBuilder.to(to).withRelations(relations);
            }

            return existsBuilder.withRelations(relations);
        }
    }

    private static TemporalObject buildPointTemporal(Temporal pointTemporal, TemporalScope scope, OWLNamedIndividual... relations) {
        final LocalDateTime at = LocalDateTime.from(pointTemporal);

        if (scope == TemporalScope.VALID) {
            return TemporalObjectBuilder.valid().at(at).withRelations(relations);
        } else {
            return TemporalObjectBuilder.exists().at(at).withRelations(relations);
        }
    }

    private static Optional<Object> accessMethodValue(Method classMethod, Object inputObject) {
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
