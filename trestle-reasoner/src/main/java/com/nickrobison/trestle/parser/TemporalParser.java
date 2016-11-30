package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.parser.ClassParser.dfStatic;
import static com.nickrobison.trestle.parser.ClassParser.filterMethodName;
import static com.nickrobison.trestle.parser.temporal.JavaTimeParser.parseDateTimeToJavaTemporal;
import static com.nickrobison.trestle.parser.temporal.JodaTimeParser.parseDateTimeToJodaTemporal;

/**
 * Created by nrobison on 7/22/16.
 */
public class TemporalParser {

    private static final Logger logger = LoggerFactory.getLogger(TemporalParser.class);

    /**
     * Enum to determine if the temporal represents the start or the end of a period.
     * We use this to set dates to start/end of day for storing as xsd:dateTime in the ontology.
     */
    public enum IntervalType {
        START,
        END
    }

    private final ClassParser cp;

    TemporalParser(ClassParser cp) {
        this.cp = cp;
    }

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

        return field.map(f -> true).orElse(false);

    }

    /**
     * Extract the time zone from StartTemporalProperty
     * Returns ZoneOffset.UTC if the time zone isn't defined.
     * @param clazz - Class to parse
     * @return - ZoneId of either declared timezone, or UTC
     */
    public static ZoneId GetStartZoneID(@Nullable Class<?> clazz) {

        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (IsDefault(clazz)) {
            return GetDefaultZoneID(clazz);
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(StartTemporalProperty.class))
                .findAny();

        if (startMethod.isPresent()) {
            final StartTemporalProperty startAnnotation = startMethod.get().getAnnotation(StartTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(StartTemporalProperty.class))
                .findAny();

        if (startField.isPresent()) {
            final StartTemporalProperty startAnnotation = startField.get().getAnnotation(StartTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new RuntimeException(String.format("Unable to extract temporal from %s", clazz.getSimpleName()));
    }

    /**
     * Extract the time zone from EndTemporalProperty
     * Returns ZoneOffset.UTC if the time zone isn't defined.
     * @param clazz - Class to parse
     * @return - ZoneId of either declared timezone, or UTC
     */
    public static ZoneId GetEndZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (IsDefault(clazz)) {
            return GetDefaultZoneID(clazz);
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EndTemporalProperty.class))
                .findAny();

        if (startMethod.isPresent()) {
            final EndTemporalProperty startAnnotation = startMethod.get().getAnnotation(EndTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(EndTemporalProperty.class))
                .findAny();

        if (startField.isPresent()) {
            final EndTemporalProperty startAnnotation = startField.get().getAnnotation(EndTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new RuntimeException(String.format("Unable to extract temporal from %s", clazz.getClass().getSimpleName()));
    }

    /**
     * Get the Zone
     * @param clazz
     * @return
     */
    public static ZoneId GetAtZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (IsDefault(clazz)) {
            return GetDefaultZoneID(clazz);
        } else {
            return GetStartZoneID(clazz);
        }
    }

    /**
     * Extract the time zone from DefaultTemporalProperty
     * Returns ZoneOffset.UTC if the time zone isn't defined.
     * @param clazz - Class to parse
     * @return - ZoneId of either declared timezone, or UTC
     */
    public static ZoneId GetDefaultZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DefaultTemporalProperty.class))
                .findAny();

        if (startMethod.isPresent()) {
            final DefaultTemporalProperty startAnnotation = startMethod.get().getAnnotation(DefaultTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(DefaultTemporalProperty.class))
                .findAny();

        if (startField.isPresent()) {
            final DefaultTemporalProperty startAnnotation = startField.get().getAnnotation(DefaultTemporalProperty.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new RuntimeException(String.format("Unable to extract temporal from %s", clazz.getClass().getSimpleName()));
    }

    private static ZoneId extractZoneIdFromTemporalProperty(String timeZone) {
        if (timeZone.equals("")) {
            return ZoneOffset.UTC;
        } else {
            return ZoneId.of(timeZone);
        }
    }

    //    TODO(nrobison): This looks gross, fix it.
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

        return method.map(m -> (Class<? extends Temporal>) m.getReturnType()).orElse(null);

    }

    /**
     * Extract temporal properties as OWLDataProperties from a given Java class
     * Does not return any values, just the property definitions
     * @param clazz - Input class to parse properties from
     * @return - Optional List of OWLDataProperties
     */
    public Optional<List<OWLDataProperty>> GetTemporalsAsDataProperties(Class<?> clazz) {

        final OWLNamedIndividual owlNamedIndividual = cp.GetIndividual(clazz);
        List<OWLDataProperty> temporalProperties = new ArrayList<>();

        if (IsDefault(clazz)) {
//            Try to find the default method or field
            final Optional<Method> defaultMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DefaultTemporalProperty.class))
                    .findFirst();

            if (defaultMethod.isPresent()) {
                Method method = defaultMethod.get();
                if (method.getAnnotation(DefaultTemporalProperty.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(DefaultTemporalProperty.class).name())));
                }
            } else {
                final Optional<Field> defaultField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(DefaultTemporalProperty.class))
                        .findFirst();

                if (defaultField.isPresent()) {
                    Field field = defaultField.get();
                    if (field.getAnnotation(DefaultTemporalProperty.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(DefaultTemporalProperty.class).name())));
                    }
                }
            }
        } else {
//            Try for start temporal

//            Methods
            final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(StartTemporalProperty.class))
                    .findFirst();

            if (startMethod.isPresent()) {
                Method method = startMethod.get();
                if (method.getAnnotation(StartTemporalProperty.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(StartTemporalProperty.class).name())));
                }
            } else {
//                Fields
                final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(StartTemporalProperty.class))
                        .findFirst();

                if (startField.isPresent()) {
                    Field field = startField.get();
                    if (field.getAnnotation(StartTemporalProperty.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(StartTemporalProperty.class).name())));
                    }
                }
            }

