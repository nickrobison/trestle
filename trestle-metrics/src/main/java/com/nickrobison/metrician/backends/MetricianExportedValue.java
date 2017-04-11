package com.nickrobison.metrician.backends;

/**
 * Created by nrobison on 4/10/17.
 */
public class MetricianExportedValue {

    private final String metric;
    private final Long timestamp;

    public String getMetric() {
        return metric;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Object getValue() {
        return value;
    }

    private final Object value;

    MetricianExportedValue(String metric, Long timestamp, Object value) {
        this.metric = metric;
        this.timestamp = timestamp;
        this.value = value;
    }
}
