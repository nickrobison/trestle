package com.nickrobison.metrician.instrumentation.transformers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterIncrement;
import com.nickrobison.metrician.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static com.nickrobison.metrician.instrumentation.MetricianInventory.*;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * Created by nrobison on 3/28/17.
 */

/**
 * Byte-code transformation to implemented the {@link CounterIncrement} and {@link CounterDecrement} annotations
 */
@SuppressWarnings({"argument.type.incompatible"})
public class CounterTransformer extends AbstractMetricianTransformer {
    private static final Logger logger = LoggerFactory.getLogger(CounterTransformer.class);
    @Override
    public AgentBuilder.Transformer getTransformer() {
        return ((builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(getAdviceClass()).on(isAnnotatedWith(CounterDecrement.class).or(isAnnotatedWith(CounterIncrement.class)))));
    }

    @Advice.OnMethodEnter
    public static void enter(@Advice.Origin Method method) {
        handleCounter(method);
    }

    public static void handleCounter(Method method) {
        final AnnotatedMetric<Counter> counterAnnotatedMetric = counters.get(method.getName());
        if (counterAnnotatedMetric == null) {
            final AnnotatedMetric<Counter> incrementingCounter = metricAnnotation(method, CounterIncrement.class, (name, absolute) -> {
                String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                try {
                    return registry.counter(absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName));
                } catch (IllegalArgumentException e) {
                    logger.error("Unable to register Incrementing Counter", e);
                    return null;
                }
            });
            if (incrementingCounter.isPresent()) {
                final CounterIncrement annotation = method.getAnnotation(CounterIncrement.class);
                if (annotation == null) {
                    logger.error("Missing annotation, not incrementing");
                    return;
                }
                counters.put(method.getName(), incrementingCounter);
                logger.debug("Registered Incrementing Counter on {}", method);
                incrementingCounter.getMetric().inc(annotation.amount());
            } else {
                final AnnotatedMetric<Counter> decrementingCounter = metricAnnotation(method, CounterDecrement.class, (name, absolute) -> {
                    String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                    try {
                        return registry.counter(absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName));
                    } catch (IllegalArgumentException e) {
                        logger.error("Unable to register Decrementing Counter", e);
                        return null;
                    }
                });
                if (decrementingCounter.isPresent()) {
                    final CounterDecrement annotation = method.getAnnotation(CounterDecrement.class);
                    if (annotation == null) {
                        logger.error("Missing annotation, not decrementing");
                        return;
                    }
                    counters.put(method.getName(), decrementingCounter);
                    logger.debug("Registered Decrementing Counter on {}", method);
                    decrementingCounter.getMetric().dec(annotation.amount());
                }
            }
        } else {
            if (method.isAnnotationPresent(CounterIncrement.class)) {
                final CounterIncrement annotation = method.getAnnotation(CounterIncrement.class);
                if (annotation == null) {
                    logger.error("Missing annotation, not incrementing");
                    return;
                }
                counterAnnotatedMetric.getMetric().inc(annotation.amount());
            } else {
                final CounterDecrement annotation = method.getAnnotation(CounterDecrement.class);
                if (annotation == null) {
                    logger.error("Missing annotation, not decrementing");
                    return;
                }
                counterAnnotatedMetric.getMetric().dec(annotation.amount());
            }
        }

    }


}