//            End temporal

            final Optional<Method> endMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(EndTemporalProperty.class))
                    .findFirst();

            if (endMethod.isPresent()) {
                Method method = endMethod.get();
                if (method.getAnnotation(EndTemporalProperty.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(EndTemporalProperty.class).name())));
                }
            } else {
                final Optional<Field> endField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(EndTemporalProperty.class))
                        .findFirst();

                if (endField.isPresent()) {
                    Field field = endField.get();
                    if (field.getAnnotation(EndTemporalProperty.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(EndTemporalProperty.class).name())));
                    }
                }
            }
        }

        if (temporalProperties.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(temporalProperties);
    }

    public Optional<List<TemporalObject>> GetTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<TemporalObject> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = cp.GetIndividual(inputObject);

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
                temporalObject.ifPresent(temporalObjects::add);
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
                        temporalObject = buildPointTemporal((Temporal) fieldValue, annotation.scope(), annotation.timeZone(), owlNamedIndividual);
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

                    temporalObject.ifPresent(temporalObjects::add);
                }
            } else if (classMethod.isAnnotationPresent(StartTemporalProperty.class)) {
                final @NonNull StartTemporalProperty annotation = classMethod.getAnnotation(StartTemporalProperty.class);
                final Optional<Object> methodValue = ClassParser.accessMethodValue(classMethod, inputObject);

                final Optional<TemporalObject> temporalObject = parseStartTemporal(annotation, methodValue.orElseThrow(RuntimeException::new), owlNamedIndividual, inputObject, ClassParser.AccessType.METHOD, clazz);

                temporalObject.ifPresent(temporalObjects::add);
            }
        }

        if (temporalObjects.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(temporalObjects);
    }

