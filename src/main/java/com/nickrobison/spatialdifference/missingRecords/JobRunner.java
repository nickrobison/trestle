package com.nickrobison.spatialdifference.missingRecords;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.spatialdifference.SpatialMapper;
import com.nickrobison.spatialdifference.records.GAULMapperResult;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * Created by nrobison on 4/29/16.
 */
public class JobRunner extends Configured implements Tool {

    private static final Logger logger = Logger.getLogger(JobRunner.class);

    public static void main(final String[] args) throws Exception {

        System.exit(ToolRunner.run(new Configuration(), new JobRunner(), args));
    }

    public int run(final String[] args) throws Exception {
        Configuration hadoopConf = this.getConf();

        Job job = Job.getInstance(hadoopConf, "Find dropouts");
        job.setJarByClass(JobRunner.class);
        job.setMapperClass(SpatialMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(GAULMapperResult.class);
        job.setReducerClass(MissingReducer.class);

        job.setInputFormatClass(PolygonFeatureInputFormat.class);

        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputDirRecursive(job, true);
//        TODO(nrobison): These input args need to be handled better
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        final Path outputDir = new Path(args[1]);
        outputDir.getFileSystem(hadoopConf).delete(outputDir, true);
        FileOutputFormat.setOutputPath(job, outputDir);

        return job.waitForCompletion(true) ? 0 : 1;
    }
}
