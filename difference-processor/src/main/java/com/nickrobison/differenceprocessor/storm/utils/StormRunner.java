package com.nickrobison.differenceprocessor.storm.utils;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.generated.StormTopology;

/**
 * Created by nrobison on 5/2/16.
 */
public class StormRunner {

    private static final int MILLIS_IN_SEC = 1000;

    private StormRunner() {}

    public static void runTopologyLocally(StormTopology topology, String topologyName, Config config, int runtimeInSeconds) throws InterruptedException {

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(topologyName, config, topology);
//        FIXME(nrobison): I don't like this, fix it.
        Thread.sleep((long)runtimeInSeconds * MILLIS_IN_SEC);
        cluster.shutdown();
    }

    public static void runTopologyRemotely(StormTopology topology, String topologyName, Config config) throws InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        StormSubmitter.submitTopology(topologyName, config, topology);
    }
}