//    TODO(nrobison): Get the timezone from the temporal, if it supports it.
    private static Optional<TemporalObject> parseDefaultTemporal(Object fieldValue, DefaultTemporalProperty annotation, OWLNamedIndividual owlNamedIndividual) {

        final TemporalObject temporalObject;

        switch (annotation.type()) {
            case POINT: {
                switch (annotation.scope()) {
                    case VALID: {
                        temporalObject = TemporalObjectBuilder.valid()
                                .at((Temporal) fieldValue)
                                .withTimeZone(annotation.timeZone())
                                .withRelations(owlNamedIndividual);
                        break;
                    }
                    case EXISTS: {
                        temporalObject = TemporalObjectBuilder.exists()
                                .at((Temporal) fieldValue)
                                .withTimeZone(annotation.timeZone())
                                .withRelations(owlNamedIndividual);
                        break;
                    }

                    default:
                        throw new RuntimeException("Cannot initialize temporal object");
                }
                break;
            }

            case INTERVAL: {
//                final LocalDateTime from = LocalDateTime.from((Temporal) fieldValue);
                final Temporal from = (Temporal) fieldValue;
                @Nullable Temporal to = null;
                if (annotation.duration() > 0) {
                    to = from.plus(annotation.duration(), annotation.unit());
                }
                switch (annotation.scope()) {
                    case VALID: {
                        if (to != null) {
                            temporalObject = TemporalObjectBuilder.valid()
                                    .from(from)
                                    .to(to)
                                    .withFromTimeZone(annotation.timeZone())
                                    .isDefault(true)
                                    .withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.valid()
                                    .from(from)
                                    .withFromTimeZone(annotation.timeZone())
                                    .isDefault(true)
                                    .withRelations(owlNamedIndividual);
                        }
                        break;
                    }

                    case EXISTS: {
                        if (to != null) {
                            temporalObject = TemporalObjectBuilder.exists()
                                    .from(from)
                                    .to(to)
                                    .isDefault(true)
                                    .withFromTimeZone(annotation.timeZone())
                                    .withRelations(owlNamedIndividual);
                        } else {
                            temporalObject = TemporalObjectBuilder.exists()
                                    .from(from)
                                    .isDefault(true)
                                    .withFromTimeZone(annotation.timeZone())
                                    .withRelations(owlNamedIndividual);
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

//    TODO(nrobison): Extract the time zone from the temporal, if it supports it.
    private static Optional<TemporalObject> parseStartTemporal(StartTemporalProperty annotation, Object fieldValue, OWLNamedIndividual owlNamedIndividual, Object inputObject, ClassParser.AccessType access, Class clazz) {
//        @Nullable final TemporalObject temporalObject;
        switch (annotation.type()) {
            case POINT: {
                return Optional.of(buildPointTemporal((Temporal) fieldValue, annotation.scope(), annotation.timeZone(), owlNamedIndividual));
            }
            case INTERVAL: {

//                Find matching end value
//                Start with methods
                final Optional<Method> endMethod = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(EndTemporalProperty.class))
                        .findFirst();

                if (endMethod.isPresent()) {
                    final Optional<Object> accessMethodValue = ClassParser.accessMethodValue(endMethod.get(), inputObject);
                    if (accessMethodValue.isPresent()) {
                        final String timeZone = endMethod.get().getAnnotation(EndTemporalProperty.class).timeZone();
                        return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), (Temporal) accessMethodValue.get(), timeZone, annotation.scope(), owlNamedIndividual));
                    }
                }

//                Now the fields
                final Optional<Field> endField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(EndTemporalProperty.class))
                        .findFirst();

                if (endField.isPresent()) {
                    try {
                        final Object endFieldValue = endField.get().get(inputObject);
                        String endZoneID = endField.get().getAnnotation(EndTemporalProperty.class).timeZone();
                        return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), (Temporal) endFieldValue, endZoneID, annotation.scope(), owlNamedIndividual));
                    } catch (IllegalAccessException e) {
                        logger.debug("Cannot access field {}", endField.get().getName(), e);
                    }
                } else {
                    return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), null, null, annotation.scope(), owlNamedIndividual));
                }
                break;
            }
            default:
                throw new RuntimeException("Cannot initialize temporal object");
        }
        return Optional.empty();

    }

    private static TemporalObject buildIntervalTemporal(Temporal start, String startZoneID, @Nullable Temporal end, @Nullable String endZoneID, TemporalScope scope, OWLNamedIndividual... relations) {
//        We store all temporals as datetimes, so we need to convert them.
//        final Temporal from = start;
//        final LocalDateTime from = parseTemporalToOntologyDateTime(start);

        if (scope == TemporalScope.VALID) {
            final IntervalTemporal.Builder validBuilder = TemporalObjectBuilder.valid().from(start).withFromTimeZone(startZoneID);
            if (end != null && endZoneID != null) {
//                final LocalDateTime to = parseTemporalToOntologyDateTime(end);
                return validBuilder.to(end).withToTimeZone(endZoneID).withRelations(relations);
            }

            return validBuilder.withRelations(relations);
        } else {
            final IntervalTemporal.Builder existsBuilder = TemporalObjectBuilder.exists().from(start).withFromTimeZone(startZoneID);
            if (end != null && endZoneID != null) {
//                final LocalDateTime to = parseTemporalToOntologyDateTime(end);
                return existsBuilder.to(end).withToTimeZone(endZoneID).withRelations(relations);
            }

            return existsBuilder.withRelations(relations);
        }
    }

    /**
     * Parse generic temporal to time unit utilized by ontology.
     * Currently OffsetDateTime, but that could change.
     * If we're given a date, we take the offset from either the start or end of the day, depending on the IntervalType parameter
     *
     * @param temporal     - Temporal to parse to ontology storage format
     * @param intervalType - Whether to extract the time from the date object at the start or end of the day.
     * @param zoneId       - ZoneId of given temporal
     * @return - OffsetDateTime to store in ontology
     */
