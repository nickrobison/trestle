package com.nickrobison.trestle.metrics.agent;

import java.lang.instrument.Instrumentation;

/**
 * Created by nrobison on 3/31/17.
 */
public class MetricianAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        MetricianAgentBuilder.BuildAgent().installOn(instrumentation);
    }
}
