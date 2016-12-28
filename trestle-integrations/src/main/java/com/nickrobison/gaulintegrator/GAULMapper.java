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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.StrictMath.toIntExact;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings("argument.type.incompatible")
public class GAULMapper extends Mapper<LongWritable, PolygonFeatureWritable, LongWritable, MapperOutput> {

    private static final Logger logger = LoggerFactory.getLogger(GAULMapper.class);
    private static final Text CODE = new Text("ADM2_CODE");
    private static final Text NAME = new Text("ADM2_NAME");
    private static final Text YEAR = new Text("TBL_YEAR");
    private static final Text STRYEAR = new Text("STR2_YEAR");
    private static final Text EXPYEAR = new Text ("EXP2_YEAR");
    private static final Text SHAPELENG = new Text("Shape_Leng");

//    Regex
    private static final Pattern coordinateRegex = Pattern.compile("(\\-?\\d+\\.\\d+)");

    private LongWritable polygonID = new LongWritable();
    private LongWritable startYear = new LongWritable();
    private LongWritable expirationYear = new LongWritable();
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

//        Generate start and end dates
        LocalDate startDate = LocalDate.of(toIntExact(startYear.get()), Month.JANUARY, 1);
        LocalDate expirationYear = LocalDate.of(toIntExact(this.expirationYear.get()), Month.DECEMBER, 10);
//        Expiration date is valid until the last day of that year, so we need to increment the year by one, and grab its first day.
        LocalDate expirationDate = expirationYear.plusYears(1).with(TemporalAdjusters.firstDayOfYear());

//        Ensure the polygon has the correct coordinate system
        final Matcher matcher = coordinateRegex.matcher(inputRecord.polygon.getBoundary().toString());
        if (!matcher.find()) {
            logger.error("Cannot parse boundary for {}, year {}", polygonName, inputYear);
        }
        final double firstCoordinate = Double.parseDouble(matcher.group());

//        If the coordinate is outside the bounds of a valid geographic coordinate, then we know this record has bad data.
        if (firstCoordinate > 180.0 || firstCoordinate < -180.0) {
            logger.error("{}, Year {} has invalid polygon data. {}", polygonName, inputYear, inputRecord.polygon.getBoundary().toString());
        }

        final MapperOutput outputRecord = new MapperOutput(polygonID, polygonName, inputYear, inputRecord, startDate, expirationDate);
        context.write(polygonID, outputRecord);
    }


}
