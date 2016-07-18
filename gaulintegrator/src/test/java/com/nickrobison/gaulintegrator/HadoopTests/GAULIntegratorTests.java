package com.nickrobison.gaulintegrator.HadoopTests;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.GAULMapper;
import com.nickrobison.gaulintegrator.MapperOutput;
import com.nickrobison.gaulintegrator.GAULMapper;
import com.nickrobison.gaulintegrator.MapperOutput;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.Assert.assertEquals;

/**
 * Created by nrobison on 5/5/16.
 */
public class GAULIntegratorTests {

    private static final Text CODE = new Text("ADM2_CODE");
    private static final Text NAME = new Text("ADM2_NAME");
    private static final Text YEAR = new Text("TBL_YEAR");
    private static final Text STRYEAR = new Text("STR2_YEAR");
    private static final Text EXPYEAR = new Text ("EXP2_YEAR");

    MapDriver<LongWritable, PolygonFeatureWritable, LongWritable, MapperOutput> mapDriver;


    @Before
    public void setup() {
        GAULMapper mapper = new GAULMapper();
        mapDriver = MapDriver.newMapDriver(mapper);
    }

    @Test
    public void testMapper() throws IOException {
        PolygonFeatureWritable testPolygon = new PolygonFeatureWritable();
        LocalDate testStartDate = LocalDate.of(1990, 01, 01);
        LocalDate testExpirationYear = LocalDate.of(1990, 12, 31);
        LocalDate testExpirationDate = testExpirationYear.plusYears(1).with(TemporalAdjusters.firstDayOfYear());
        Polygon polygon = testPolygon.polygon;
        Envelope env = new Envelope(1000, 2000, 1010, 2010);
        polygon.addEnvelope(env, false);
        testPolygon.attributes.put(CODE, new LongWritable(1234));
        testPolygon.attributes.put(NAME, new Text("Test Region"));
        testPolygon.attributes.put(YEAR, new IntWritable(1990));
        testPolygon.attributes.put(STRYEAR, new LongWritable(1000));
        testPolygon.attributes.put(EXPYEAR, new LongWritable(3000));

        MapperOutput testOutput = new MapperOutput(new LongWritable(1234), new Text("Test Region"), new IntWritable(1990), testPolygon, testStartDate, testExpirationDate);



        mapDriver.withInput(new LongWritable(), testPolygon);
        mapDriver.withOutput(new LongWritable(1234), testOutput);
        mapDriver.runTest();
    }
}
