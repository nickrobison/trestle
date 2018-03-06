package com.nickrobison.trestle.reasoner.parser;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLLiteral;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Created by nickrobison on 3/6/18.
 */
public class TypeUtils {

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
                return (T) (Object) Boolean.getBoolean(literal.getLiteral());
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

    /**
     * Convert the {@link Class} into its corresponding primitive, if it is a primitive type
     * This is required to handle the auto-boxing of the Reflection APIs
     *
     * @param returnClass - {@link Class} to parse
     * @return - Primitive {@link Class}, or original class if the input is not primitive
     */
    @SuppressWarnings({"Duplicates", "squid:S1199"})
    public static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "float": {
                    return Float.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return Boolean.class;
                }
                case "long": {
                    return Long.class;
                }
                default: {
                    throw new ClassCastException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }
}
