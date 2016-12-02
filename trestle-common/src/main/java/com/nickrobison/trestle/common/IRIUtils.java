package com.nickrobison.trestle.common;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.IRI;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 9/6/16.
 */
public class IRIUtils {

    /**
     * Determines if a given string represents a full IRI
     *
     * @param inputString - Inputstring to verify
     * @return - Boolean whether the String represents a full IRI
     */
    public static boolean isFullIRI(String inputString) {
        if (inputString.contains("//")) {
            return true;
        }

        return false;
    }

    /**
     * Parse an input string and return a fully expanded IRI
     * Automatically uses the TRESTLE_PREFIX as part of the full expansion
     * @param inputString - Input string to expand
     * @return - Fully expanded IRI, using the TRESTLE_PREFIX
     */
//    TODO(nrobison): Add some sanitation processes
    public static IRI parseStringToIRI(@NonNull String inputString) {
        return parseStringToIRI(TRESTLE_PREFIX, inputString);
    }

    /**
     * Parse an input string and prefix and return a fully expanded IRI
     * @param prefix - Prefix to expand string with
     * @param inputString - Input string to expande
     * @return - Fully expanded IRI, using the provided prefix
     */
    public static IRI parseStringToIRI(@NonNull String prefix, @NonNull String inputString) {
//        Check to see if the inputString is an expanded IRI
        if (isFullIRI(inputString)) {
            return IRI.create(inputString);
        } else {
//            If we have the unexpanded base prefix, replace it and move on
            if (inputString.startsWith("trestle:")) {
                return IRI.create(TRESTLE_PREFIX, inputString.replace("trestle:", ""));
            }
            return IRI.create(prefix, inputString.replaceAll("\\s+", "_"));
        }
    }
}
