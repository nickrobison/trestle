package com.nickrobison.trestle.metrics.backends;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;

import java.io.File;

/**
 * Created by nrobison on 3/20/17.
 */
public interface ITrestleMetricsBackend {

    void shutdown(File exportFile);

    void registerGauge(String name, Gauge<?> gauge);
    void removeGauge(String name);
    void registerCounter(String name, Counter counter);
    void removeCounter(String name);
}
