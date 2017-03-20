package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect MetricStaticAspect extends AbstractMetricAspect {

    private static final Logger logger = LoggerFactory.getLogger(MetricStaticAspect.class);
    static final Map<String, AnnotatedMetric<Gauge>> GAUGES = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Meter>> METERS = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Timer>> TIMERS = new ConcurrentHashMap<>();
    static final Map<String, AnnotatedMetric<Counter>> COUNTERS = new ConcurrentHashMap<>();

    pointcut profiled(): staticinitialization(@Metriced *);

    after(): profiled() {
        final Class<?> clazz = thisJoinPointStaticPart.getSignature().getDeclaringType();
        final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();

        logger.debug("Resolved class {} as Metriced", clazz.getName());
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
                    logger.debug("Registered ExceptionMeter on {}", method);
                }

                final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (meter.isPresent()) {
                    METERS.put(method.getName(), meter);
                    logger.debug("Registered Meter on {}", method);
                }

//                Gauges

                final AnnotatedMetric<Gauge> gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.register(absolute ? finalName : MetricRegistry.name(clazz, finalName), new ForwardingGauge(method, clazz));
                });
                if (gaugeAnnotatedMetric.isPresent()) {
                    GAUGES.put(method.getName(), gaugeAnnotatedMetric);
                    logger.debug("Registered Gauge on {} of type {}", method, gaugeAnnotatedMetric.getMetric().getValue().getClass().getName());
                }

//                Timers

                final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.timer(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (timer.isPresent()) {
                    TIMERS.put(method.getName(), timer);
                    logger.debug("Registered Timer on {}", method);
                }

//                Counters

                final AnnotatedMetric<Counter> incrementingCounter = metricAnnotation(method, CounterIncrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (incrementingCounter.isPresent()) {
                    COUNTERS.put(method.getName(), incrementingCounter);
                    logger.debug("Registered incrementing Counter {}", method);
                }

                final AnnotatedMetric<Counter> decrementingCounter = metricAnnotation(method, CounterDecrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(clazz.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(clazz, finalName));
                });
                if (decrementingCounter.isPresent()) {
                    COUNTERS.put(method.getName(), decrementingCounter);
                    logger.debug("Registered decrementing Counter on {}", method);
                }
            }
        }
    }
}
