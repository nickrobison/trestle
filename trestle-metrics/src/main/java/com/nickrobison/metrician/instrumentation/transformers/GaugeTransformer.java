package com.nickrobison.metrician.instrumentation.transformers;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.nickrobison.metrician.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static com.nickrobison.metrician.instrumentation.MetricianInventory.*;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;

/**
 * Created by nrobison on 3/28/17.
 */

/**
 * Byte-code transformation class to implement the {@link Gauge} annotation
 * Currently set to excecute when the class constructor is called.
 */
@SuppressWarnings({"dreference.of.nullable"})
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
        while (clazz != Object.class && clazz != null) {
            for (final Method method : clazz.getDeclaredMethods()) {
                processMethod(object, method);
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Process the input method to determine if we need to get a {@link Gauge} from it
     * @param object - {@link Object} input object
     * @param method - {@link Method} method object to process
     */
    private static void processMethod(Object object, Method method) {
        if (!gauges.containsKey(method.getName())) {
            final com.codahale.metrics.annotation.Gauge annotation = method.getAnnotation(com.codahale.metrics.annotation.Gauge.class);
            if (annotation != null && isNoParamsAndNonVoid(method)) {
                extractGauge(object, method);
            }
        } else {
            logger.debug("Gauge already registered for {}", method.getName());
        }
    }

    /**
     * Extract the {@link Gauge}, since we now know one exists
     * @param object - {@link Object} input object
     * @param method - {@link Method} method to extract {@link Gauge} from
     */
    private static void extractGauge(Object object, Method method) {
        AnnotatedMetric<Gauge> gaugeAnnotatedMetric = metricAnnotation(method, com.codahale.metrics.annotation.Gauge.class, (name, absolute) -> {
            final String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
            final String registerName = absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName);
            try {
                return registry.register(registerName, new ForwardingGauge(method, object));
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to register Gauge", e);
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

    private static boolean isNoParamsAndNonVoid(Method method) {
        return method.getGenericParameterTypes().length == 0 && method.getReturnType() != Void.class;
    }

    protected static class ForwardingGauge implements com.codahale.metrics.Gauge<Object> {

        private final Object object;
        private final MethodHandle mh;

        ForwardingGauge(Method method, Object object) {
            try {
                method.setAccessible(true);
                this.mh = MethodHandles.lookup().unreflect(method);
                method.setAccessible(false);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(String.format("Cannot access method %s", method.getName()), e);
            }
            this.object = object;

        }

        @Override
        public Object getValue() {
            try {
                return this.mh.invoke(this.object);
            } catch (Throwable throwable) {
                throw new IllegalStateException(String.format("Error while calling method %s", this.mh.toString()), throwable);
            }
        }
    }
}
