package com.nickrobison.trestle.common;

/**
 * Created by nrobison on 9/6/16.
 */
public class IRIUtils {

    /**
     * Determines if a given string represents a full IRI
     * @param inputString - Inputstring to verify
     * @return - Boolean whether the String represents a full IRI
     */
    public static boolean isFullIRI(String inputString) {
        if (inputString.contains("//")) {
            return true;
        }

        return false;
    }
}
