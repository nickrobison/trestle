package com.nickrobison.trestle.common;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nickrobison on 3/25/18.
 */
public class SpatialUtilsTest {

    @Test
    public void testSpatialRegex() {

        final String fullWKT = "<http://www.opengis.net/def/crs/EPSG/0/4326> MULTIPOLYGON((";
        final String wktWithSpace = "<http://www.opengis.net/def/crs/EPSG/0/4326> MULTIPOLYGON ((";
        final String wktNoSpace = "<http://www.opengis.net/def/crs/EPSG/0/4326>MULTIPOLYGON((";
        final String wktSomeSpace = "<http://www.opengis.net/def/crs/EPSG/0/4326>MultiPolyGON ((";
        final String CRSWKT = "<http://www.opengis.net/def/crs/OGC/1.3/CRS84> point((";
        final String noCRS = "POLYGON (())";
        final String noCRSNoSpace = "POLYGON(())";
        final String badCRS = "http;//www.opengis.net/def/crs/OGC/1.3/CRS84> POINT((";
        final String badWKT = "<http://www.opengis.net/def/crs/OGC/1.3/CRS84> POINT32((";
        final String badWKTNoCRS = "POINT32 [(";

//        Full WKT
        final Matcher wktMatches = CommonSpatialUtils.wktRegex.matcher(fullWKT);
        assertAll(() -> assertTrue(wktMatches.matches(), "Should match fullWKT"),
                () -> assertEquals("4326", wktMatches.group(1), "Should have 4326 Projection"),
                () -> assertEquals("MULTIPOLYGON((", wktMatches.group(2), "Should match WKT"));

        final Matcher withSpaceMatches = CommonSpatialUtils.wktRegex.matcher(wktWithSpace);
        assertAll(() -> assertTrue(withSpaceMatches.matches(), "Should match"),
                () -> assertEquals("4326", withSpaceMatches.group(1), "Should have 4326 Projection"),
                () -> assertEquals("MULTIPOLYGON ((", withSpaceMatches.group(2), "Should have WKT with space"));

        final Matcher noSpaceMatcher = CommonSpatialUtils.wktRegex.matcher(wktNoSpace);
        assertAll(() -> assertTrue(noSpaceMatcher.matches(), "Should match"),
                () -> assertEquals("4326", noSpaceMatcher.group(1), "Should have 4326 projection"),
                () -> assertEquals("MULTIPOLYGON((", noSpaceMatcher.group(2), "Should match with no spaces"));

        final Matcher someSpaceMatcher = CommonSpatialUtils.wktRegex.matcher(wktSomeSpace);
        assertAll(() -> assertTrue(someSpaceMatcher.matches(), "Should match"),
                () -> assertEquals("4326", someSpaceMatcher.group(1), "Should have 4326 projection"),
                () -> assertEquals("MultiPolyGON ((", someSpaceMatcher.group(2), "Should match with some spaces"));

        final Matcher crsMatcher = CommonSpatialUtils.wktRegex.matcher(CRSWKT);
        assertAll(() -> assertTrue(crsMatcher.matches(), "Should match"),
                () -> assertEquals("84", crsMatcher.group(1), "Should have CRS84 projection"),
                () -> assertEquals("point((", crsMatcher.group(2), "Should have POINT WKT"));

        final Matcher noCRSMatcher = CommonSpatialUtils.wktRegex.matcher(noCRS);
        assertAll(() -> assertTrue(noCRSMatcher.matches(), "Should match with no CRS"),
                () -> assertNull(noCRSMatcher.group(1), "Should not have Projection"),
                () -> assertEquals("POLYGON (())", noCRSMatcher.group(2), "Should have POLYGON WKT"));

        final Matcher nocrsSpaceMatcher = CommonSpatialUtils.wktRegex.matcher(noCRSNoSpace);
        assertAll(() -> assertTrue(nocrsSpaceMatcher.matches(), "Should match with no CRS"),
                () -> assertNull(nocrsSpaceMatcher.group(1), "Should not have Projection"),
                () -> assertEquals("POLYGON(())", nocrsSpaceMatcher.group(2), "Should have POLYGON WKT"));

        assertFalse(CommonSpatialUtils.wktRegex.matcher(badCRS).matches(), "Should not match with bad CRS");
        assertFalse(CommonSpatialUtils.wktRegex.matcher(badWKT).matches(), "Should not match with bad WKT");
        assertFalse(CommonSpatialUtils.wktRegex.matcher(badWKTNoCRS).matches(), "Should not match with bad WKT and no CRS");



    }
}
