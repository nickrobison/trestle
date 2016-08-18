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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized", "OptionalGetWithoutIsPresent"})
public class GAULIntegratorTests {

    private static FileSystem fileSystem;
    private static HdfsConfiguration conf;
    private static MiniDFSCluster cluster;

    private static final Logger logger = LoggerFactory.getLogger(GAULIntegratorTests.class);
    private static TrestleReasoner reasoner;
    private static String connectionString;
    private static String userName;
    private static String password;
    private static String ontologyName;

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

//        String connectionString = "jdbc:virtuoso://localhost:1111";
//        String userName = "dba";
//        String password = "dba";
        connectionString = "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial";
        userName = "spatialUser";
        password = "spatial1";
        ontologyName = "hadoop_gaul5";
        conf.set("reasoner.db.connection", connectionString);
        conf.set("reasoner.db.username", userName);
        conf.set("reasoner.db.password", password);
        conf.set("reasoner.ontology.name", ontologyName);

//        Setup reasoner
        reasoner = new TrestleReasoner.TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .initialize()
                .withName(ontologyName)
                .build();

//        File outputFile = new File("/Users/nrobison/Desktop/hadoop.owl");
//        reasoner.writeOntology(outputFile.toURI(), true);
        reasoner.shutdown(false);
    }

    @Test
    public void testReducer() throws IOException, ClassNotFoundException, InterruptedException, SQLException {

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

//        Try to find some individuals
        reasoner = new TrestleReasoner.TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .withName(ontologyName)
                .build();

        final Optional<Map<@NonNull GAULObject, Double>> relatedObjects1 = reasoner.getRelatedObjects(GAULObject.class, "0fea0c09-621e-4def-9a8b-bd36b45a09bc", 0.0);
        assertTrue(relatedObjects1.isPresent(), "Should have related objects");
        assertTrue(relatedObjects1.get().size() > 0, "Should have more than 0 related objects");
        logger.info("Has {} objects}", relatedObjects1.get().size());

    }

    @AfterAll
    public static void close() throws IOException {
        cluster.shutdown();

//        File outputFile = new File("/Users/nrobison/Desktop/hadoop.owl");
//        reasoner.writeOntology(outputFile.toURI(), true);
        reasoner.shutdown(false);
    }
}