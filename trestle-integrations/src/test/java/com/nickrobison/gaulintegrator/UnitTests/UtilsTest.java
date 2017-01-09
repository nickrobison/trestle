package com.nickrobison.gaulintegrator.UnitTests;

import com.nickrobison.gaulintegrator.common.Utils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 5/9/16.
 */
public class UtilsTest {

    @Test
    public void TestYearExtraction() {
        final String inputString = "g2015_2008_2";
        final String[] testHosts = {"localhost", "test-host"};
        FileSplit inputSplit = new FileSplit(new Path("test/path/" + inputString), 0L, 1000L, testHosts);
        assertEquals(new IntWritable(2008), Utils.ExtractSplitYear(inputSplit), "Years should match");
    }

}
