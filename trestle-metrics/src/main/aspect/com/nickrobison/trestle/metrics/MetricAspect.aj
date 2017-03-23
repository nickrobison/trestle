package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.annotation.*;
import com.codahale.metrics.annotation.Metered;
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
public aspect MetricAspect extends AbstractMetricAspect {

    declare precedence: MetricStaticAspect, MetricAspect, *;

    declare parents:(@Metriced *) implements Profiled;

    private static final Logger logger = LoggerFactory.getLogger(MetricAspect.class);
    final Map<String, AnnotatedMetric<Gauge>> Profiled.gauges = new ConcurrentHashMap<>();
    final Map<String, AnnotatedMetric<Meter>> Profiled.meters = new ConcurrentHashMap<>();
    final Map<String, AnnotatedMetric<Timer>> Profiled.timers = new ConcurrentHashMap<>();
    final Map<String, AnnotatedMetric<Counter>> Profiled.counters = new ConcurrentHashMap<>();

    pointcut profiled(Profiled object): (execution((@Metriced Profiled+).new(..))) && this(object);

    after(final Profiled object): profiled(object) {
        final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();
        Class<?> clazz = object.getClass();
        logger.debug("Resolved class {} as Metriced", clazz.getName());
        do {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
//                FIXME(nrobison): Finish implementing duplicate checks
                final Class<?> type = clazz;
//                Metered
                final AnnotatedMetric<Meter> exceptionMeter = metricAnnotation(method, ExceptionMetered.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() + "." + ExceptionMetered.DEFAULT_NAME_SUFFIX : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (exceptionMeter.isPresent()) {
                    object.meters.put(method.getName(), exceptionMeter);
                    logger.debug("Registered ExceptionMeter on {}", method);
                }

                final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (meter.isPresent()) {
                    object.meters.put(method.getName(), meter);
                    logger.debug("Registered Meter on {}", method);
                }

//                Gauge

                final AnnotatedMetric<Gauge> gaugeAnnotatedMetric;
                gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    final String registerName = absolute ? finalName : MetricRegistry.name(type, finalName);
                    try {
                        final ForwardingGauge gauge = registry.register(registerName, new ForwardingGauge(method, object));
                        return gauge;
                    } catch(IllegalArgumentException e) {
                        logger.debug("Gauge {} already registered", registerName);
                        return null;
                    }
                });
                if (gaugeAnnotatedMetric.isPresent()) {
                    object.gauges.put(method.getName(), gaugeAnnotatedMetric);
                    logger.debug("Registered Gauge on {} of type {}", method, gaugeAnnotatedMetric.getMetric().getValue().getClass().getName());
                }

//                Timer

                final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.timer(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (timer.isPresent()) {
                    object.timers.put(method.getName(), timer);
                    logger.debug("Registered Timer on {}", method);
                }

//                Counter

                final AnnotatedMetric<Counter> incrementingCounter = metricAnnotation(method, CounterIncrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (incrementingCounter.isPresent()) {
                    object.counters.put(method.getName(), incrementingCounter);
                    logger.debug("Registered incrementing Counter {}", method);
                }

                final AnnotatedMetric<Counter> decrementingCounter = metricAnnotation(method, CounterDecrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.counter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (decrementingCounter.isPresent()) {
                    object.counters.put(method.getName(), decrementingCounter);
                }
            }
            clazz = clazz.getSuperclass();
        } while (Object.class.equals(clazz));
    }
}
