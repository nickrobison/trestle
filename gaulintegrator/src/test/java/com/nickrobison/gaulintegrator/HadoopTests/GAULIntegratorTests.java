package com.nickrobison.gaulintegrator.HadoopTests;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.gaulintegrator.*;
import com.nickrobison.trestle.TrestleReasoner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 5/5/16.
 */
public class GAULIntegratorTests {

    private static FileSystem fileSystem;
    private static HdfsConfiguration conf;
    private static MiniDFSCluster cluster;

    private static final Logger logger = LoggerFactory.getLogger(GAULIntegratorTests.class);
    private static TrestleReasoner reasoner;

    @BeforeAll
    public static void setup() throws IOException {


        final File baseDir = new File("./target/hdfs/gaul-test").getAbsoluteFile();
        FileUtil.fullyDelete(baseDir);
        conf = new HdfsConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());

        final Properties userProperties = new Properties();
        final InputStream is = IntegrationRunner.class.getClassLoader().getResourceAsStream("sd.properties");
        userProperties.load(is);

        for (String name : userProperties.stringPropertyNames()) {
            conf.set(name, userProperties.getProperty(name));
        }

         fileSystem = FileSystem.get(conf);
        final YarnConfiguration clusterConf = new YarnConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();

//        Setup reasoner
        //                .withDBConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1")
//                .withDBConnection(conf.get("reasoner.db.connection"),
//                        conf.get("reasoner.db.username"),
//                        conf.get("reasoner.db.password"))
        reasoner = new TrestleReasoner.TrestleBuilder()
//                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
                .withDBConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1")
//                .withDBConnection(conf.get("reasoner.db.connection"),
//                        conf.get("reasoner.db.username"),
//                        conf.get("reasoner.db.password"))
                .withInputClasses(GAULObject.class)
                .initialize()
                .withName("hadoop_test")
                .build();
//        reasoner.shutdown(false);
    }

    @Test
    public void testReducer() throws IOException, ClassNotFoundException, InterruptedException {

        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("shapefiles/gates-test/");
        URL OUT_DIR = GAULIntegratorTests.class.getClassLoader().getResource("out/");

        Path inDir = new Path(IN_DIR.toString());
        Path outDir = new Path("./target/out/");

        fileSystem.delete(outDir, true);

        Job job = Job.getInstance(conf, "GAUL Integrator");
        job.setJarByClass(IntegrationRunner.class);
        job.setMapperClass(GAULMapper.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(MapperOutput.class);
        job.setReducerClass(GAULReducer.class);

        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, false);
        FileInputFormat.setInputPaths(job, inDir);
        FileOutputFormat.setOutputPath(job, outDir);
        job.waitForCompletion(true);
        assertTrue(job.isSuccessful());
    }

    @AfterAll
    public static void close() throws IOException {
        cluster.shutdown();

        File outputFile = new File("/Users/nrobison/Desktop/hadoop.owl");
        reasoner.writeOntology(outputFile.toURI(), true);
        reasoner.shutdown(false);
    }
}