package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.nickrobison.trestle.metrics.transformer.GaugeTransformer;
import com.nickrobison.trestle.metrics.advice.MeterAdvice;
import com.nickrobison.trestle.metrics.advice.TimerAdvice;
import com.nickrobison.trestle.metrics.transformer.AbstractMetricianTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 3/27/17.
 */
public class MetricianTransformer {
//    private static final Logger logger = LoggerFactory.getLogger(MetricianTransformer.class);
//
////    @Override
//    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
//        final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();
//        final String className = typeDescription.getName();
//        final Metriced metriced = typeDescription.getInheritedAnnotations().ofType(Metriced.class).loadSilent();
//        final MethodList<MethodDescription.InDefinedShape> declaredMethods = typeDescription.getDeclaredMethods();
//        for (final MethodDescription.InDefinedShape method : declaredMethods) {
//            if (method.isStatic()) {
//                continue;
//            }
//
////                Metered
//
//            final AnnotatedMetric<Meter> exceptionMeter = metricAnnotation(method, ExceptionMetered.class, (name, absolute) -> {
//                final String finalName = name.isEmpty() ? method.getName() + "." + ExceptionMetered.DEFAULT_NAME_SUFFIX : strategy.resolveMetricName(name);
//                final MetricRegistry registry = strategy.resolveMetricRegistry(metriced.registry());
//                return registry.meter(absolute ? finalName : MetricRegistry.name(className, finalName));
//            });
//            if (exceptionMeter.isPresent()) {
//                MetricianInventory.meters.put(method.getName(), exceptionMeter);
//                logger.debug("Registered ExceptionMeter on {}", method);
//            }
//
//            final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
//                final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
//                final MetricRegistry registry = strategy.resolveMetricRegistry(metriced.registry());
//                return registry.meter(absolute ? finalName : MetricRegistry.name(className, finalName));
//            });
//            if (meter.isPresent()) {
//                MetricianInventory.meters.put(method.getName(), meter);
//                logger.debug("Registered Meter on {}", method);
//            }
//
//            //                Gauge
////
////            final AnnotatedMetric<Gauge> gaugeAnnotatedMetric;
////            gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
////                final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
////                final MetricRegistry registry = strategy.resolveMetricRegistry(metriced.registry());
////                final String registerName = absolute ? finalName : MetricRegistry.name(className, finalName);
////                try {
////                    final AbstractMetricAspect.ForwardingGauge gauge = registry.register(registerName, new ForwardingGauge(method, object));
////                    return gauge;
////                } catch(IllegalArgumentException e) {
////                    logger.debug("Gauge {} already registered", registerName);
////                    return null;
////                }
////            });
////            if (gaugeAnnotatedMetric.isPresent()) {
////                MetricianInventory.gauges.put(method.getName(), gaugeAnnotatedMetric);
////                logger.debug("Registered Gauge on {} of type {}", method, gaugeAnnotatedMetric.getMetric().getValue().getClass().getName());
////            }
//
////                Timer
//
//            final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
//                String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
//                MetricRegistry registry = strategy.resolveMetricRegistry(metriced.registry());
//                return registry.timer(absolute ? finalName : MetricRegistry.name(className, finalName));
//            });
//            if (timer.isPresent()) {
//                MetricianInventory.timers.put(method.getName(), timer);
//                logger.debug("Registered Timer on {}", method);
//            }
//        }
//
////        Implement advice
//        return builder
//                .visit(Advice.to(MeterAdvice.class).on(ElementMatchers.isAnnotatedWith(Metered.class)))
//                .visit(Advice.to(TimerAdvice.class).on(ElementMatchers.isAnnotatedWith(Timed.class)))
//                .visit(Advice.to(GaugeTransformer.class).on(ElementMatchers.isAnnotatedWith(com.codahale.metrics.annotation.Gauge.class)));
//    }
}
