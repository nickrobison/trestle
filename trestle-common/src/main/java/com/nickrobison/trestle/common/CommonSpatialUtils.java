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
    // Finally, it grabs everything that's left (after the space) and uses that as the WKT value
    public static final Pattern wktRegex = Pattern.compile("<[a-z]+://[^ ]*/(?:[A-Z]{3,4})?([0-9A-Z]+)>\\s(.*)");

    /**
     * Parses a Spatial WKT value and extracts the WKT value and drops the CRS URI
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
        throw new TrestleInvalidDataException("Cannot parse projected WKT value", literal);
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
        throw new TrestleInvalidDataException("Cannot parse projected WKT value", literal);
    }
}
