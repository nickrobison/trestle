package com.nickrobison.gaulintegrator.UnitTests;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.MapperOutput;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 5/6/16.
 */
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized"})
@Tag("unit")
public class GAULRecordTests {

    private MapperOutput mapperOutput;
    private LocalDate testStartDate;
    private LocalDate testExpirationDate;

    @BeforeEach
    public void setup() throws SQLException {
        testStartDate = LocalDate.of(1990, 01, 01);
        testExpirationDate = LocalDate.of(1990, 12, 31);
        mapperOutput = new MapperOutput(
                new LongWritable(1234),
                new Text("Test Polygon"),
                new IntWritable(1990),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );
    }

    @Test
    public void TestDateSerialization() {

        assertEquals(testStartDate, mapperOutput.getStartDate(), "Start dates are not equal");
        assertEquals(testExpirationDate, mapperOutput.getExpirationDate(), "Expiration dates are not equal");
    }

    @Test
    public void TestDateValidInterval() {
        LocalDate testDateWithin = LocalDate.of(1990, Month.APRIL, 24);
        LocalDate testDateBefore = LocalDate.of(1989, Month.AUGUST, 12);
        LocalDate testDateAfter = LocalDate.of(2000, Month.SEPTEMBER, 01);

//        Test whether the object is valid or not for a given date
        assertTrue(mapperOutput.isValidNow(testDateWithin), "Date is within valid range, should be true");
        assertFalse(mapperOutput.isValidNow(testDateAfter), "Date is outside of valid range, should be false");
        assertFalse(mapperOutput.isValidNow(testDateBefore), "Date is outside of valid range, should be false");

//        Test comparing different dates
        assertEquals(-1, mapperOutput.compareDate(testDateBefore), "Date is before valid range");
        assertEquals(0, mapperOutput.compareDate(testDateWithin), "Date is within valid range");
        assertEquals(1, mapperOutput.compareDate(testDateAfter), "Date is after valid range");

//        Test the inclusive/exclusive nature of the start/expire dates
        assertTrue(mapperOutput.isValidNow(testStartDate), "Date is start date, should be valid");
        assertFalse(mapperOutput.isValidNow(testExpirationDate), "Date is expiration date, should not be valid");
        assertEquals(0, mapperOutput.compareDate(testStartDate), "Start date is during valid range, should be 0");
        assertEquals(1, mapperOutput.compareDate(testExpirationDate), "End date is after valid range, should be 1");

    }

    @Test
    public void TestHadoopSerialization() throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        mapperOutput.write(dos);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        DataInputStream dis = new DataInputStream(is);
        MapperOutput mo = new MapperOutput();

        mo.readFields(dis);
        assertEquals(mapperOutput, mo, "Objects should be the same");
//        Read some data out of the object
        assertEquals(mapperOutput.getStartDate(), mo.getStartDate(), "Dates should be the same");
        assertEquals(mapperOutput.getPolygonData().polygon, mo.getPolygonData().polygon, "Polygons should be the same");
//        Test data bytearray
        assertArrayEquals(mapperOutput.getDateField(), mo.getDateField(), "Date byte arrays should be equal");
    }

    @Test
    public void TestCompareTo() {
        MapperOutput mapperCompareGreaterID = new MapperOutput(
                new LongWritable(5000),
                new Text("Test Polygon"),
                new IntWritable(1990),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        MapperOutput mapperCompareGreaterYear = new MapperOutput(
                new LongWritable(1234),
                new Text("Test Polygon"),
                new IntWritable(2000),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        MapperOutput mapperCompareLesserID = new MapperOutput(
                new LongWritable(345),
                new Text("Test Polygon"),
                new IntWritable(1990),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        MapperOutput mapperCompareLesserYear = new MapperOutput(
                new LongWritable(1234),
                new Text("Test Polygon"),
                new IntWritable(1800),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        MapperOutput mapperCompareLesserIDAndYear = new MapperOutput(
                new LongWritable(345),
                new Text("Test Polygon"),
                new IntWritable(1800),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        MapperOutput mapperCompareGreaterIDandYear = new MapperOutput(
                new LongWritable(5000),
                new Text("Test Polygon"),
                new IntWritable(2000),
                new PolygonFeatureWritable(),
                testStartDate,
                testExpirationDate
        );

        assertEquals(-1, mapperOutput.compareTo(mapperCompareGreaterID), "Other has greater ID");
        assertEquals(-1, mapperOutput.compareTo(mapperCompareGreaterYear), "Other has greater Year");
        assertEquals(1, mapperOutput.compareTo(mapperCompareLesserID), "Other has lesser ID");
        assertEquals(1, mapperOutput.compareTo(mapperCompareLesserYear), "Other has lesser Year");
        assertEquals(1, mapperOutput.compareTo(mapperCompareLesserIDAndYear), "Other has lesser ID and Year");
        assertEquals(-1, mapperOutput.compareTo(mapperCompareGreaterIDandYear), "Other has greater ID and Year");
    }
}
