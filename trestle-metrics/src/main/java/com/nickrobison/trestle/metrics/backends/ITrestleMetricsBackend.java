package com.nickrobison.trestle.metrics.backends;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

/**
 * Created by nrobison on 3/20/17.
 */
public interface ITrestleMetricsBackend {

    void shutdown(File exportFile);

    void exportData(File file);

    void registerGauge(String name, @Nullable Gauge<?> gauge);
    void removeGauge(String name);
    void registerCounter(String name, @Nullable Counter counter);
    void removeCounter(String name);
}
