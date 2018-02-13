package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.common.exceptions.TrestleFormatException;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static com.nickrobison.trestle.common.StaticIRI.dateTimeDatatypeIRI;
import static com.nickrobison.trestle.common.TrestleExceptionUtils.*;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.dfStatic;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.filterMethodName;
import static com.nickrobison.trestle.reasoner.parser.temporal.JavaTimeParser.parseDateTimeToJavaTemporal;
import static com.nickrobison.trestle.reasoner.parser.temporal.JodaTimeParser.parseDateTimeToJodaTemporal;

/**
 * Created by nrobison on 7/22/16.
 */
public class TemporalParser {

    private static final Logger logger = LoggerFactory.getLogger(TemporalParser.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final String FIELD_WARNING = "Cannot access field {}";
    private static final String TEMPORAL_ERROR = "Unable to extract temporal from %s";
    private static final String UNSUPPORTED_TEMPORAL_PARSING = "Unsupported parsing of temporal %s to %s";
    private static final String TEMPORAL_INIT_ERROR = "Cannot initialize temporal object";

    /**
     * Parse a {@link Temporal} to an {@link OWLLiteral} by calling {@link TemporalParser#parseTemporalToOntologyDateTime(Temporal, ZoneId)}
     *
     * @param temporal - {@link Temporal} to parse
     * @return - {@link OWLLiteral}
     */
    public static OWLLiteral temporalToLiteral(Temporal temporal) {
        return df.getOWLLiteral(parseTemporalToOntologyDateTime(temporal, ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), OWL2Datatype.XSD_DATE_TIME);
    }

    /**
     * Enum to determine if the temporal represents the start or the end of a period.
     * We use this to set dates to start/end of day for storing as xsd:dateTime in the ontology.
     */
    public enum IntervalType {
        START,
        END
    }

    private final IClassParser cp;

    TemporalParser(IClassParser cp) {
        this.cp = cp;
    }

    public static boolean isDefault(Class<?> clazz) {

        final Optional<Method> method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DefaultTemporal.class))
                .findFirst();
        if (method.isPresent()) {
            return true;
        }

