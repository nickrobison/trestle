package com.nickrobison.StormTests;

import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Polygon;
import com.esri.io.Attributes;
import com.esri.io.PolygonFeatureWritable;
import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.spatialdifference.storm.bolts.NewShapeBolt;
import com.nickrobison.spatialdifference.storm.spouts.PolygonSpout;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.apache.log4j.Logger;
import org.apache.storm.Config;
import org.apache.storm.Testing;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.testing.CompleteTopologyParam;
import org.apache.storm.testing.MkClusterParam;
import org.apache.storm.testing.MockedSources;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Values;
import org.junit.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.log4j.Level.ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by nrobison on 5/3/16.
 */
public class MainFlowTest {

    private MkClusterParam mkClusterParam;
    private Config daemonConfig;
    private static RecordReader<LongWritable, PolygonFeatureWritable> polygonRecordReader;
    private static List<PolygonFeatureWritable> inputPolygons;

    private static MiniDFSCluster hdfsCluster;
    private static DistributedFileSystem fs;
    private static String hdfsURI;
    private static Configuration jobConf;
    private MockedSources mockedSources;

    @BeforeClass
    public static void setupTests() throws IOException, URISyntaxException, InterruptedException {

        setupMetricsLogging();
        setupHDFSCluster();
        loadFiles();
        setupInputPolygons();
    }

    @AfterClass
    public static void shutdownTest() throws IOException {
        fs.close();
        hdfsCluster.shutdown();
    }

    @After
    public void closeFiles() throws IOException {
        polygonRecordReader.close();
    }

    @Before
    public void setupNewCluster() {
//        Reset cluster config to new state
        mkClusterParam = new MkClusterParam();
        mkClusterParam.setSupervisors(1);
        daemonConfig = new Config();
        daemonConfig.put(Config.STORM_CLUSTER_MODE, "local");
        daemonConfig.setDebug(true);
        mkClusterParam.setDaemonConf(daemonConfig);

//        Now the mock data
        mockedSources = new MockedSources();
        List<Values> mockedValues = inputPolygons
                .stream()
                .map(Values::new)
                .collect(Collectors.toList());
        mockedSources.addMockData("1", mockedValues.toArray(new Values[mockedValues.size()]));

    }

    @Test
    public void testHDFSClusterCreation() {
        Testing.withLocalCluster(mkClusterParam, cluster -> assertNotNull(cluster.getState()));
    }

    @Test
    public void testHDFSPolygonSpout() {

        Testing.withSimulatedTimeLocalCluster(mkClusterParam, cluster -> {
            TopologyBuilder builder = new TopologyBuilder();

            builder.setSpout("1", new PolygonSpout(), 1);
            builder.setBolt("2", new NewShapeBolt(), 1).shuffleGrouping("1");

            StormTopology localTopology = builder.createTopology();

            com.esotericsoftware.minlog.Log.TRACE();
            Config conf = new Config();
            conf.setNumWorkers(2);
            conf.registerSerialization(Class.forName("com.esri.core.geometry.AttributeStreamOfDbl"));
            conf.registerSerialization(Class.forName("com.esri.core.geometry.MultiPathImpl"));
            conf.registerSerialization(MultiPath.class);
            conf.registerSerialization(Attributes.class);
            conf.registerSerialization(Polygon.class);
            conf.registerSerialization(PolygonFeatureWritable.class);

            conf.setFallBackOnJavaSerialization(false);

            CompleteTopologyParam topologyParam = new CompleteTopologyParam();
                topologyParam.setMockedSources(mockedSources);
            topologyParam.setStormConf(conf);

            Map result = Testing.completeTopology(cluster, localTopology, topologyParam);

            assertNotNull(result);
        });
    }

    private static Path getPathAndLoad(final String name) throws URISyntaxException, IOException {
        final URI resource = Paths.get(name).toAbsolutePath().toUri();
        final Path path = new Path(name);
        fs.copyFromLocalFile(new Path(resource), path);
        return path;
    }

    private static FileSplit getFileSplit(final Path dst) throws IOException {
        final long len = fs.getFileStatus(dst).getLen();
        return new FileSplit(dst, 0, len, null);
    }

    private static void setupHDFSCluster() throws IOException {
        jobConf = new Configuration();
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(jobConf);
        hdfsCluster = builder.build();
        fs = hdfsCluster.getFileSystem();
        hdfsURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/";
    }

    private static void loadFiles() throws IOException, URISyntaxException, InterruptedException {
        final Path dbf = getPathAndLoad("src/test/resources/shapefiles/combined.dbf");
        final Path shp = getPathAndLoad("src/test/resources/shapefiles/combined.shp");
        final FileSplit fileSplit = getFileSplit(shp);
        final PolygonFeatureInputFormat polygonInputFormat = new PolygonFeatureInputFormat();
        final TaskAttemptContext taskAttemptContext = new TaskAttemptContextImpl(jobConf, new TaskAttemptID());
        polygonRecordReader = polygonInputFormat.createRecordReader(fileSplit, taskAttemptContext);
    }

    private static void setupInputPolygons() throws IOException, InterruptedException {
        inputPolygons = new ArrayList<>();
        assertTrue(polygonRecordReader.nextKeyValue());
        do {
            final PolygonFeatureWritable currentPolygon = polygonRecordReader.getCurrentValue();
            inputPolygons.add(currentPolygon);
        } while (polygonRecordReader.nextKeyValue());
        assertEquals(3000, inputPolygons.size());
    }

    public static void setupMetricsLogging()
    {
//        Logger.getLogger(org.apache.hadoop.metrics2.util.MBeans.class).setLevel(ERROR);
//        Logger.getLogger(org.apache.hadoop.metrics2.impl.MetricsSystemImpl.class).setLevel(ERROR);
    }
}
