package com.nickrobison.spatialdifference.records;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by nrobison on 4/28/16.
 */
public class GAULRecordWriter extends RecordWriter<Text, GAULOutput> {
    private DataOutputStream out;

    public GAULRecordWriter(DataOutputStream stream) throws IOException {
        out = stream;
        out.writeBytes("adm2name:\tspatialVariance:\tcentroidVariance:\trecords:\t\r\n");
    }

    @Override
    public void write(Text key, GAULOutput value) throws IOException, InterruptedException {
        out.writeBytes(key.toString() + "\t");
        out.writeBytes(value.getFormattedResult());
        out.writeBytes("\r\n");
    }

    @Override
    public void close(TaskAttemptContext context) throws IOException, InterruptedException {
        out.close();
    }
}
