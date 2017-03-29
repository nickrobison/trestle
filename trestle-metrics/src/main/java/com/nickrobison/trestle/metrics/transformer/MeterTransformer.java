package com.nickrobison.trestle.metrics.transformer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.metrics.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static com.nickrobison.trestle.metrics.MetricianInventory.*;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * Created by nrobison on 3/28/17.
 */
@SuppressWarnings("WeakerAccess")
public class MeterTransformer extends AbstractMetricianTransformer {
    private static final Logger logger = LoggerFactory.getLogger(MeterTransformer.class);

    @Override
    public AgentBuilder.Transformer getTransformer() {

        return (builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(MeterTransformer.class).on(isAnnotatedWith(Metered.class)));
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return isAnnotatedWith(Metered.class);
    }

    @Advice.OnMethodEnter
    public static void enter(@Advice.Origin Method method) {
        handleMeter(method);
    }

    public static void handleMeter(Method method) {
        final AnnotatedMetric<Meter> annotatedMetric = meters.get(method.getName());
        if (annotatedMetric == null) {
            final AnnotatedMetric<Meter> meter = metricAnnotation(method, Metered.class, (name, absolute) -> {
                String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
//                MetricRegistry registry = strategy.resolveMetricRegistry(metriced.registry());
                return registry.meter(absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName));
            });
            meters.put(method.getName(), meter);
            logger.debug("Registered Meter on {}", method);
            meter.getMetric().mark();
        } else {
            annotatedMetric.getMetric().mark();
        }
    }
}
