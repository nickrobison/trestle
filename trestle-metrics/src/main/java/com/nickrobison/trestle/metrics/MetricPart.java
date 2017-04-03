package com.nickrobison.trestle.metrics;

/**
 * Created by nrobison on 3/20/17.
 */

/**
 * Decomposed {@link com.codahale.metrics.Metric} representing one built-in metric (such as 5-minute rate)
 * @param <T> - Generic key type of {@link com.codahale.metrics.Metric}
 * @param <U> - Generic Value type
 */
interface MetricPart<T, U> {
    U getData(T input);
    String getSuffix();
    String getMetricType();
    default String getMetricNameWithSuffix(String name) {
        return name + "." + getSuffix();
    }
}
