package com.nickrobison.gaulintegrator;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.gaulintegrator.sorting.GAULMapperADM2CodeComparator;
import com.nickrobison.gaulintegrator.sorting.GAULPartitioner;
import com.nickrobison.gaulintegrator.sorting.NaturalKeyGroupingComparator;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Created by nrobison on 5/5/16.
 */
public class IntegrationRunner extends Configured implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationRunner.class);

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


//        Do we a command line property that specifies which GAUL codes to restrict output to?
        if (args.length > 2) {
            conf.set("gaulcode.restriction", args[2]);
        }


//        Setup the reasoner
        TrestleReasoner reasoner = new TrestleBuilder()
                .withDBConnection(conf.get("reasoner.db.connection"),
                        conf.get("reasoner.db.username"),
                        conf.get("reasoner.db.password"))
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(conf.get("reasoner.ontology.location")))
                .withPrefix(conf.get("reasoner.ontology.prefix"))
                .withName(conf.get("reasoner.ontology.name"))
                .initialize()
                .withoutCaching()
                .withoutMetrics()
                .build();

        Job job = Job.getInstance(conf, "GAUL Integrator");
        job.setJarByClass(IntegrationRunner.class);
        job.setMapperClass(GAULMapper.class);
        job.setMapOutputKeyClass(GAULMapperKey.class);
        job.setMapOutputValueClass(MapperOutput.class);
        //        Deterministic sorting and partitioning, very course grained, we'll just do country code
        //        We also need both the grouping and the sort comparator, not entirely sure why
        job.setGroupingComparatorClass(NaturalKeyGroupingComparator.class);
        job.setSortComparatorClass(GAULMapperADM2CodeComparator.class);
        job.setPartitionerClass(GAULPartitioner.class);
        job.setReducerClass(GAULReducer.class);

        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, true);
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        final Path outputDir = new Path(args[1]);

        //        If we're in debug mode, truncate the table and delete the output dir
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting output dir: {}", outputDir.getName());
            outputDir.getFileSystem(conf).delete(outputDir, true);
        }
        FileOutputFormat.setOutputPath(job, outputDir);

        //        Remove the HDS output directory
        try (final FileSystem fileSystem = FileSystem.get(conf)) {
            if (fileSystem.exists(outputDir)) {
                fileSystem.delete(outputDir, true);
            }
        }

//        Add the cache files
//        final URL resource = IntegrationRunner.class.getClassLoader().getResource("trestle.owl");
//        logger.debug("Loading: {}", URI.create(resource.toString() + "#trestle"));
//        job.addCacheFile(URI.create(resource.toString() + "#trestle"));
        job.waitForCompletion(true);

        reasoner.getUnderlyingOntology().runInference();
        reasoner.shutdown(false);

        if (job.isSuccessful()) {
            return 0;
        }
        return 1;
    }
}
