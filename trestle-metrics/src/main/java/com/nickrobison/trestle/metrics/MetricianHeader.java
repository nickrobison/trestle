package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 3/24/17.
 */
@SuppressWarnings("WeakerAccess")
public class MetricianHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Long upTime;
    public final Long period;
    public final Map<String, String> meters;

    MetricianHeader(long upTime, long period, Map<String, Metric> meters) {
        this.upTime = upTime;
        this.period = period;
        this.meters = meters.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getClass().getName()));
    }
}
