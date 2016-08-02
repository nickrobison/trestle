package com.nickrobison.gaulintegrator;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.common.Utils;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

import static java.lang.StrictMath.toIntExact;

/**
 * Created by nrobison on 5/5/16.
 */
public class GAULMapper extends Mapper<LongWritable, PolygonFeatureWritable, LongWritable, MapperOutput> {

    private static final Logger logger = LoggerFactory.getLogger(GAULMapper.class);
    private static final Text CODE = new Text("ADM2_CODE");
    private static final Text NAME = new Text("ADM2_NAME");
    private static final Text YEAR = new Text("TBL_YEAR");
    private static final Text STRYEAR = new Text("STR2_YEAR");
    private static final Text EXPYEAR = new Text ("EXP2_YEAR");
    private static final Text SHAPELENG = new Text("Shape_Leng");

    private LongWritable polygonID = new LongWritable();
    private LongWritable startYear = new LongWritable();
    private LongWritable expirationYear = new LongWritable();
    private FloatWritable shape_leng = new FloatWritable();
    private Text polygonName = new Text();

    @Override
    public void map(final LongWritable key,
                    final PolygonFeatureWritable inputRecord,
                    final Context context) throws IOException, InterruptedException {

//        Get the data table year
        final IntWritable inputYear = Utils.ExtractSplitYear(context.getInputSplit());

        polygonID = (LongWritable) inputRecord.attributes.get(CODE);
        startYear = (LongWritable) inputRecord.attributes.get(STRYEAR);
        expirationYear = (LongWritable) inputRecord.attributes.get(EXPYEAR);
//        We need to trim off the white space from the text input fields.
        polygonName.set(inputRecord.attributes.getText(NAME.toString()).trim());
        shape_leng = (FloatWritable) inputRecord.attributes.get(SHAPELENG);

//        Generate start and end dates
        LocalDate startDate = LocalDate.of(toIntExact(startYear.get()), Month.JANUARY, 1);
        LocalDate expirationYear = LocalDate.of(toIntExact(this.expirationYear.get()), Month.DECEMBER, 10);
//        Expiration date is valid until the last day of that year, so we need to increment the year by one, and grab its first day.
        LocalDate expirationDate = expirationYear.plusYears(1).with(TemporalAdjusters.firstDayOfYear());

        final MapperOutput outputRecord = new MapperOutput(polygonID, polygonName, inputYear, inputRecord, startDate, expirationDate);
        context.write(polygonID, outputRecord);
    }


}