//    TODO(nrobison): Is this the best way to handle temporal parsing? Should the zones be different?
//    TODO(nrobison): Add Joda time support
    public static OffsetDateTime parseTemporalToOntologyDateTime(Temporal temporal, IntervalType intervalType, ZoneId zoneId) {

        if (temporal instanceof LocalDateTime) {
            final LocalDateTime ldt = (LocalDateTime) temporal;
            final ZoneOffset zoneOffset = zoneId.getRules().getOffset(ldt);
            return OffsetDateTime.of(ldt, zoneOffset);
        } else if (temporal instanceof LocalDate) {
            if (intervalType == IntervalType.START) {
                final LocalDateTime startOfDay = ((LocalDate) temporal).atStartOfDay();
                final ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfDay);
                return OffsetDateTime.of(startOfDay, zoneOffset);
            } else {
                final LocalDateTime endOfDay = ((LocalDate) temporal).atTime(23, 59, 59, 999999999);
                final ZoneOffset zoneOffset = zoneId.getRules().getOffset(endOfDay);
                return OffsetDateTime.of(endOfDay, zoneOffset);
            }
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toOffsetDateTime();
        } else if (temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        } else {
            throw new RuntimeException(String.format("Unsupported date class %s", temporal.getClass().getName()));
        }
    }

    /**
     * Parse an OWL Literal to a given Java temporal
     * @param literal - OWL Literal to parse
     * @param destinationType - Java destination temporal type
     * @return - Java temporal
     */
    public static Temporal parseToTemporal(OWLLiteral literal, Class<? extends Temporal> destinationType) {
        return parseToTemporal(literal, destinationType, ZoneOffset.UTC);
    }

    /**
     * Parse an OWL Literal to a given Java temporal
     * @param literal - OWL Literal to parse
     * @param destinationType - Java destination temporal type
     * @param timeZone - Zone ID to adjust temporal to
     * @return - Java temporal (adjusted to the given timezone
     */
    public static Temporal parseToTemporal(OWLLiteral literal, Class<? extends Temporal> destinationType, ZoneId timeZone) {
        final Temporal parsedTemporal;
        final OWLDatatype datatype = literal.getDatatype();
        if (datatype.getIRI().equals(dateTimeDatatypeIRI)) {
            if (destinationType.getTypeName().contains("java.time")) {
                final Optional<Temporal> optionalJavaTemporal = parseDateTimeToJavaTemporal(destinationType.getTypeName(), literal, timeZone);
                parsedTemporal = optionalJavaTemporal.orElseThrow(() -> new RuntimeException(String.format("Unsupported parsing of temporal %s to %s", literal.getDatatype(), destinationType.getTypeName())));
            } else if (destinationType.getTypeName().contains("org.joda.time")) {
                Optional<Temporal> optionalJodaTemporal = parseDateTimeToJodaTemporal(destinationType.getTypeName(), literal);
                parsedTemporal = optionalJodaTemporal.orElseThrow(() -> new RuntimeException(String.format("Unsupported parsing of temporal %s to %s", literal.getDatatype(), destinationType.getTypeName())));
            } else {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationType.getTypeName());
                throw new RuntimeException(String.format("Unsupported parsing of temporal %s to %s", literal.getDatatype(), destinationType.getTypeName()));
            }
        } else {
            logger.error("Unsupported parsing of XSD type {}", datatype);
            throw new RuntimeException(String.format("Unsupported parsing of XSD type %s", datatype));
        }

        return parsedTemporal;
    }

    private static TemporalObject buildPointTemporal(Temporal pointTemporal, TemporalScope scope, String zoneID, OWLNamedIndividual... relations) {
//        final LocalDateTime at = parseTemporalToOntologyDateTime(pointTemporal);

        if (scope == TemporalScope.VALID) {
            return TemporalObjectBuilder.valid().at(pointTemporal).withTimeZone(zoneID).withRelations(relations);
        } else {
            return TemporalObjectBuilder.exists().at(pointTemporal).withTimeZone(zoneID).withRelations(relations);
        }
    }
}
