package com.nickrobison.StormTests;

import com.esri.core.geometry.*;
import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.differenceprocessor.storm.bolts.NewShapeBolt;
import com.nickrobison.differenceprocessor.storm.spouts.SimplePolygonSpout;
import com.nickrobison.differenceprocessor.storm.utils.KryoFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.storm.Config;
import org.apache.storm.Testing;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.testing.CompleteTopologyParam;
import org.apache.storm.testing.MkClusterParam;
import org.apache.storm.testing.MockedSources;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Created by nrobison on 5/4/16.
 */
// Totally doesn't work. For some reason, the mocked sources just decides to not serialize the Polygon attributes correctly.
//    But it works great when running outside the test environment.
public class StormSimpleTest {

    private static final Logger logger = Logger.getLogger(StormSimpleTest.class);

    @Test
    public void TestSimplePolygonOutput() {

        MkClusterParam mkClusterParam = new MkClusterParam();
        mkClusterParam.setSupervisors(1);
        Config daemonConfig = new Config();
        daemonConfig.put(Config.STORM_CLUSTER_MODE, "local");
        daemonConfig.setDebug(false);
        daemonConfig.setKryoFactory(KryoFactory.class);
        daemonConfig.setFallBackOnJavaSerialization(false);
        mkClusterParam.setDaemonConf(daemonConfig);

        Testing.withSimulatedTimeLocalCluster(mkClusterParam, cluster -> {
            TopologyBuilder builder = new TopologyBuilder();

            builder.setSpout("1", new SimplePolygonSpout(), 1);
            builder.setBolt("2", new NewShapeBolt(), 1).shuffleGrouping("1");

            StormTopology localTopology = builder.createTopology();

            Config conf = new Config();
            conf.setNumWorkers(1);
//            conf.setKryoFactory(KryoFactory.class);
//            conf.setDebug(false);
//            conf.setFallBackOnJavaSerialization(false);
//            conf.registerSerialization(Class.forName("com.esri.core.geometry.AttributeStreamOfDbl"));
//            conf.registerSerialization(Class.forName("com.esri.core.geometry.AttributeStreamOfInt32"));
//            conf.registerSerialization(Class.forName("com.esri.core.geometry.AttributeStreamOfInt8"));
//            conf.registerSerialization(Class.forName("com.esri.core.geometry.AttributeStreamBase"));
//            conf.registerSerialization(Class.forName("[Lcom.esri.core.geometry.AttributeStreamBase;"));
//            conf.registerSerialization(Class.forName("com.esri.core.geometry.MultiPathImpl"));
//            conf.registerSerialization(double[].class);
//            conf.registerSerialization(int[].class);
//            conf.registerSerialization(VertexDescription.class);
//            conf.registerSerialization(MultiPath.class);
//            conf.registerSerialization(Attributes.class);
//            conf.registerSerialization(Point2D.class);
//            conf.registerSerialization(Point.class);
//            conf.registerSerialization(Envelope2D.class);
//            conf.registerSerialization(Envelope.class);
//            conf.registerSerialization(Polygon.class);
//            conf.registerSerialization(PolygonFeatureWritable.class);
//            conf.setFallBackOnJavaSerialization(false);

//                Mocked data
            MockedSources mockedSources = new MockedSources();
            final PolygonFeatureWritable newPolygonWritable1 = new PolygonFeatureWritable();
            final Polygon polygon1 = newPolygonWritable1.polygon;
            Envelope env = new Envelope(1000, 2000, 1010, 2010);
            polygon1.addEnvelope(env, false);
//            polygon1.startPath(0, 0);
//            polygon1.lineTo(10, 0);
//            polygon1.lineTo(10, 10);
//            polygon1.lineTo(0, 0);
//            polygon1.closeAllPaths();
            newPolygonWritable1.attributes.put(new Text("ADM2_CODE"), new IntWritable(4326));
            newPolygonWritable1.attributes.put(new Text("ADM2_NAME"), new Text("Test Region"));

            final PolygonFeatureWritable newPolygonWritable2 = new PolygonFeatureWritable();
            final Polygon polygon2 = newPolygonWritable2.polygon;
            polygon2.startPath(0, 0);
            polygon2.lineTo(20, 0);
            polygon2.lineTo(20, 20);
            polygon2.lineTo(0, 0);
            polygon2.closeAllPaths();
//            newPolygonWritable2.attributes.put(new Text("ADM2_CODE"), new IntWritable(1234));
//            newPolygonWritable2.attributes.put(new Text("ADM2_NAME"), new Text("Test Region2"));

            mockedSources.addMockData("1", new Values(newPolygonWritable1));


            CompleteTopologyParam topologyParam = new CompleteTopologyParam();
            topologyParam.setMockedSources(mockedSources);
            topologyParam.setStormConf(conf);
//            topologyParam.setTimeoutMs(100000);

            Map result = Testing.completeTopology(cluster, localTopology, topologyParam);
            logger.info(result);
            Assert.assertNotNull(result);
        });
    }
}
