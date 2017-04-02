package com.nickrobison.trestle.metrics.backends;

/**
 * Created by nrobison on 4/2/17.
 */

/**
 * Simple class for moving a Metric value from the abstract backends to the RDBMS specific implementations
 * Metrics are a key/value pair with the Key being a conjunction of both metricKey and timestamp
 * Supported metrics are given using the {@link ValueType} enum.
 * @param <V> - Generic value type for event. Currently either a {@link Long} or {@link Double}
 */
public class MetricianMetricValue<V> {

    private final ValueType type;
    private final Long key;
    private final long timestamp;
    private final V value;

    MetricianMetricValue(ValueType type, Long key, long timstamp, V value) {
        this.type = type;
        this.key = key;
        this.timestamp = timstamp;
        this.value = value;
    }

    public ValueType getType() {
        return this.type;
    }

    public Long getKey() {
        return key;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public V getValue() {
        return value;
    }

    enum ValueType {
        GAUGE,
        COUNTER
    }
}
