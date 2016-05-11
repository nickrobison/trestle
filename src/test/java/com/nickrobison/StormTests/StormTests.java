package com.nickrobison.StormTests;

import org.apache.storm.Config;
import org.apache.storm.testing.MkClusterParam;
import org.junit.Before;

/**
 * Created by nrobison on 5/4/16.
 */
public class StormTests {
    protected MkClusterParam mkClusterParam;
    protected Config daemonConfig;
//    protected MockedSources mockedSources;

    @Before
    public void setupCluster() {
        //        Reset cluster config to new state
        mkClusterParam = new MkClusterParam();
        mkClusterParam.setSupervisors(1);
        daemonConfig = new Config();
        daemonConfig.put(Config.STORM_CLUSTER_MODE, "local");
        daemonConfig.setDebug(false);
        mkClusterParam.setDaemonConf(daemonConfig);
    }
}
