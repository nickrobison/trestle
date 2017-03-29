package com.nickrobison.trestle.metrics.transformer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.trestle.metrics.AnnotatedMetric;
import com.nickrobison.trestle.metrics.MetricianInventory;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/**
 * Created by nrobison on 3/28/17.
 */
public class GaugeTransformer extends AbstractMetricianTransformer {

    private static final Logger logger = LoggerFactory.getLogger(GaugeTransformer.class);

    @Override
    public AgentBuilder.Transformer getTransformer() {
        return ((builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(getAdviceClass()).on(isConstructor())));
    }

    @Override
    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return isAnnotatedWith(Gauge.class);
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
        return isConstructor();
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.This Object thiz) {
        System.out.println("In gauge advice");
        setupGauge(thiz);
    }

    public static void setupGauge(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (!MetricianInventory.gauges.containsKey(method.getName())) {
                final Gauge annotation = method.getAnnotation(Gauge.class);
                if (annotation != null && isNoParamsAndNonVoid(method)) {
                    final String finalName = annotation.name().isEmpty() ? method.getName() : annotation.name();
                    final MetricRegistry registry = MetricianInventory.registry;
                    try {
                        final ForwardingGauge gauge = new ForwardingGauge(method, object);
                        registry.register(finalName, gauge);
                        final AnnotatedMetric<com.codahale.metrics.Gauge> isPresent = new AnnotatedMetric.IsPresent<>(gauge, annotation);
                        MetricianInventory.gauges.put(method.getName(), isPresent);
                    } catch (IllegalArgumentException e) {
                    }
                }
            } else {
                logger.warn("Metric already registered");
            }
        }
    }

    private static boolean isNoParamsAndNonVoid(Method method) {
        return method.getGenericParameterTypes().length == 0 && method.getReturnType() != Void.class;
    }

    protected static class ForwardingGauge implements com.codahale.metrics.Gauge<Object> {

        private final Method method;
        private final Object object;

        ForwardingGauge(Method method, Object object) {
            this.method = method;
            this.object = object;
            this.method.setAccessible(true);
        }

        @Override
        public Object getValue() {
            try {
                return this.method.invoke(this.object);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Error while calling method (" + method + ")", e);
            }
        }
    }
}
