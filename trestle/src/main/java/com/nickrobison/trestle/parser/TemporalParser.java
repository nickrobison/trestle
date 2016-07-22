package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 7/22/16.
 */
class TemporalParser {

    private static final Logger logger = LoggerFactory.getLogger(TemporalParser.class);

    static Optional<List<TemporalObject>> GetTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<TemporalObject> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);

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
                        final Optional<TemporalObject> temporalObject1 = parseStartTemporal(annotation, fieldValue, owlNamedIndividual, inputObject, ClassParser.AccessType.FIELD, clazz);
                        if (temporalObject1.isPresent()) {
                            temporalObject = temporalObject1.get();
                        }
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
                final Optional<Object> methodValue = ClassParser.accessMethodValue(classMethod, inputObject);

                if (methodValue.isPresent()) {

                    if (!(methodValue.get() instanceof Temporal)) {
                        throw new RuntimeException("Not a temporal return value");
                    }

                    final Optional<TemporalObject> temporalObject = parseDefaultTemporal(methodValue.get(), annotation, owlNamedIndividual);

                    if (temporalObject.isPresent()) {
                        temporalObjects.add(temporalObject.get());
                    }
                }
            } else if (classMethod.isAnnotationPresent(StartTemporalProperty.class)) {
                final StartTemporalProperty annotation = classMethod.getAnnotation(StartTemporalProperty.class);
                final Optional<Object> methodValue = ClassParser.accessMethodValue(classMethod, inputObject);

                final Optional<TemporalObject> temporalObject = parseStartTemporal(annotation, methodValue.orElseThrow(RuntimeException::new), owlNamedIndividual, inputObject, ClassParser.AccessType.METHOD, clazz);

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

        @Nullable final TemporalObject temporalObject;

        switch (annotation.type()) {
            case POINT: {
                switch (annotation.scope()) {
                    case VALID: {
                        temporalObject = TemporalObjectBuilder.valid().at(LocalDateTime.from((Temporal) fieldValue)).withRelations(owlNamedIndividual);
                        break;
                    }
                    case EXISTS: {
                        temporalObject = TemporalObjectBuilder.exists().at(LocalDateTime.from((Temporal) fieldValue)).withRelations(owlNamedIndividual);
                        break;
                    }

                    default: throw new RuntimeException("Cannot initialize temporal object");
                }
                break;
            }

            case INTERVAL: {
                final LocalDateTime from = LocalDateTime.from((Temporal) fieldValue);
                @Nullable LocalDateTime to = null;
                if (annotation.duration() > 0) {
                    to = from.plus(annotation.duration(), annotation.unit());
                }
                switch (annotation.scope()) {
                    case VALID: {
                        if (to != null) {
                            temporalObject = TemporalObjectBuilder.valid().from(from).to(to).withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.valid().from(from).withRelations(owlNamedIndividual);
                        }
                        break;
                    }

                    case EXISTS: {
                        if (to != null) {
                            temporalObject = TemporalObjectBuilder.exists().from(from).to(to).withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.exists().from(from).withRelations(owlNamedIndividual);
                        }
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

    private static Optional<TemporalObject> parseStartTemporal(StartTemporalProperty annotation, Object fieldValue, OWLNamedIndividual owlNamedIndividual, Object inputObject, ClassParser.AccessType access, Class clazz) {
        @Nullable final TemporalObject temporalObject;
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
                                final Optional<Object> methodValue = ClassParser.accessMethodValue(endMethod, inputObject);
                                if (methodValue.isPresent()) {
                                    endingFieldValue = methodValue.get();
                                }
                            }
                        }
                        break;
                    }

                    default: throw new RuntimeException("Not sure what to access");

                }

                if ((endingFieldValue != null ) && !(endingFieldValue instanceof Temporal)) {
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
}
