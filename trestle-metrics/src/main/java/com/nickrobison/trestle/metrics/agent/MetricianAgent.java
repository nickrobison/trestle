package com.nickrobison.trestle.metrics.agent;

import java.lang.instrument.Instrumentation;

/**
 * Created by nrobison on 3/31/17.
 */

/**
 * pre-main class to runtime load the MetricianAgent into the JVM {@link Instrumentation}
 */
public class MetricianAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        MetricianAgentBuilder.BuildAgent().installOn(instrumentation);
    }
}
