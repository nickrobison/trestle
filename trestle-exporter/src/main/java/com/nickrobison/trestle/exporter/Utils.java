package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.Geometry;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("Duplicates")
class Utils {

    private Utils() {
//        Not used
    }

    private static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
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
                    throw new IllegalArgumentException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }

    /**
     * Parse the incoming types to make sure they're valid inputs for the DBF file
     *
     * @param typeClass - Java {@link Class} to verify
     * @return - {@link Class} to safely cast to
     */
    static Class<?> parseShapefileClass(Class<?> typeClass) {
        if (typeClass.isPrimitive()) {
            return parsePrimitiveClass(typeClass);
        }

//        Check for supported DBF types
        if ((typeClass == Integer.class) || (typeClass == Short.class) || (typeClass == Byte.class)) {
            return typeClass;
        } else if (typeClass == Long.class) {
            return typeClass;
        } else if (typeClass == BigInteger.class) {
            return typeClass;
        } else if (Number.class.isAssignableFrom(typeClass)) {
            return typeClass;
        } else if (java.util.Date.class.isAssignableFrom(typeClass)) {
            return typeClass;
        } else if (typeClass == Boolean.class) {
            return typeClass;
        } else if (CharSequence.class.isAssignableFrom(typeClass) || typeClass == UUID.class) {
            return typeClass;
        } else if (Geometry.class.isAssignableFrom(typeClass)) {
            return typeClass;
        } else if (typeClass == byte[].class) {
            return typeClass;
        } else if (String.class.isAssignableFrom(typeClass)) {
//            When in doubt, return a string class
            return typeClass;
        } else {
            return String.class;
        }
    }
}
