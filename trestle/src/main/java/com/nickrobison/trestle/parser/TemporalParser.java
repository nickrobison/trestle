package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.*;

/**
 * Created by nrobison on 7/22/16.
 */
public class TemporalParser {

    private static final Logger logger = LoggerFactory.getLogger(TemporalParser.class);


    public static boolean IsDefault(Class<?> clazz) {

        final Optional<Method> method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DefaultTemporalProperty.class))
                .findFirst();
        if (method.isPresent()) {
            return true;
        }

        final Optional<Field> field = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(DefaultTemporalProperty.class))
                .findFirst();

        if (field.isPresent()) {
            return true;
        }

        return false;
    }

    public static @Nullable Class<? extends Temporal> GetTemporalType(Class<?> clazz) {

        final Optional<Field> first = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> (f.isAnnotationPresent(DefaultTemporalProperty.class) | f.isAnnotationPresent(StartTemporalProperty.class) | f.isAnnotationPresent(EndTemporalProperty.class)))
                .filter(f -> Temporal.class.isAssignableFrom(f.getType()))
                .findFirst();

        if (first.isPresent()) {
            return (Class<? extends Temporal>) first.get().getType();
        }

        final Optional<Method> method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> (m.isAnnotationPresent(DefaultTemporalProperty.class) | m.isAnnotationPresent(StartTemporalProperty.class) | m.isAnnotationPresent(EndTemporalProperty.class)))
                .filter(m -> Temporal.class.isAssignableFrom(m.getReturnType()))
                .findFirst();
        if (method.isPresent()) {
            return (Class<? extends Temporal>) method.get().getReturnType();
        }

        return null;

    }

    public static Optional<List<TemporalObject>> GetTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<TemporalObject> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);

//        Fields
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(DefaultTemporalProperty.class)) {
                final @NonNull DefaultTemporalProperty annotation = classField.getAnnotation(DefaultTemporalProperty.class);
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

                    default:
                        throw new RuntimeException("Cannot initialize temporal object");
                }

                if (temporalObject != null) {
                    temporalObjects.add(temporalObject);
                }
            }
        }

//        Methods
        for (Method classMethod : clazz.getDeclaredMethods()) {
            if (classMethod.isAnnotationPresent(DefaultTemporalProperty.class)) {

                final @NonNull DefaultTemporalProperty annotation = classMethod.getAnnotation(DefaultTemporalProperty.class);
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
                final @NonNull StartTemporalProperty annotation = classMethod.getAnnotation(StartTemporalProperty.class);
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

        final TemporalObject temporalObject;

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

                    default:
                        throw new RuntimeException("Cannot initialize temporal object");
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
                            temporalObject = TemporalObjectBuilder.valid().from(from).to(to).isDefault(true).withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.valid().from(from).isDefault(true).withRelations(owlNamedIndividual);
                        }
                        break;
                    }

                    case EXISTS: {
                        if (to != null) {
                            temporalObject = TemporalObjectBuilder.exists().from(from).to(to).isDefault(true).withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.exists().from(from).isDefault(true).withRelations(owlNamedIndividual);
                        }
                        break;
                    }

                    default:
                        throw new RuntimeException("Cannot initialize temporal object");
                }
                break;
            }

            default:
                throw new RuntimeException("Cannot initialize temporal object");
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

                    default:
                        throw new RuntimeException("Not sure what to access");

                }

                if ((endingFieldValue != null) && !(endingFieldValue instanceof Temporal)) {
                    throw new RuntimeException("Not a temporal field");
                }

                temporalObject = buildIntervalTemporal((Temporal) fieldValue, (Temporal) endingFieldValue, annotation.scope(), owlNamedIndividual);
                break;
            }

            default:
                throw new RuntimeException("Cannot initialize temporal object");
        }

        if (temporalObject == null) {
            return Optional.empty();
        }

        return Optional.of(temporalObject);

    }

    private static TemporalObject buildIntervalTemporal(Temporal start, @Nullable Temporal end, TemporalScope scope, OWLNamedIndividual... relations) {
//        We store all temporals as datetimes, so we need to convert them.
//        final Temporal from = start;
//        final LocalDateTime from = parseTemporalToLocalDateTime(start);

        if (scope == TemporalScope.VALID) {
            final IntervalTemporal.Builder validBuilder = TemporalObjectBuilder.valid().from(start);
            if (end != null) {
//                final LocalDateTime to = parseTemporalToLocalDateTime(end);
                return validBuilder.to(end).withRelations(relations);
            }

            return validBuilder.withRelations(relations);
        } else {
            final IntervalTemporal.Builder existsBuilder = TemporalObjectBuilder.exists().from(start);
            if (end != null) {
//                final LocalDateTime to = parseTemporalToLocalDateTime(end);
                return existsBuilder.to(end).withRelations(relations);
            }

            return existsBuilder.withRelations(relations);
        }
    }

    private static LocalDateTime parseTemporalToLocalDateTime(Temporal temporal) {
        if (temporal instanceof LocalDateTime) {
            return (LocalDateTime) temporal;
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay();
        } else {
//            Need to add and subtract some time in order to get a correct LocalDateTime from LocalDate
            return LocalDateTime.from(temporal).plusSeconds(1).minusSeconds(1);
        }
    }

    public static Temporal parseToTemporal(OWLLiteral literal, Class<? extends Temporal> destinationType) {
        final Temporal parsedTemporal;
//        switch (literal.getDatatype().getBuiltInDatatype()) {
        final OWLDatatype datatype = literal.getDatatype();
        if (datatype.getIRI().equals(dateTimeDatatypeIRI)) {
//            case XSD_DATE_TIME: {
            switch (destinationType.getTypeName()) {
                case "java.time.LocalDateTime": {
                    parsedTemporal = LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE_TIME);
                    break;
                }
                case "java.time.LocalDate": {
                    parsedTemporal = LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
                    break;
                }
                default: {
                    logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype().getBuiltInDatatype(), destinationType.getTypeName());
                    throw new RuntimeException(String.format("Unsupported parsing of temporal %s to %s", literal.getDatatype().getBuiltInDatatype(), destinationType.getTypeName()));
                }
            }
        } else if (datatype.getIRI().equals(dateDatatypeIRI)) {
            switch (destinationType.getTypeName()) {
                case "java.time.LocalDateTime": {
                    parsedTemporal = LocalDateTime.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE);
                    break;
                }
                case "java.time.LocalDate": {
                    parsedTemporal = LocalDate.parse(literal.getLiteral(), DateTimeFormatter.ISO_DATE);
                    break;
                }
                default: {
                    logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype().getBuiltInDatatype(), destinationType.getTypeName());
                    throw new RuntimeException(String.format("Unsupported parsing of temporal %s to %s", literal.getDatatype().getBuiltInDatatype(), destinationType.getTypeName()));
                }
            }
        } else {
            logger.error("Unsupported parsing of XSD type {}", datatype);
            throw new RuntimeException(String.format("Unsupported parsing of XSD type %s", datatype));
        }
//                break;
//            }
//            default: {
//            }
//        }

        return parsedTemporal;
    }

    private static TemporalObject buildPointTemporal(Temporal pointTemporal, TemporalScope scope, OWLNamedIndividual... relations) {
//        final LocalDateTime at = parseTemporalToLocalDateTime(pointTemporal);

        if (scope == TemporalScope.VALID) {
            return TemporalObjectBuilder.valid().at(pointTemporal).withRelations(relations);
        } else {
            return TemporalObjectBuilder.exists().at(pointTemporal).withRelations(relations);
        }
    }
}
