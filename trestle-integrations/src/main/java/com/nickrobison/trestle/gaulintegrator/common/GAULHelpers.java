package com.nickrobison.trestle.gaulintegrator.common;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nrobison on 7/2/17.
 */
public class GAULHelpers {
    private static final Logger logger = LoggerFactory.getLogger(GAULHelpers.class);
    private static final String REGEX_STRING = "(?<=\\_).*?(?=\\_)";
    private static final Pattern regexPattern = Pattern.compile(REGEX_STRING);

    private GAULHelpers() {}
    /**
     * Extracts the year from input filename
     *
     * @param inputSplit split currently being processed
     * @return IntWritable - year being processed
     */
    public static IntWritable extractSplitYear(InputSplit inputSplit) {
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
}
