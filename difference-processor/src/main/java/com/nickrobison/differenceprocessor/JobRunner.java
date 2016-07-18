package com.nickrobison.differenceprocessor;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.differenceprocessor.records.GAULOutput;
import com.nickrobison.differenceprocessor.records.GAULMapperResult;
import com.nickrobison.differenceprocessor.records.GAULTextOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.db.DBConfiguration;
import org.apache.hadoop.mapreduce.lib.db.DBOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by nrobison on 4/18/16.
 */
public class JobRunner extends Configured implements Tool {

    private static final Logger logger = Logger.getLogger(JobRunner.class);

    public static void main(String[] args) throws Exception {

        System.exit(ToolRunner.run(new Configuration(), new JobRunner(), args));
    }

    public int run(final String[] args) throws Exception {
        Configuration hadoopConf = this.getConf();

//        Set config properties from file
        Properties props = new Properties();
        InputStream is = JobRunner.class.getClassLoader().getResourceAsStream("sd.properties");
        try {
            if (is == null) {
                throw new RuntimeException("Classpath missing sd.properties file");
            }

            props.load(is);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String name : props.stringPropertyNames()) {
            hadoopConf.set(name, props.getProperty(name));
        }

        // Database Settings
		DBConfiguration.configureDB(hadoopConf, "org.postgresql.Driver", hadoopConf.get("hadoop.database.uri"));

//        Mapreduce stuff
        Job job = Job.getInstance(hadoopConf, "Spatial Difference");
        job.setJarByClass(JobRunner.class);
        job.setMapperClass(SpatialMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(GAULMapperResult.class);
        job.setReducerClass(SpatialReducer.class);

        if (logger.isDebugEnabled()) {
            logger.debug("Reduce tasks: " + job.getNumReduceTasks());
        }

        job.setInputFormatClass(PolygonFeatureInputFormat.class);

//        Set outputs
        MultipleOutputs.addNamedOutput(job, "HDFS", GAULTextOutputFormat.class, Text.class, GAULOutput.class);
        MultipleOutputs.addNamedOutput(job, "Postgres", DBOutputFormat.class, GAULOutput.class, NullWritable.class);
        DBOutputFormat.setOutput(job, hadoopConf.get("hadoop.database.table"), GAULOutput.getFieldNumber());

        FileInputFormat.setInputDirRecursive(job, true);
//        TODO(nrobison): These input args need to be handled better
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        final Path outputDir = new Path(args[1]);
        outputDir.getFileSystem(hadoopConf).delete(outputDir, true);
        FileOutputFormat.setOutputPath(job, outputDir);

        return job.waitForCompletion(true) ? 0 : 1;
    }
}
