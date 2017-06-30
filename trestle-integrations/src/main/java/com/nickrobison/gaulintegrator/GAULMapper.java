package com.nickrobison.gaulintegrator;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.common.DateFieldUtils;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
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
    private static final Text STRYEAR = new Text("STR2_YEAR");
    private static final Text EXPYEAR = new Text("EXP2_YEAR");
    private static final Text A1CODE = new Text("ADM1_CODE");
    private static final Text A1NAME = new Text("ADM1_NAME");
    private static final Text STATUS = new Text("STATUS");
    private static final Text DISP_AREA = new Text("DISP_AREA");
    private static final Text A0CODE = new Text("ADM0_CODE");
    private static final Text A0NAME = new Text("ADM0_NAME");


    //    Regex
    private static final Pattern coordinateRegex = Pattern.compile("(\\-?\\d+\\.\\d+)");

    @Override
    public void map(final LongWritable key,
                    final PolygonFeatureWritable inputRecord,
                    final Context context) throws IOException, InterruptedException {

//        Get the data table year
        final IntWritable inputYear = DateFieldUtils.extractSplitYear(context.getInputSplit());

        final LongWritable polygonID = (LongWritable) inputRecord.attributes.get(CODE);
        final LongWritable startYear = (LongWritable) inputRecord.attributes.get(STRYEAR);
        final LongWritable expYear = (LongWritable) inputRecord.attributes.get(EXPYEAR);
//        We need to trim off the white space from the text input fields.
        final Text polygonName = new Text(inputRecord.attributes.getText(NAME.toString()).trim());
        final LongWritable a1Code = (LongWritable) inputRecord.attributes.get(A1CODE);
        final Text a1Name = new Text(inputRecord.attributes.getText(A1NAME.toString()).trim());
        final LongWritable a0Code = (LongWritable) inputRecord.attributes.get(A0CODE);
        final Text a0Name = new Text(inputRecord.attributes.getText(A0NAME.toString()).trim());
        final Text status = new Text(inputRecord.attributes.getText(STATUS.toString()).trim());
        final Text dispArea = new Text(inputRecord.attributes.getText(DISP_AREA.toString()).trim());


//        Generate start and end dates
        LocalDate startDate = LocalDate.of(toIntExact(startYear.get()), Month.JANUARY, 1);
        LocalDate expirationYear = LocalDate.of(toIntExact(expYear.get()), Month.DECEMBER, 10);
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
            logger.error("{}, Year {} has invalid polygon data. {}", polygonName, inputYear, inputRecord.polygon.getBoundary());
        }
        final BooleanWritable dispBool;
        if (dispArea.toString().equals("NO")) {
            dispBool = new BooleanWritable(false);
        } else {
            dispBool = new BooleanWritable(true);
        }

        final MapperOutput outputRecord = new MapperOutput(polygonID, polygonName, inputYear, inputRecord, startDate, expirationDate, a0Code, a0Name, a1Code, a1Name, dispBool, status);
        context.write(polygonID, outputRecord);
    }
}
