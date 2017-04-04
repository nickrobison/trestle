package com.nickrobison.metrician;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 3/24/17.
 */

/**
 * Initial header to detail the current state of the Metrics registry.
 * Includes things like currently registered Metrics, uptime, and update period.
 */
@SuppressWarnings("WeakerAccess")
public class MetricianHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Long upTime;
    public final Long period;
    public final Map<String, String> meters;

    /**
     * Default constructor
     * @param upTime - Current uptime (in ms)
     * @param period - Update period (in ms)
     * @param meters - {@link Map} of {@link Metric} along with their registry keys
     */
    MetricianHeader(long upTime, long period, Map<String, Metric> meters) {
        this.upTime = upTime;
        this.period = period;
        this.meters = meters.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getClass().getName()));
    }
}