        final Optional<Field> field = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(DefaultTemporal.class))
                .findFirst();

        return field.map(f -> true).orElse(false);

    }

    /**
     * Extract the time zone from StartTemporal
     * Returns ZoneOffset.UTC if the time zone isn't defined.
     *
     * @param clazz - Class to parse
     * @return - ZoneId of either declared timezone, or UTC
     */
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public static ZoneId getStartZoneID(@Nullable Class<?> clazz) {

        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (isDefault(clazz)) {
            return getDefaultZoneID(clazz);
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(StartTemporal.class))
                .findAny();

        if (startMethod.isPresent()) {
            final StartTemporal startAnnotation = startMethod.get().getAnnotation(StartTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(StartTemporal.class))
                .findAny();

        if (startField.isPresent()) {
            final StartTemporal startAnnotation = startField.get().getAnnotation(StartTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new TrestleFormatException(String.format(TEMPORAL_ERROR, clazz.getSimpleName()));
    }

    /**
     * Extract the time zone from EndTemporal
     * Returns ZoneOffset.UTC if the time zone isn't defined.
     *
     * @param clazz - Class to parse
     * @return - ZoneId of either declared timezone, or UTC
     */
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public static ZoneId getEndZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (isDefault(clazz)) {
            return getDefaultZoneID(clazz);
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(EndTemporal.class))
                .findAny();

        if (startMethod.isPresent()) {
            final EndTemporal startAnnotation = startMethod.get().getAnnotation(EndTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(EndTemporal.class))
                .findAny();

        if (startField.isPresent()) {
            final EndTemporal startAnnotation = startField.get().getAnnotation(EndTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new TrestleFormatException(String.format(TEMPORAL_ERROR, clazz.getSimpleName()));
    }

    /**
     * Get the timezone of the temporal from the provided class definition
     * If no class is specified, return the default timezone {@link ZoneOffset#UTC}
     *
     * @param clazz - Class to parse
     * @return - {@link ZoneId} of the specified class
     */
    public static ZoneId getAtZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        if (isDefault(clazz)) {
            return getDefaultZoneID(clazz);
        } else {
            return getStartZoneID(clazz);
        }
    }

    /**
     * Extract the time zone from DefaultTemporal
     * Returns {@link ZoneOffset#UTC} if the time zone isn't defined.
     *
     * @param clazz - Class to parse
     * @return - {@link ZoneId} of either declared timezone, or {@link ZoneOffset#UTC}
     */
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public static ZoneId getDefaultZoneID(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return ZoneOffset.UTC;
        }

        final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DefaultTemporal.class))
                .findAny();

        if (startMethod.isPresent()) {
            final DefaultTemporal startAnnotation = startMethod.get().getAnnotation(DefaultTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(DefaultTemporal.class))
                .findAny();

        if (startField.isPresent()) {
            final DefaultTemporal startAnnotation = startField.get().getAnnotation(DefaultTemporal.class);
            return extractZoneIdFromTemporalProperty(startAnnotation.timeZone());
        }

        throw new TrestleFormatException(String.format(TEMPORAL_ERROR, clazz.getSimpleName()));
    }

    private static ZoneId extractZoneIdFromTemporalProperty(String timeZone) {
        if (timeZone.equals("")) {
            return ZoneOffset.UTC;
        } else {
            return ZoneId.of(timeZone);
        }
    }

    //    TODO(nrobison): This looks gross, fix it.
    public static @Nullable Class<? extends Temporal> getTemporalType(Class<?> clazz) {

        final Optional<Field> first = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> (f.isAnnotationPresent(DefaultTemporal.class) || f.isAnnotationPresent(StartTemporal.class) || f.isAnnotationPresent(EndTemporal.class)))
                .filter(f -> Temporal.class.isAssignableFrom(f.getType()))
                .findFirst();

        if (first.isPresent()) {
            return (Class<? extends Temporal>) first.get().getType();
        }

        final Optional<Method> method = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> (m.isAnnotationPresent(DefaultTemporal.class) || m.isAnnotationPresent(StartTemporal.class) || m.isAnnotationPresent(EndTemporal.class)))
                .filter(m -> Temporal.class.isAssignableFrom(m.getReturnType()))
                .findFirst();

        return method.map(m -> (Class<? extends Temporal>) m.getReturnType()).orElse(null);

    }

    /**
     * Extract temporal properties as OWLDataProperties from a given Java class
     * Does not return any values, just the property definitions
     *
     * @param clazz - Input class to parse properties from
     * @return - Optional List of OWLDataProperties
     */
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public Optional<List<OWLDataProperty>> getTemporalsAsDataProperties(Class<?> clazz) {

        List<OWLDataProperty> temporalProperties = new ArrayList<>();

        if (isDefault(clazz)) {
//            Try to find the default method or field
            final Optional<Method> defaultMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DefaultTemporal.class))
                    .findFirst();

            if (defaultMethod.isPresent()) {
                Method method = defaultMethod.get();
                if (method.getAnnotation(DefaultTemporal.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(DefaultTemporal.class).name())));
                }
            } else {
                final Optional<Field> defaultField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(DefaultTemporal.class))
                        .findFirst();

                if (defaultField.isPresent()) {
                    Field field = defaultField.get();
                    if (field.getAnnotation(DefaultTemporal.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(DefaultTemporal.class).name())));
                    }
                }
            }
        } else {
//            Try for start temporal

//            Methods
            final Optional<Method> startMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(StartTemporal.class))
                    .findFirst();

            if (startMethod.isPresent()) {
                Method method = startMethod.get();
                if (method.getAnnotation(StartTemporal.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(StartTemporal.class).name())));
                }
            } else {
//                Fields
                final Optional<Field> startField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(StartTemporal.class))
                        .findFirst();

                if (startField.isPresent()) {
                    Field field = startField.get();
                    if (field.getAnnotation(StartTemporal.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(StartTemporal.class).name())));
                    }
                }
            }

