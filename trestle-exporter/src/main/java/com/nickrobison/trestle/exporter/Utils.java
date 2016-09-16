package com.nickrobison.trestle.exporter;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("Duplicates")
public class Utils {

    static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return boolean.class;
                }
                case "long": {
                    return Long.class;
                }
                default: {
                    throw new RuntimeException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }
}
