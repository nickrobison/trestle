package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect MetricStaticAspect extends AbstractMetricAspect {

    static final Map<String, AnnotatedMetric<Gauge>> GAUGES = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Meter>> METERS = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Timer>> TIMERS = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Counter>> COUNTERS = new ConcurrentHashMap<>();

    pointcut profiled(): staticinitialization(@Metriced *);

    after(): profiled() {
        final Class<?> clazz = thisJoinPointStaticPart.getSignature().getDeclaringType();
        final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();

        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && !method.isSynthetic()) {

//                Meters

                final AnnotatedMetric<Meter> exceptionMeter = metricAnnotation(method, ExceptionMetered.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() + "." + ExceptionMetered.DEFAULT_NAME_SUFFIX : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (exceptionMeter.isPresent()) {
                    METERS.put(method.getName(), exceptionMeter);
                }

                final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (meter.isPresent()) {
                    METERS.put(method.getName(), meter);
                }

//                Gauges

                final AnnotatedMetric<Gauge> gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.register(absolute ? finalName : MetricRegistry.name(clazz, finalName), new ForwardingGauge(method, clazz));
                });
                if (gaugeAnnotatedMetric.isPresent()) {
                    GAUGES.put(method.getName(), gaugeAnnotatedMetric);
                }

//                Timers

                final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.timer(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (timer.isPresent()) {
                    TIMERS.put(method.getName(), timer);
                }

//                Counters

                final AnnotatedMetric<Counter> incrementingCounter = metricAnnotation(method, CounterIncrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (incrementingCounter.isPresent()) {
                    COUNTERS.put(method.getName(), incrementingCounter);
                }

                final AnnotatedMetric<Counter> decrementingCounter = metricAnnotation(method, CounterDecrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (decrementingCounter.isPresent()) {
                    COUNTERS.put(method.getName(), decrementingCounter);
                }
            }
        }
    }
}