//            End temporal

            final Optional<Method> endMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(EndTemporal.class))
                    .findFirst();

            if (endMethod.isPresent()) {
                Method method = endMethod.get();
                if (method.getAnnotation(EndTemporal.class).name().equals("")) {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, filterMethodName(method))));
                } else {
                    temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, method.getAnnotation(EndTemporal.class).name())));
                }
            } else {
                final Optional<Field> endField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(EndTemporal.class))
                        .findFirst();

                if (endField.isPresent()) {
                    Field field = endField.get();
                    if (field.getAnnotation(EndTemporal.class).name().equals("")) {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getName())));
                    } else {
                        temporalProperties.add(dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, field.getAnnotation(EndTemporal.class).name())));
                    }
                }
            }
        }

        if (temporalProperties.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(temporalProperties);
    }

    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public Optional<List<TemporalObject>> getTemporalObjects(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        List<TemporalObject> temporalObjects = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = cp.getIndividual(inputObject);

//        Fields
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(DefaultTemporal.class)) {
                final DefaultTemporal annotation = classField.getAnnotation(DefaultTemporal.class);
//                Try to get the value
                Object fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject);
                } catch (IllegalAccessException e) {
                    logger.warn(FIELD_WARNING, classField.getName(), e);
//                    should this be here?
                    continue;
                }

                if (!(fieldValue instanceof java.time.temporal.Temporal)) {
                    throw new IllegalStateException("Not a temporal field");
                }

                final Optional<TemporalObject> temporalObject = parseDefaultTemporal(fieldValue, annotation);


//                TODO(nrobison): All of this is gross
                temporalObject.ifPresent(temporalObjects::add);
            } else if (classField.isAnnotationPresent(StartTemporal.class)) {
                final StartTemporal annotation = classField.getAnnotation(StartTemporal.class);
                TemporalObject temporalObject = null;

                Object fieldValue = null;
                try {
                    fieldValue = classField.get(inputObject);
                } catch (IllegalAccessException e) {
                    logger.warn(FIELD_WARNING, classField.getName(), e);
//                    should this be here?
                    continue;
                }

                if (!(fieldValue instanceof java.time.temporal.Temporal)) {
                    throw new IllegalStateException("Not a temporal field");
                }

                switch (annotation.type()) {
                    case POINT: {
                        temporalObject = buildPointTemporal((Temporal) fieldValue, annotation.scope(), annotation.timeZone());
                        break;
                    }
                    case INTERVAL: {
//                        Find its matching end field
                        final Optional<TemporalObject> parsedTemporal = parseStartTemporal(annotation, fieldValue, owlNamedIndividual, inputObject, ClassParser.AccessType.FIELD, clazz);
                        if (parsedTemporal.isPresent()) {
                            temporalObject = parsedTemporal.get();
                        }
                        break;
                    }

                    default:
                        throw new IllegalStateException(TEMPORAL_INIT_ERROR);
                }

                if (temporalObject != null) {
                    temporalObjects.add(temporalObject);
                }
            }
        }

