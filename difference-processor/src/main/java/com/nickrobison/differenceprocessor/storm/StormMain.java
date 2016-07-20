package com.nickrobison.differenceprocessor.storm;

import com.nickrobison.differenceprocessor.storm.bolts.NewShapeBolt;
import com.nickrobison.differenceprocessor.storm.spouts.SimplePolygonSpout;
import com.nickrobison.differenceprocessor.storm.utils.KryoFactory;
import com.nickrobison.differenceprocessor.storm.utils.StormRunner;
import org.apache.log4j.Logger;
import org.apache.storm.Config;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;

/**
 * Created by nrobison on 5/2/16.
 */
public class StormMain {

    private static final Logger logger = Logger.getLogger(StormMain.class);
    private static final int RUNTIME_IN_SECONDS = 60;

    private final TopologyBuilder builder;
    private final String topologyName;
    private final Config topologyConfig;
    private final int runtimeInSeconds;

    public StormMain(String topologyName) {
        builder = new TopologyBuilder();
        this.topologyName = topologyName;
        topologyConfig = createTopologyConfig();
        runtimeInSeconds = RUNTIME_IN_SECONDS;

        wireTopology();
    }

    public static void main(String[] args) throws InterruptedException, InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        String topologyName = StormMain.class.getSimpleName();

        logger.info("Setting up topology: " + topologyName);
        StormMain mainTopology = new StormMain(topologyName);

//        TODO(nrobison): Change this to check input args for whether or not to run in production
        boolean runLocally = true;
        if (runLocally) {
            logger.info("Running topology locally");
            mainTopology.runLocally();
        } else {
            logger.info("Running topology remotely");
            mainTopology.runRemotely();
        }

    }

    private static Config createTopologyConfig() {
        Config conf = new Config();
        conf.setDebug(false);
//        Custom KryoFactory to support ESRI polygons
        conf.setKryoFactory(KryoFactory.class);
        conf.setFallBackOnJavaSerialization(false);
        return conf;
    }

    private void wireTopology() {
        builder.setSpout("1", new SimplePolygonSpout(), 1);
        builder.setBolt("2", new NewShapeBolt(), 1).shuffleGrouping("1");

    }

    private void runLocally() throws InterruptedException {
        StormRunner.runTopologyLocally(builder.createTopology(), topologyName, topologyConfig, runtimeInSeconds);
    }

    private void runRemotely() throws InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        StormRunner.runTopologyRemotely(builder.createTopology(), topologyName, topologyConfig);
    }
}
