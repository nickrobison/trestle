package com.nickrobison.trestle.metrics.transformer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.nickrobison.trestle.metrics.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static com.nickrobison.trestle.metrics.MetricianInventory.meters;
import static com.nickrobison.trestle.metrics.MetricianInventory.registry;
import static com.nickrobison.trestle.metrics.MetricianInventory.strategy;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * Created by nrobison on 3/29/17.
 */
public class ExceptionMeterTransformer extends AbstractMetricianTransformer {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMeterTransformer.class);

    @Override
    public AgentBuilder.Transformer getTransformer() {
        return ((builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(getAdviceClass()).on(isAnnotatedWith(ExceptionMetered.class))));
    }

    @Advice.OnMethodExit(onThrowable = Exception.class, inline = false)
    public static void enter(@Advice.Origin Method method, @Advice.Thrown Throwable e) {
        handleException(method, e);
    }

    public static void handleException(Method method, Throwable thrown) {
        final ExceptionMetered annotation = method.getAnnotation(ExceptionMetered.class);
        if (annotation != null) {
            AnnotatedMetric<Meter> meter = meters.get(method.getName());
            if (meter == null) {
                final AnnotatedMetric<Meter> exceptionMeter = metricAnnotation(method, ExceptionMetered.class, (name, absolute) -> {
                    final String finalName = name.isEmpty() ? method.getName() + "." + ExceptionMetered.DEFAULT_NAME_SUFFIX : strategy.resolveMetricName(name);
                    return registry.meter(absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName));
                });
                meters.put(method.getName(), exceptionMeter);
                meter = exceptionMeter;
                logger.debug("Registered Exception meter on {}, catching {}", method, annotation.cause());
            }
            if (annotation.cause().isAssignableFrom(thrown.getClass())) {
                meter.getMetric().mark();
            } else {
                logger.debug("Not marking meter, watching for {} exception, but {} was thrown", annotation.cause().getName(), thrown.getClass().getName());
            }
        }
    }
}
