package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.nickrobison.trestle.metrics.backends.H2MemoryBackend;
import com.nickrobison.trestle.metrics.backends.ITrestleMetricsBackend;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 3/16/17.
 */
public class TrestleMetrician {
    private static final Logger logger = LoggerFactory.getLogger(TrestleMetrician.class);

    private final MetricRegistry registry;
    private final ManyToManyConcurrentArrayQueue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final TrestleMetricsReporter trestleMetricsReporter;
    private final ITrestleMetricsBackend metricsBackend;

    public TrestleMetrician() {
        logger.info("Initializing Trestle Metrician");
        this.dataQueue = new ManyToManyConcurrentArrayQueue<>(100);
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
        final MetricsDecomposer metricsDecomposer = new MetricsDecomposer(new HashMap<>(), new ArrayList<>());
        final MetricsListener metricsListener = new MetricsListener(Optional.empty(), new HashMap<>(), new HashMap<>(), new ArrayList<>(), false, metricsDecomposer, registry, MetricFilter.ALL);
        trestleMetricsReporter = new TrestleMetricsReporter(registry, dataQueue, Optional.empty(), metricsDecomposer, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        metricsBackend = new H2MemoryBackend(this.dataQueue);
//        jmxReporter = JmxReporter.forRegistry(registry).build();
//        jmxReporter.start();
    }

    public void shutdown() {
        shutdown(null);
    }

    public void shutdown(File exportFile) {
        logger.info("Shutting down Trestle Metrician");
        metricsBackend.shutdown(exportFile);
    }

    public TrestleMetricsReporter getReporter() {
        return this.trestleMetricsReporter;
    }

    public MetricRegistry getRegistry() {
        return this.registry;
    }
}
