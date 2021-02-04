package com.nickrobison.trestle.reasoner.parser;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.joda.time.DateTime;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.nickrobison.trestle.common.StaticIRI.dateDatatypeIRI;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.dfStatic;

/**
 * Created by nickrobison on 3/6/18.
 */

/**
 * Helper functions for typing conversions, that seemed easier to implement in Java than in Clojure
 * Not a lot to see, just some ugly casting through raw types
 */
public final class TypeUtils {

    private TypeUtils() {
//        Not used
    }

    /**
     * Given a {@link Class} and {@link OWLLiteral}, either return the appropriate cast object, or null, if it's not one of our built-in type
     * We're not moving this method to Clojure, because it's easier to deal with the Raw types in Java
     *
     * @param javaClass - {@link Class} to cast literal to
     * @param literal   - {@link OWLLiteral} to be cast
     * @param <T>       - {@link T} generic type parameter
     * @return - {@link T}, or null if the {@link Class} is not a built-in type
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> T rawLiteralConversion(Class<T> javaClass, @Nullable OWLLiteral literal) {
        if (literal == null) {
            throw new IllegalStateException("Cannot have null literal");
        }

        switch (javaClass.getTypeName()) {

            case "int": {
                return (T) (Object) Integer.parseInt(literal.getLiteral());
            }

            case "java.lang.Integer": {
                return javaClass.cast(Integer.parseInt(literal.getLiteral()));
            }

            case "short": {
                return (T) (Object) Short.parseShort(literal.getLiteral());
            }

            case "java.lang.Short": {
                return javaClass.cast(Short.parseShort(literal.getLiteral()));
            }

            case "long": {
                return (T) (Object) Long.parseLong(literal.getLiteral());
            }

            case "java.lang.Long": {
                return javaClass.cast(Long.parseLong(literal.getLiteral()));
            }

            case "java.time.LocalDateTime": {
                return javaClass.cast(LocalDateTime.parse(literal.getLiteral()));
            }

            case "java.time.LocalDate": {
                return javaClass.cast(LocalDate.parse(literal.getLiteral()));
            }

            case "java.time.OffsetDateTime": {
                return javaClass.cast(OffsetDateTime.parse(literal.getLiteral()));
            }

            case "java.time.ZonedDateTime": {
                return javaClass.cast(ZonedDateTime.parse(literal.getLiteral()));
            }

            case "java.lang.String": {
                return javaClass.cast(literal.getLiteral());
            }

            case "float": {
                return (T) (Object) Float.parseFloat(literal.getLiteral());
            }

            case "java.lang.Float": {
                return javaClass.cast(literal.parseFloat());
            }

            case "double": {
                return (T) (Object) Double.parseDouble(literal.getLiteral());
            }

            case "java.lang.Double": {
                return javaClass.cast(literal.parseDouble());
            }

            case "boolean": {
                return (T) (Object) Boolean.parseBoolean(literal.getLiteral());
            }

            case "java.lang.Boolean": {
                return javaClass.cast(literal.parseBoolean());
            }

            case "java.math.BigInteger": {
                return (T) new BigInteger(literal.getLiteral());
            }

            case "java.math.BigDecimal": {
                return (T) new BigDecimal(literal.getLiteral());
            }
            default: {
                return null;
            }
        }

    }

    public static Map<OWLDatatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWLDatatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic), BigInteger.class);
        datatypeMap.put(OWL2Datatype.XSD_INT.getDatatype(dfStatic), int.class);
        datatypeMap.put(OWL2Datatype.XSD_SHORT.getDatatype(dfStatic), short.class);
        datatypeMap.put(OWL2Datatype.XSD_LONG.getDatatype(dfStatic), long.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic), double.class);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic), float.class);
        datatypeMap.put(OWL2Datatype.XSD_DECIMAL.getDatatype(dfStatic), BigDecimal.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic), LocalDateTime.class);
        datatypeMap.put(dfStatic.getOWLDatatype(dateDatatypeIRI), LocalDate.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic), boolean.class);
        datatypeMap.put(OWL2Datatype.XSD_STRING.getDatatype(dfStatic), String.class);
        datatypeMap.put(OWL2Datatype.XSD_BYTE.getDatatype(dfStatic), byte.class);
        datatypeMap.put(OWL2Datatype.RDF_LANG_STRING.getDatatype(dfStatic), String.class);

        return datatypeMap;
    }

    public static Map<Class<?>, OWLDatatype> buildClassMap() {
        Map<Class<?>, OWLDatatype> types = new HashMap<>();
        types.put(Integer.class, OWL2Datatype.XSD_INT.getDatatype(dfStatic));
        types.put(int.class, OWL2Datatype.XSD_INT.getDatatype(dfStatic));
        types.put(Double.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(double.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(float.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Float.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(Long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(BigInteger.class, OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic));
        types.put(BigDecimal.class, OWL2Datatype.XSD_DECIMAL.getDatatype(dfStatic));
        types.put(short.class, OWL2Datatype.XSD_SHORT.getDatatype(dfStatic));
        types.put(Short.class, OWL2Datatype.XSD_SHORT.getDatatype(dfStatic));
        types.put(String.class, OWL2Datatype.XSD_STRING.getDatatype(dfStatic));
        types.put(byte.class, OWL2Datatype.XSD_BYTE.getDatatype(dfStatic));
        types.put(Byte.class, OWL2Datatype.XSD_BYTE.getDatatype(dfStatic));
//        Java temporals
        types.put(LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(LocalDate.class, dfStatic.getOWLDatatype(dateDatatypeIRI));
        types.put(OffsetDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(ZonedDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
//        Joda temporals
        types.put(DateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(org.joda.time.LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(org.joda.time.LocalDate.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
//        types.put(Geometry.class, dfStatic.getOWLDatatype(WKTDatatypeIRI));

        return types;
    }
}