//        Methods
        for (Method classMethod : clazz.getDeclaredMethods()) {
            if (classMethod.isAnnotationPresent(DefaultTemporal.class)) {

                final DefaultTemporal annotation = classMethod.getAnnotation(DefaultTemporal.class);
                final Optional<Object> methodValue = ClassParser.accessMethodValue(classMethod, inputObject);

                if (methodValue.isPresent()) {

                    if (!(methodValue.get() instanceof Temporal)) {
                        throw new IllegalStateException("Not a temporal return value");
                    }

                    final Optional<TemporalObject> temporalObject = parseDefaultTemporal(methodValue.get(), annotation);

                    temporalObject.ifPresent(temporalObjects::add);
                }
            } else if (classMethod.isAnnotationPresent(StartTemporal.class)) {
                final StartTemporal annotation = classMethod.getAnnotation(StartTemporal.class);
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
    private static Optional<TemporalObject> parseDefaultTemporal(Object fieldValue, @Nullable DefaultTemporal annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Missing default temporal annotation");
        }
        final TemporalObject temporalObject;

        switch (annotation.type()) {
            case POINT: {
                switch (annotation.scope()) {
                    case VALID: {
                        temporalObject = TemporalObjectBuilder.valid()
                                .at((Temporal) fieldValue)
                                .withTimeZone(annotation.timeZone())
                                .build();
//                                .withRelations(owlNamedIndividual);
                        break;
                    }
                    case EXISTS: {
                        temporalObject = TemporalObjectBuilder.exists()
                                .at((Temporal) fieldValue)
                                .withTimeZone(annotation.timeZone())
                                .build();
                        break;
                    }

                    default:
                        throw new IllegalStateException(TEMPORAL_INIT_ERROR);
                }
                break;
            }

            case INTERVAL: {
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
                                    .build();
                        } else {
                            temporalObject = TemporalObjectBuilder.valid()
                                    .from(from)
                                    .withFromTimeZone(annotation.timeZone())
                                    .isDefault(true)
                                    .build();
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
                                    .build();
                        } else {
                            temporalObject = TemporalObjectBuilder.exists()
                                    .from(from)
                                    .isDefault(true)
                                    .withFromTimeZone(annotation.timeZone())
                                    .build();
                        }
                        break;
                    }

                    default:
                        throw new IllegalStateException(TEMPORAL_INIT_ERROR);
                }
                break;
            }

            default:
                throw new IllegalStateException(TEMPORAL_INIT_ERROR);
        }

        if (temporalObject == null) {
            return Optional.empty();
        }

        return Optional.of(temporalObject);
    }

    //    TODO(nrobison): Extract the time zone from the temporal, if it supports it.
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    private static Optional<TemporalObject> parseStartTemporal(@Nullable StartTemporal annotation, Object fieldValue, OWLNamedIndividual owlNamedIndividual, Object inputObject, ClassParser.AccessType access, Class clazz) {
        if (annotation == null) {
            throw new IllegalArgumentException("Missing StartTemporal annotation");
        }

        switch (annotation.type()) {
            case POINT: {
                return Optional.of(buildPointTemporal((Temporal) fieldValue, annotation.scope(), annotation.timeZone()));
            }
            case INTERVAL: {

//                Find matching end value
//                Start with methods
                final Optional<Method> endMethod = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(EndTemporal.class))
                        .findFirst();

                if (endMethod.isPresent()) {
                    final Optional<Object> accessMethodValue = ClassParser.accessMethodValue(endMethod.get(), inputObject);
                    if (accessMethodValue.isPresent()) {
                        final String timeZone = endMethod.get().getAnnotation(EndTemporal.class).timeZone();
                        return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), (Temporal) accessMethodValue.get(), timeZone, annotation.scope()));
                    }
                }

//                Now the fields
                final Optional<Field> endField = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(EndTemporal.class))
                        .findFirst();

                if (endField.isPresent()) {
                    try {
                        final Object endFieldValue = endField.get().get(inputObject);
                        String endZoneID = endField.get().getAnnotation(EndTemporal.class).timeZone();
                        return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), (Temporal) endFieldValue, endZoneID, annotation.scope()));
                    } catch (IllegalAccessException e) {
                        logger.warn(FIELD_WARNING, endField.get().getName(), e);
                    }
                } else {
                    return Optional.of(buildIntervalTemporal((Temporal) fieldValue, annotation.timeZone(), null, null, annotation.scope()));
                }
                break;
            }
            default:
                throw new IllegalStateException(TEMPORAL_INIT_ERROR);
        }
        return Optional.empty();

    }

    private static TemporalObject buildIntervalTemporal(Temporal start, String startZoneID, @Nullable Temporal end, @Nullable String endZoneID, TemporalScope scope) {
//        We store all temporals as datetimes, so we need to convert them.
//        final Temporal from = start;
//        final LocalDateTime from = parseTemporalToOntologyDateTime(start);

        if (scope == TemporalScope.VALID) {
            final IntervalTemporal.Builder validBuilder = TemporalObjectBuilder.valid().from(start).withFromTimeZone(startZoneID);
            if (end != null && endZoneID != null) {
//                final LocalDateTime to = parseTemporalToOntologyDateTime(end);
                return validBuilder.to(end).withToTimeZone(endZoneID).build(); // .withRelations(relations);
            }

            return validBuilder.build(); //.withRelations(relations);
        } else {
            final IntervalTemporal.Builder existsBuilder = TemporalObjectBuilder.exists().from(start).withFromTimeZone(startZoneID);
            if (end != null && endZoneID != null) {
//                final LocalDateTime to = parseTemporalToOntologyDateTime(end);
                return existsBuilder.to(end).withToTimeZone(endZoneID).build(); //.withRelations(relations);
            }

            return existsBuilder.build(); //.withRelations(relations);
        }
    }

    /**
     * Parse generic temporal to time unit utilized by ontology.
     * Currently OffsetDateTime, but that could change.
     * If we're given a date, we take the offset from either the start or end of the day, depending on the IntervalType parameter
     *
     * @param temporal - Temporal to parse to ontology storage format
     * @param zoneId   - ZoneId of given temporal
     * @return - OffsetDateTime to store in ontology
     */
