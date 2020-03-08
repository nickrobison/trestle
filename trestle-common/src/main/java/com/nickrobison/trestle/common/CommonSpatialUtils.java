package com.nickrobison.trestle.common;

import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nickrobison on 3/7/18.
 */
public class CommonSpatialUtils {
    //  I think this Regex works. It first looks for the '<uri://' pattern.
    //  Then it skips ahead until it comes to the last part of the URI, filters out optional authorities (such as EPSG or AUTO) and grabs the SRID
    // Finally, it grabs everything that's left (after the space) and tries to use that as the WKT value
    // If it can't find the URI, then it tries to match against and WKT value
    // We don't really do WKT validation here, we leave that to the spatial libraries, but we do check that there aren't any invalid starting characters,
    // such as numbers or brackets.
    public static final Pattern wktRegex = Pattern.compile("(?:<[a-z]+://[^ ]*/(?:[A-Z]{3,4})?([0-9A-Z]+)>\\s?)?([a-zA-Z]+\\s?\\(.*)");

    /**
     * Parses a Spatial WKT value and extracts the WKT value and drops the CRS URI
     * If the regex doesn't match against anything, it returns the literal as is
     *
     * @param literal - {@link String} literal to parse
     * @return - {@link String} WKT representation
     */
    public static String getWKTFromLiteral(String literal) {
        final Matcher matcher = wktRegex.matcher(literal);
        if (matcher.matches()) {
            final String wktGroup = matcher.group(2);
            if (wktGroup != null) {
                return wktGroup;
            }
        }
//        If we don't match on anything, assume that the literal is correct
        return literal;
    }

    /**
     * Parses a spatial WKT value value and extracts the projection value
     *
     * @param literal - {@link String} literal to parse
     * @return - {@link Integer} projection
     */
    public static int getProjectionFromLiteral(String literal) {
        final Matcher matcher = wktRegex.matcher(literal);
        if (matcher.matches()) {
            final String wktGroup = matcher.group(1);
            if (wktGroup != null) {
                return Integer.valueOf(wktGroup);
            }
        }
        throw new TrestleInvalidDataException("Cannot get WKT projection", literal);
    }
}
