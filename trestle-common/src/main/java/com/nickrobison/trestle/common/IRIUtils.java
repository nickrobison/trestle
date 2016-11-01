package com.nickrobison.trestle.common;

import org.semanticweb.owlapi.model.IRI;

import static com.nickrobison.trestle.common.StaticIRI.PREFIX;

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

    public static IRI parseStringToIRI(String inputString) {
//        Check to see if the inputString is an expanded IRI
        if (isFullIRI(inputString)) {
            return IRI.create(inputString);
        } else {
//            If we have the unexpanded base prefix, replace it and move on
            if (inputString.startsWith("trestle:")) {
                return IRI.create(PREFIX, inputString.replace("trestle:", ""));
            }
            return IRI.create(PREFIX, inputString.replaceAll("\\s+", "_"));
        }
    }
}
