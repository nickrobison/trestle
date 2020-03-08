package com.nickrobison.trestle.reasoner.engines.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static javax.measure.unit.NonSI.FOOT;
import static javax.measure.unit.NonSI.INCH;
import static javax.measure.unit.SI.CENTIMETER;
import static javax.measure.unit.SI.KILOMETER;
import static javax.measure.unit.SI.METER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nickrobison on 3/19/18.
 */
public class SpatialUtilsTest {

    private static Geometry bufferedGeom;
    private static String wktInput;
    private static String bufferedWKT;
    private static WKTWriter wktWriter = new WKTWriter();
    public static final WKTReader WKT_READER = new WKTReader();

    @BeforeAll
    public static void setup() throws ParseException {
        wktInput =  "POLYGON((-122.374781 47.690612, -122.325515 47.690612, -122.325515 47.668884, -122.374781 47.668884, -122.374781 47.690612))";
        Geometry wktGeom = WKT_READER.read(wktInput);
        bufferedGeom = wktGeom.buffer(50);
        bufferedWKT = wktWriter.write(bufferedGeom);
    }

    @Test
    public void testBufferUnits() throws ParseException {

//        Directly compare with buffer in meters

//        Try to apply a buffer of 50 m
        final String buff_m = SpatialEngineUtils.addWKTBuffer(wktInput, 50, METER);
        assertEquals(bufferedWKT, buff_m, "Should be equal when adding 50 m");

//        Try for 5000cm
        final String buff_cm = SpatialEngineUtils.addWKTBuffer(wktInput, 5000, CENTIMETER);
        assertEquals(bufferedWKT, buff_cm, "Should be equal when adding 5000 cm");

//        Try for .05km
        final String buff_km = SpatialEngineUtils.addWKTBuffer(wktInput, .05, KILOMETER);
        assertEquals(bufferedWKT, buff_km, "Should be equal when adding .05 km");

//        Try for imperial units
//        We can't directly compare, due to rounding, but we can ensure they're almost equal

//        Try for 50m in ft
        final String buff_ft = SpatialEngineUtils.addWKTBuffer(wktInput, 164.042, FOOT);
        assertTrue(bufferedGeom.equalsExact(WKT_READER.read(buff_ft), 0.999), "Should be equal when adding 50m in ft");

//        in inches
        final String buff_in = SpatialEngineUtils.addWKTBuffer(wktInput, 1968.5, INCH);
        assertTrue(bufferedGeom.equalsExact(WKT_READER.read(buff_in), 0.999), "Should be equal when adding 50m in ft");

    }
}
