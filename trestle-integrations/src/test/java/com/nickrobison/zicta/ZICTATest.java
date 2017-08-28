package com.nickrobison.zicta;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.gaulintegrator.HadoopTests.GAULIntegratorTests;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 3/6/17.
 */
@Disabled
@Tag("load")
public class ZICTATest {

    private static HdfsConfiguration conf;
    private static FileSystem fileSystem;
    private static MiniDFSCluster cluster;

    @BeforeAll
    public static void setup() throws IOException {
        final File baseDir = new File("./target/hdfs/gaul-test").getAbsoluteFile();
        FileUtil.fullyDelete(baseDir);
        conf = new HdfsConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());

        fileSystem = FileSystem.get(conf);
        final YarnConfiguration clusterConf = new YarnConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();
    }

    @Test
    public void testZICTA() throws URISyntaxException, IOException, ClassNotFoundException, InterruptedException {
        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("shapefiles/zipcodes/");
        URL OUT_DIR = GAULIntegratorTests.class.getClassLoader().getResource("out/zipcodes/");

        Path inDir = new Path(IN_DIR.toString());
        Path outDir = new Path("./target/out/zipcodes");

        fileSystem.delete(outDir, true);

        final Job job = Job.getInstance(conf, "Zip Codes");
        job.setMapperClass(ZICTAMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ZICTAMapOutput.class);
        job.setReducerClass(ZICTAReducer.class);
        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, false);
        FileInputFormat.setInputPaths(job, inDir);
        FileOutputFormat.setOutputPath(job, outDir);
        job.waitForCompletion(true);
        assertTrue(job.isSuccessful());
    }

    @AfterAll
    public static void close() {
        cluster.shutdown();
    }
}
