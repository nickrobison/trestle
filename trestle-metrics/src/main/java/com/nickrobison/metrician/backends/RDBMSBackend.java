package com.nickrobison.metrician.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.nickrobison.metrician.MetricianReporter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 4/2/17.
 */

/**
 * Abstract class for shipping metrics to relational database backend
 */
public abstract class RDBMSBackend implements IMetricianBackend {

    private static final Logger logger = LoggerFactory.getLogger(RDBMSBackend.class);
    final int thread_wait;
    final Config config;
    final Map<String, Long> metricMap;
    final Thread eventThread;
    final String threadName;

    final BlockingQueue<MetricianReporter.DataAccumulator> dataQueue;
    protected Connection connection;

    RDBMSBackend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue, String threadName) {
        this.threadName = threadName;
        this.dataQueue = dataQueue;
        this.config = ConfigFactory.load().getConfig("trestle.metrics.backend");
        this.thread_wait = this.config.getInt("threadWait");
        this.metricMap = new HashMap<>();

        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, this.threadName);
        eventThread.start();
    }

    @Override
    public void shutdown() {
        shutdown(null);
    }

    abstract Long registerMetric(String metricName);

    @Override
    public abstract void exportData(File file);

    @Override
    public void registerGauge(String name, @Nullable Gauge<?> gauge) {
        logger.debug("Registering Gauge {}", name);
        registerMetric(name);
    }

    @Override
    public void registerCounter(String name, @Nullable Counter counter) {
        logger.debug("Registering counter {}", name);
        registerMetric(name);
    }

    @Override
    public void removeGauge(String name) {

    }

    @Override
    public void removeCounter(String name) {

    }

    abstract void insertValues(List<MetricianMetricValue> events);

    protected class ProcessEvents implements Runnable {

        @Override
        public void run() {
            while (true) {
                final MetricianReporter.DataAccumulator event;
                try {
                    event = dataQueue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    logger.debug("Thread interrupted, draining queue");
                    ArrayDeque<MetricianReporter.DataAccumulator> remainingEvents = new ArrayDeque<>();
                    dataQueue.drainTo(remainingEvents);
                    remainingEvents.forEach(this::processEvent);
                    logger.debug("Finished draining events");
                    return;
                }
            }
        }

        private void processEvent(MetricianReporter.DataAccumulator event) {
            final long timestamp = event.getTimestamp();
            List<MetricianMetricValue> events = new ArrayList<>();
//            Counters
            event.getCounters().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.trace("Counter {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.COUNTER, metricKey, timestamp, entry.getValue()));
//                sqlInsertString.append(String.format("INSERT INTO metrics.counters VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
            });
//            Gauges
            event.getGauges().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.trace("Gauge {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.GAUGE, metricKey, timestamp, entry.getValue()));
//                sqlInsertString.append(String.format("INSERT INTO metrics.gauges VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
            });
            insertValues(events);
        }
    }
}
