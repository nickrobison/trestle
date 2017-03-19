package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.annotation.*;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect MetricAspect extends AbstractMetricAspect {

    declare precedence: MetricStaticAspect, MetricAspect, *;

    declare parents:( @Metriced *) implements Profiled;

    final Map<String, AnnotatedMetric<Gauge>> Profiled.gauges = new ConcurrentHashMap<>();
    final Map<String, AnnotatedMetric<Meter>> Profiled.meters = new ConcurrentHashMap<>();
    final Map<String, AnnotatedMetric<Timer>> Profiled.timers = new ConcurrentHashMap<>();

    pointcut profiled(Profiled object): execution((@Metriced Profiled+).new(..)) && this(object);

    after(final Profiled object): profiled(object) {
        final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();
        Class<?> clazz = object.getClass();
        do {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }

                final Class<?> type = clazz;
                final AnnotatedMetric<Meter> exceptionMeter = metricAnnotation(method, ExceptionMetered.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() + "." + ExceptionMetered.DEFAULT_NAME_SUFFIX : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (exceptionMeter.isPresent()) {
                    object.meters.put(method.getName(), exceptionMeter);
                }

                final AnnotatedMetric<Gauge> gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    final MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.register(absolute ? finalName : MetricRegistry.name(type, finalName), new ForwardingGauge(method, object));
                });
                if (gaugeAnnotatedMetric.isPresent()) {
                    object.gauges.put(method.getName(), gaugeAnnotatedMetric);
                }

                final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.meter(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (meter.isPresent()) {
                    object.meters.put(method.getName(), meter);
                }

                final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    MetricRegistry registry = strategy.resolveMetricRegistry(type.getAnnotation(Metriced.class).registry());
                    return registry.timer(absolute ? finalName : MetricRegistry.name(type, finalName));
                });
                if (timer.isPresent()) {
                    object.timers.put(method.getName(), timer);
                }
            }
            clazz = clazz.getSuperclass();
        } while (Object.class.equals(clazz));
    }
}
