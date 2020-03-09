package com.nickrobison.trestle.reasoner.engines.spatial;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import tec.uom.se.unit.MetricPrefix;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static si.uom.SI.METRE;
import static systems.uom.common.USCustomary.FOOT;
import static systems.uom.common.USCustomary.INCH;

/**
 * Created by nickrobison on 3/19/18.
 */
public class SpatialUtilsTest {

    private static final Unit<Length> CENTIMETER = MetricPrefix.CENTI(METRE);
    private static final Unit<Length> KILOMETER = MetricPrefix.KILO(METRE);

    private static Geometry bufferedGeom;
    private static String wktInput;
    private static String bufferedWKT;
    private static WKTWriter wktWriter = new WKTWriter();
    public static final WKTReader WKT_READER = new WKTReader();

    @BeforeAll
    public static void setup() throws ParseException {
        wktInput = "POLYGON((-122.374781 47.690612, -122.325515 47.690612, -122.325515 47.668884, -122.374781 47.668884, -122.374781 47.690612))";
        Geometry wktGeom = WKT_READER.read(wktInput);
        bufferedGeom = wktGeom.buffer(50);
        bufferedWKT = wktWriter.write(bufferedGeom);
    }

    @Test
    public void testBufferUnits() throws ParseException {

//        Directly compare with buffer in meters

//        Try to apply a buffer of 50 m
        final String buff_m = SpatialEngineUtils.addWKTBuffer(wktInput, 50, METRE);
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
