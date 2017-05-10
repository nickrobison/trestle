package com.nickrobison.metrician;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 3/20/17.
 */

/**
 * Scheduled class which parses {@link MetricRegistry} {@link Metric}s at the specified interval and sends them to the {@link com.nickrobison.metrician.backends.IMetricianBackend}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MetricianReporter extends ScheduledReporter {

    private static final Logger logger = LoggerFactory.getLogger(MetricianReporter.class);

    private final Clock clock;
    private final Optional<String> prefix;
    private final MetricsDecomposer decomposer;
    private final Queue<DataAccumulator> dataQueue;

    MetricianReporter(MetricRegistry registry,
                      BlockingQueue<DataAccumulator> dataQueue,
                      Optional<String> prefix,
                      MetricsDecomposer decomposer,
                      MetricFilter filter,
                      TimeUnit rateUnit,
                      TimeUnit durationUnit) {
        super(registry, "trestle-reporter", filter, rateUnit, durationUnit);
        clock = Clock.defaultClock();
        this.prefix = prefix;
        this.decomposer = decomposer;
        this.dataQueue = dataQueue;
        logger.info("Instantiated Trestle Metrics Reporter");
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        if (gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()) {
            return;
        }

        final long timestamp = clock.getTime();

        final DataAccumulator accumulator = new DataAccumulator(timestamp, this.prefix);
        processGauges(accumulator, gauges);
        processCounters(accumulator, counters);
        processMeters(accumulator, meters);
        processHistograms(accumulator, histograms);
        processTimers(accumulator, timers);
        logger.trace("Reported metrics {}", accumulator);
        if (!accumulator.getCounters().isEmpty() || !accumulator.getGauges().isEmpty()) {
            final boolean offer = this.dataQueue.offer(accumulator);
            if (!offer) {
                logger.error("Unable to add data to queue");
            }
        }
    }

    private static void processGauges(DataAccumulator accumulator, Map<String, Gauge> gauges) {
        gauges.forEach((key, value) -> accumulator.addGauge(key, value.getValue()));
    }

    private static void processCounters(DataAccumulator accumulator, Map<String, Counter> counters) {
        counters.forEach((key, value) -> accumulator.addCounter(key, value.getCount()));
    }

    private void processMeters(DataAccumulator accumulator, Map<String, Meter> meters) {
        meters.entrySet().forEach(entry -> {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(entry.getKey());
            streamer.countings().forEach(metricPart -> accumulator.addSubCounter(metricPart, entry));
            streamer.metered().forEach(metricPart -> accumulator.addSubGauge(metricPart, entry));
        });
    }

    private void processHistograms(DataAccumulator accumulator, Map<String, Histogram> histograms) {
        histograms.entrySet().forEach(entry -> {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(entry.getKey());
            streamer.countings().forEach(metricPart -> accumulator.addSubCounter(metricPart, entry));
            streamer.samplings().forEach(metricPart -> accumulator.addSubGauge(metricPart, entry));
        });
    }

    private void processTimers(DataAccumulator accumulator, Map<String, Timer> timers) {
        timers.entrySet().forEach(entry -> {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(entry.getKey());
            streamer.countings().forEach(metricPart -> accumulator.addSubCounter(metricPart, entry));
            streamer.metered().forEach(metricPart -> accumulator.addSubGauge(metricPart, entry));
            streamer.samplings().forEach(metricPart -> accumulator.addSubGauge(metricPart, entry));
        });
    }

    public class DataAccumulator {
        private final Map<String, Double> gauges = new HashMap<>();
        private final Map<String, Long> counters = new HashMap<>();
        private final Optional<String> prefix;
        private final long timestamp;

        private DataAccumulator(long timestamp, Optional<String> prefix) {
            this.prefix = prefix;
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public Map<String, Double> getGauges() {
            return gauges;
        }

        public Map<String, Long> getCounters() {
            return counters;
        }

        private DataAccumulator addCounter(String name, long value) {
            final String finalName = this.prefix.map(p -> p + name).orElse(name);
            counters.put(finalName, value);
            return this;
        }

        private DataAccumulator addGauge(String name, Object value) {
            final String finalName = this.prefix.map(p -> p + name).orElse(name);
            if (value instanceof BigDecimal) {
                gauges.put(finalName, ((BigDecimal) value).doubleValue());
            } else if (value instanceof BigInteger) {
                gauges.put(finalName, ((BigInteger) value).doubleValue());
            } else if (value != null && value.getClass().isAssignableFrom(Double.class)) {
                if (!Double.isNaN((Double) value) && Double.isFinite((Double) value)) {
                    gauges.put(finalName, (Double) value);
                }
            } else if (value != null && value instanceof Number) {
                gauges.put(finalName, ((Number) value).doubleValue());
            }
            return this;
        }

        private <T> DataAccumulator addSubCounter(MetricPart<T, Long> metricPart, Map.Entry<String, ? extends T> counterEntry) {
            final String nameWithSuffix = metricPart.getMetricNameWithSuffix(counterEntry.getKey());
            final String finalName = this.prefix.map(p -> p + nameWithSuffix).orElse(nameWithSuffix);
            counters.put(finalName, metricPart.getData(counterEntry.getValue()));
            return this;
        }

        private <T> DataAccumulator addSubGauge(MetricPart<T, Object> metricPart, Map.Entry<String, ? extends T> gaugeEntry) {
            final String metricNameWithSuffix = metricPart.getMetricNameWithSuffix(gaugeEntry.getKey());
            final String finalName = this.prefix.map(p -> p + metricNameWithSuffix).orElse(metricNameWithSuffix);
            final Object value = metricPart.getData(gaugeEntry.getValue());
            if (value instanceof BigDecimal) {
                gauges.put(finalName, ((BigDecimal) value).doubleValue());
            } else if (value != null && value.getClass().isAssignableFrom(Double.class) && !Double.isNaN((Double) value) && Double.isFinite((Double) value)) {
                gauges.put(finalName, (Double) value);
            }
            return this;
        }

        @Override
        public String toString() {
            return "DataAccumulator{" +
                    "gauges=" + gauges +
                    ", counters=" + counters +
                    '}';
        }
    }
}
