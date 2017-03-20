package com.nickrobison.trestle.metrics;

/**
 * Created by nrobison on 3/20/17.
 */
interface MetricPart<T, U> {
    U getData(T input);
    String getSuffix();
    String getMetricType();
    default String getMetricNameWithSuffix(String name) {
        return name + "." + getSuffix();
    }
}
