package com.nickrobison.gaulintegrator;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.common.GAULHelpers;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.StrictMath.toIntExact;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings("argument.type.incompatible")
public class GAULMapper extends Mapper<LongWritable, PolygonFeatureWritable, LongWritable, MapperOutput> {

    private static final Logger logger = LoggerFactory.getLogger(GAULMapper.class);
//    private static final Text CODE = new Text("adm2_code");
//    private static final Text NAME = new Text("adm2_name");
//    private static final Text STRYEAR = new Text("str2_year");
//    private static final Text EXPYEAR = new Text("exp2_year");
//    private static final Text A1CODE = new Text("adm1_code");
//    private static final Text A1NAME = new Text("adm1_name");
//    private static final Text STATUS = new Text("status");
//    private static final Text DISP_AREA = new Text("disp_area");
//    private static final Text A0CODE = new Text("adm0_code");
//    private static final Text A0NAME = new Text("adm0_name");
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

    //    Regex for validating input geometries
    private static final Pattern coordinateRegex = Pattern.compile("(\\-?\\d+\\.\\d+)");
    private final Set<Long> GAUL_IDS = new HashSet<>();

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        final String gaulCode = context.getConfiguration().get("gaulcode.restriction");
        if (gaulCode != null) {
//            GAUL_IDS = new HashSet<>();
            GAUL_IDS.add(Long.parseLong(gaulCode));
        } else {
//            Subset of a few countries, those explicitly mentioned in the World Bank paper
            GAUL_IDS.addAll(Arrays.asList(6L, 29L, 49L, 59L, 68L, 74L, 170L, 182L, 253L));
//          List of ADM0 Codes for Africa Bounding Box POLYGON((-26.4 38.1, 61.9 38.1, 61.9 -37.2, -26.4 -37.2, -26.4 38.1))
//            GAUL_IDS.addAll(Arrays.asList(1L, 4L, 6L, 8L, 21L, 25L, 29L, 35L, 42L, 43L, 45L, 47L, 49L, 50L, 58L, 59L, 64L, 66L, 68L, 70L, 74L, 76L, 77L, 79L, 80L, 89L, 90L, 91L, 94L, 95L, 96L, 97L, 102L, 105L, 106L, 117L, 118L, 121L, 122L, 130L, 131L, 133L, 137L, 141L, 142L, 144L, 145L, 150L, 151L, 152L, 155L, 156L, 159L, 160L, 161L, 169L, 170L, 172L, 181L, 182L, 187L, 188L, 199L, 201L, 205L, 206L, 207L, 214L, 215L, 217L, 220L, 221L, 226L, 227L, 229L, 235L, 238L, 243L, 247L, 248L, 249L, 250L, 253L, 255L, 257L, 267L, 268L, 269L, 270L, 271L, 40760L, 40762L, 40765L, 61013L, 74578L));
        }
    }

    @Override
    public void map(final LongWritable key,
                    final PolygonFeatureWritable inputRecord,
                    final Context context) throws IOException, InterruptedException {

        final LongWritable a0Code = (LongWritable) inputRecord.attributes.get(A0CODE);
        final Text dispArea = new Text(inputRecord.attributes.getText(DISP_AREA.toString()).trim());

        final BooleanWritable dispBool;
        if (dispArea.toString().equals("NO")) {
            dispBool = new BooleanWritable(false);
        } else {
            dispBool = new BooleanWritable(true);
        }

//        Should we proceed?
        if (!dispBool.get() && GAUL_IDS.contains(a0Code.get())) {
            //        Get the data table year
            final IntWritable inputYear = GAULHelpers.extractSplitYear(context.getInputSplit());
            final LongWritable polygonID = (LongWritable) inputRecord.attributes.get(CODE);
            final LongWritable startYear = (LongWritable) inputRecord.attributes.get(STRYEAR);
            final LongWritable expYear = (LongWritable) inputRecord.attributes.get(EXPYEAR);
//        We need to trim off the white space from the text input fields.
            final Text polygonName = new Text(inputRecord.attributes.getText(NAME.toString()).trim());
            final LongWritable a1Code = (LongWritable) inputRecord.attributes.get(A1CODE);
            final Text a1Name = new Text(inputRecord.attributes.getText(A1NAME.toString()).trim());
            final Text a0Name = new Text(inputRecord.attributes.getText(A0NAME.toString()).trim());
            final Text status = new Text(inputRecord.attributes.getText(STATUS.toString()).trim());
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


            final MapperOutput outputRecord = new MapperOutput(polygonID, polygonName, inputYear, inputRecord, startDate, expirationDate, a0Code, a0Name, a1Code, a1Name, dispBool, status);
            context.write(polygonID, outputRecord);
        }
    }
}