//    TODO(nrobison): Is this the best way to handle temporal parsing? Should the zones be different?
//    TODO(nrobison): Add Joda time support
    public static OffsetDateTime parseTemporalToOntologyDateTime(Temporal temporal, ZoneId zoneId) {

        if (temporal instanceof LocalDateTime) {
            final LocalDateTime ldt = (LocalDateTime) temporal;
            final ZoneOffset zoneOffset = zoneId.getRules().getOffset(ldt);
            return OffsetDateTime.of(ldt, zoneOffset);
        } else if (temporal instanceof LocalDate) {
            final LocalDateTime startOfDay = ((LocalDate) temporal).atStartOfDay();
            final ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfDay);
            return OffsetDateTime.of(startOfDay, zoneOffset);
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toOffsetDateTime();
        } else if (temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        } else {
            throw new IllegalArgumentException(String.format("Unsupported date class %s", temporal.getClass().getName()));
        }
    }

    /**
     * Parse an OWL Literal to a given Java temporal
     *
     * @param literal         - OWL Literal to parse
     * @param destinationType - Java destination temporal type
     * @return - Java temporal
     */
    public static Temporal parseToTemporal(OWLLiteral literal, @Nullable Class<? extends Temporal> destinationType) {
        return parseToTemporal(literal, destinationType, ZoneOffset.UTC);
    }

    /**
     * Parse an OWL Literal to a given Java temporal
     *
     * @param literal         - OWL Literal to parse
     * @param destinationType - Java destination temporal type
     * @param timeZone        - Zone ID to adjust temporal to
     * @return - Java temporal (adjusted to the given timezone
     */
    public static Temporal parseToTemporal(OWLLiteral literal, @Nullable Class<? extends Temporal> destinationType, ZoneId timeZone) {
        if (destinationType == null) {
            throw new IllegalStateException("Cannot parse temporal with null type");
        }
        final Temporal parsedTemporal;
        final OWLDatatype datatype = literal.getDatatype();
        if (datatype.getIRI().equals(dateTimeDatatypeIRI)) {
            if (destinationType.getTypeName().contains("java.time")) {
                final Optional<Temporal> optionalJavaTemporal = parseDateTimeToJavaTemporal(destinationType.getTypeName(), literal, timeZone);
                parsedTemporal = optionalJavaTemporal.orElseThrow(() -> new RuntimeException(String.format(UNSUPPORTED_TEMPORAL_PARSING, literal.getDatatype(),
                        handleNullOrEmptyString(destinationType.getTypeName()))));
            } else if (destinationType.getTypeName().contains("org.joda.time")) {
                Optional<Temporal> optionalJodaTemporal = parseDateTimeToJodaTemporal(destinationType.getTypeName(), literal);
                parsedTemporal = optionalJodaTemporal.orElseThrow(() -> new RuntimeException(String.format(UNSUPPORTED_TEMPORAL_PARSING, literal.getDatatype(),
                        handleNullOrEmptyString(destinationType.getTypeName()))));
            } else {
                logger.error("Unsupported parsing of temporal {} to {}", literal.getDatatype(), destinationType.getTypeName());
                throw new IllegalArgumentException(String.format(UNSUPPORTED_TEMPORAL_PARSING, literal.getDatatype(),
                        handleNullOrEmptyString(destinationType.getTypeName())));
            }
        } else {
            logger.error("Unsupported parsing of XSD type {}", datatype);
            throw new IllegalArgumentException(String.format("Unsupported parsing of XSD type %s", datatype));
        }

        return parsedTemporal;
    }

    private static TemporalObject buildPointTemporal(Temporal pointTemporal, TemporalScope scope, String zoneID) {
//        final LocalDateTime at = parseTemporalToOntologyDateTime(pointTemporal);

        if (scope == TemporalScope.VALID) {
            return TemporalObjectBuilder.valid().at(pointTemporal).withTimeZone(zoneID).build(); //.withRelations(relations);
        } else {
            return TemporalObjectBuilder.exists().at(pointTemporal).withTimeZone(zoneID).build(); //.withRelations(relations);
        }
    }
}
