package com.nickrobison.differenceprocessor.records;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * Created by nrobison on 4/28/16.
 */
public class GAULTextOutputFormat extends FileOutputFormat<Text, GAULOutput> {
    @Override
    public RecordWriter<Text, GAULOutput> getRecordWriter(TaskAttemptContext job) throws IOException, InterruptedException {
//        Get current path
        Path path = FileOutputFormat.getOutputPath(job);
//        create the full path with the output filename
        Path fullPath = new Path(path, "part-" + job.getTaskAttemptID());

//        create the file on the filesystem
        FileSystem fs = path.getFileSystem(job.getConfiguration());
        FSDataOutputStream fileOut = fs.create(fullPath, job);

//        Create out record writer with the new file
        return new GAULRecordWriter(fileOut);
    }
}
