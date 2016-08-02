package com.nickrobison.gaulintegrator.common;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nrobison on 5/9/16.
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final String regexString = "(?<=\\_).*?(?=\\_)";
    private static final Pattern regexPattern = Pattern.compile(regexString);
    public static final int TIMEDATASIZE = 2 * Long.BYTES;

    /**
     * Extracts the year from input filename
     *
     * @param inputSplit split currently being processed
     * @return IntWritable - year being processed
     */
    public static IntWritable ExtractSplitYear(InputSplit inputSplit) {
        int year = 9999;
        StringBuilder inputString = new StringBuilder(((FileSplit) inputSplit).getPath().getName());
        Matcher matcher = regexPattern.matcher(inputString);
        if (matcher.find()) {
            StringBuilder stringYear = new StringBuilder(matcher.group(0));
            year = Integer.parseInt(stringYear.toString());
        } else {
            logger.error("Cannot parse split input: {}", ((FileSplit) inputSplit).getPath());
        }

        return new IntWritable(year);
    }

    /**
     * Writes a LocalDate pair into a byte array as longs
     * @param start LocalDate - Start date
     * @param end LocalDate - End date
     * @return byte[] - Byte array of start/end long pair
     */
    public static byte[] WriteDateField(LocalDate start, LocalDate end) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[TIMEDATASIZE]);
        bb.putLong(start.toEpochDay());
        bb.putLong(end.toEpochDay());

        return bb.array();
    }

    /**
     * Returns a constructed LocalDate from a byte[] of start/end long pair
     * @param dateField byte[] - Start/end long pair
     * @return LocalDate - Start date from byte[]
     */
    public static LocalDate ReadStartDate(byte[] dateField) {
        ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong());
    }

    /**
     * Returns a constructed LocalDate from a byte[] of start/end long pair
     * @param dateField byte[] - Start/end long pair
     * @return LocalDate - End date from byte[]
     */
    public static LocalDate ReadExpirationDate(byte[] dateField) {
        ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong(Long.BYTES));
    }
}
