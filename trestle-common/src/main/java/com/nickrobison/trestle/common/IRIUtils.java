package com.nickrobison.trestle.common;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 9/6/16.
 */
public class IRIUtils {

    private static final Pattern suffixRegex = Pattern.compile(".*[/#]([^(/#)].*)$");
    private static final Pattern prefixRegex = Pattern.compile(".*[/#]");

    private IRIUtils() {
//        Not used
    }

    /**
     * Determines if a given string represents a full IRI
     *
     * @param inputString - Input string to verify
     * @return - Boolean whether the String represents a full IRI
     */
    public static boolean isFullIRI(String inputString) {
        return inputString.contains("//");

    }

    /**
     * Parse an input string and return a fully expanded IRI
     * Automatically uses {@link StaticIRI#TRESTLE_PREFIX} as part of the full expansion
     *
     * @param inputString - Input string to expand
     * @return - Fully expanded IRI, using the TRESTLE_PREFIX
     */
    public static IRI parseStringToIRI(String inputString) {
        return parseStringToIRI(TRESTLE_PREFIX, inputString);
    }

    /**
     * Parse an input string and prefix and return a fully expanded IRI
     *
     * @param prefix      - Prefix to expand string with
     * @param inputString - Input string to expand
     * @return - Fully expanded IRI, using the provided prefix
     */
    public static IRI parseStringToIRI(String prefix, String inputString) {
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

    /**
     * Extract the prefix from a given {@link IRI}, everything up to, and including the last \ or #
     *
     * @param iri - {@link IRI to parse}
     * @return - String of prefix
     */
    public static String extractPrefix(IRI iri) {
        final Matcher matcher = prefixRegex.matcher(iri.toString());
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(0);
        }
        return "";
    }

    /**
     * Takes an {@link IRI} and returns the individual name from the full string
     * Returns everything after the last / or # character in the IRI, or returns an empty string
     *
     * @param iri - {@link IRI} to extract name from
     * @return - String of individual name
     */
    public static String extractTrestleIndividualName(IRI iri) {
        return extractTrestleIndividualName(iri.getIRIString());
    }

    /**
     * Takes an IRI string and returns the individual name from the full string
     * Returns everything after the last / or # character in the IRI, or returns an empty string
     *
     * @param iriString - IRI string to extract name from
     * @return - String of individual name
     */
    public static String extractTrestleIndividualName(@Nullable String iriString) {

//        If it's null, return the empty string
        if (iriString == null) {
            return "";
        }

//        If it's not expanded, return it as is
        if (!isFullIRI(iriString)) {
            return iriString;
        }
        final Matcher matcher = suffixRegex.matcher(iriString);
        if (matcher.matches() && matcher.groupCount() > 0) {
            return matcher.group(1);
        }
        return "";
    }
}
