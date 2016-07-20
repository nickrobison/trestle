package com.nickrobison.differenceprocessor.missingRecords;

import com.nickrobison.differenceprocessor.records.GAULMapperResult;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nrobison on 4/29/16.
 */
public class MissingReducer extends Reducer<Text, GAULMapperResult, Text, Text> {

    private static final List<String> yearValues = Arrays.asList("2009", "2010", "2011", "2012", "2013", "2014");

    public void reduce(Text key, Iterable<GAULMapperResult> values, Context context) throws IOException, InterruptedException {

        List<String> resultYears = new ArrayList<>();
        int recordCount = 0;
        for (GAULMapperResult result : values) {
            resultYears.add(result.getYear());
            recordCount++;
        }

//        Figure out which years are missing from the count
        List<String> notPresent = new ArrayList<>(yearValues);
        notPresent.removeAll(resultYears);

        context.write(key, new Text(recordCount + " records present. Missing Years: " + notPresent.toString()));
    }
}
