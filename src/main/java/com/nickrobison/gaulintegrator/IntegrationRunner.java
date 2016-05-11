package com.nickrobison.gaulintegrator;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Created by nrobison on 5/5/16.
 */
public class IntegrationRunner extends Configured implements Tool {

    private static final Logger logger = Logger.getLogger(IntegrationRunner.class);

    public static void main(String[] args) throws Exception {

        System.exit(ToolRunner.run(new Configuration(), new IntegrationRunner(), args));
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = this.getConf();

        final Properties userProperties = new Properties();
        final InputStream is = IntegrationRunner.class.getClassLoader().getResourceAsStream("sd.properties");
        userProperties.load(is);

        for (String name : userProperties.stringPropertyNames()) {
            conf.set(name, userProperties.getProperty(name));
        }

        Job job = Job.getInstance(conf, "GAUL Integrator");
        job.setJarByClass(IntegrationRunner.class);
        job.setMapperClass(GAULMapper.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(MapperOutput.class);
        job.setReducerClass(GAULReducer.class);

        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, false);
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        final Path outputDir = new Path(args[1]);

        //        If we're in debug mode, truncate the table and delete the output dir
//        TODO(nrobison): Truncate database table
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting output dir: " + outputDir.getName());
            outputDir.getFileSystem(conf).delete(outputDir, true);
        }
        FileOutputFormat.setOutputPath(job, outputDir);

        return job.waitForCompletion(true) ? 0 : 1;
    }
}
