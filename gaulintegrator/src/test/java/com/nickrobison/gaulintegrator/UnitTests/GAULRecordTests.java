package com.nickrobison.gaulintegrator.UnitTests;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.MapperOutput;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.Assert.*;

/**
 * Created by nrobison on 5/6/16.
 */
public class GAULRecordTests {

    private MapperOutput mapperOutput;
    private LocalDate testStartDate;
    private LocalDate testExpirationDate;

    @Before
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

        assertEquals("Start dates are not equal", testStartDate, mapperOutput.getStartDate());
        assertEquals("Expiration dates are not equal", testExpirationDate, mapperOutput.getExpirationDate());
    }

    @Test
    public void TestDateValidInterval() {
        LocalDate testDateWithin = LocalDate.of(1990, Month.APRIL, 24);
        LocalDate testDateBefore = LocalDate.of(1989, Month.AUGUST, 12);
        LocalDate testDateAfter = LocalDate.of(2000, Month.SEPTEMBER, 01);

//        Test whether the object is valid or not for a given date
        assertTrue("Date is within valid range, should be true", mapperOutput.isValidNow(testDateWithin));
        assertFalse("Date is outside of valid range, should be false", mapperOutput.isValidNow(testDateAfter));
        assertFalse("Date is outside of valid range, should be false", mapperOutput.isValidNow(testDateBefore));

//        Test comparing different dates
        assertEquals("Date is before valid range", -1, mapperOutput.compareDate(testDateBefore));
        assertEquals("Date is within valid range", 0, mapperOutput.compareDate(testDateWithin));
        assertEquals("Date is after valid range", 1, mapperOutput.compareDate(testDateAfter));

//        Test the inclusive/exclusive nature of the start/expire dates
        assertTrue("Date is start date, should be valid", mapperOutput.isValidNow(testStartDate));
        assertFalse("Date is expiration date, should not be valid", mapperOutput.isValidNow(testExpirationDate));
        assertEquals("Start date is during valid range, should be 0", 0, mapperOutput.compareDate(testStartDate));
        assertEquals("End date is after valid range, should be 1", 1, mapperOutput.compareDate(testExpirationDate));

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
        assertEquals("Objects should be the same", mapperOutput, mo);
//        Read some data out of the object
        assertEquals("Dates should be the same", mapperOutput.getStartDate(), mo.getStartDate());
        assertEquals("Polygons should be the same", mapperOutput.getPolygonData().polygon, mo.getPolygonData().polygon);
//        Test data bytearray
        assertArrayEquals("Date byte arrays should be equal", mapperOutput.getDateField(), mo.getDateField());
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

        assertEquals("Other has greater ID", -1, mapperOutput.compareTo(mapperCompareGreaterID));
        assertEquals("Other has greater Year", -1, mapperOutput.compareTo(mapperCompareGreaterYear));
        assertEquals("Other has lesser ID", 1, mapperOutput.compareTo(mapperCompareLesserID));
        assertEquals("Other has lesser Year", 1, mapperOutput.compareTo(mapperCompareLesserYear));
        assertEquals("Other has lesser ID and Year", 1, mapperOutput.compareTo(mapperCompareLesserIDAndYear));
        assertEquals("Other has greater ID and Year", -1, mapperOutput.compareTo(mapperCompareGreaterIDandYear));
    }
}
