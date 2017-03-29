package com.nickrobison.trestle.metrics.instrumentation.transformers;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.nickrobison.trestle.metrics.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.nickrobison.trestle.metrics.instrumentation.MetricianInventory.*;
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
        return isAnnotatedWith(com.codahale.metrics.annotation.Gauge.class);
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
        return isConstructor();
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.This Object thiz) {
        setupGauge(thiz);
    }

    public static void setupGauge(Object object) {
//        Try to step through the entire heirarchy, in order to find gauges that are declared on superclasses of the instantiated object
        Class<?> clazz = object.getClass();
        while (clazz != Object.class) {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (!gauges.containsKey(method.getName())) {
                    final com.codahale.metrics.annotation.Gauge annotation = method.getAnnotation(com.codahale.metrics.annotation.Gauge.class);
                    if (annotation != null && isNoParamsAndNonVoid(method)) {

                        AnnotatedMetric<Gauge> gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
                            final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                            final String registerName = absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName);
                            try {
                                final ForwardingGauge gauge = registry.register(registerName, new ForwardingGauge(method, object));
                                return gauge;
                            } catch (IllegalArgumentException e) {
                                logger.debug("Gauge {} already registered", registerName);
                                return null;
                            }
                        });
                        if (gaugeAnnotatedMetric.isPresent()) {
                            gauges.put(method.getName(), gaugeAnnotatedMetric);
                            logger.debug("Registered Gauge {} method {}", gaugeAnnotatedMetric.getMetric().getValue().getClass().getName(), method.getName());
                        } else {
                            logger.debug("Missing Gauge for method {}", method.getName());
                        }
                    }
                } else {
                    logger.debug("Gauge already registered for {}", method.getName());
                }
            }
            clazz = clazz.getSuperclass();
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
