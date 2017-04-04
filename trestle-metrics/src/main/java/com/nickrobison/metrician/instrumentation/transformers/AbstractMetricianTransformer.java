package com.nickrobison.metrician.instrumentation.transformers;

import afu.edu.emory.mathcs.backport.java.util.Collections;
import com.codahale.metrics.Metric;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.metrician.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Created by nrobison on 3/27/17.
 */

/**
 * Abstract class that implements byte-code transformation of the various {@link Metric}s
 */
public abstract class AbstractMetricianTransformer {

    public abstract AgentBuilder.Transformer getTransformer();
//    public AgentBuilder.Transformer getTransformer() {
//        return (builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(getAdviceClass()).on(getMethodElementMatcher()));
//    }

    protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
        return getIncludeTypeMatcher()
                .and(not(isInterface()))
                .and(not(isSynthetic()))
                .and(not(getExtraExcludeTypeMatcher()));
    }

    protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
        return getExtraIncludeTypeMatcher().and(getNarrowTypesMatcher());
    }

    protected ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
        return none();
    }

    protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
        return any();
    }

    protected ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
        return none();
    }

    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getMethodElementMatcher() {
        return not(isConstructor())
                .and(not(isAbstract()))
                .and(not(isNative()))
                .and(not(isFinal()))
                .and(not(isSynthetic()))
                .and(not(isTypeInitializer()))
                .and(getExtraMethodElementMatcher());
    }

    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return any();
    }

    protected Class<? extends AbstractMetricianTransformer> getAdviceClass() {
        return getClass();
    }

    private Advice.WithCustomMapping registerDynamicValues() {
        final List<MetricianDynamicValue<?>> dynamicValues = getDynamicValues();
        Advice.WithCustomMapping withCustomMapping = Advice.withCustomMapping();
        for (MetricianDynamicValue value : dynamicValues) {
            withCustomMapping = withCustomMapping.bind(value.getAnnotationClass(), value);
        }
        return withCustomMapping;
    }

    protected List<MetricianDynamicValue<?>> getDynamicValues() {
        return Collections.emptyList();
    }



    public static abstract class MetricianDynamicValue<T extends Annotation> extends Advice.DynamicValue.ForFixedValue<T> {
        public abstract Class<T> getAnnotationClass();
    }




//    Other garbage

    protected interface MetricFactory<T extends Metric> {
        T metric(String name, boolean absolute);
    }

    protected static <T extends Metric> AnnotatedMetric<T> metricAnnotation(Method method, Class<? extends Annotation> clazz, MetricFactory<T> factory) {
        if (method.isAnnotationPresent(clazz)) {
            final Annotation annotation = method.getAnnotation(clazz);
            final T metric = factory.metric(metricAnnotationName(annotation), metricAnnotationAbsolute(annotation));
            if (metric != null) {
                return new AnnotatedMetric.IsPresent<>(metric, annotation);
            }
        }
        return new AnnotatedMetric.IsNotPresent<>();
    }

    private static String metricAnnotationName(Annotation annotation) {
        if (Gauge.class.isInstance(annotation)) {
            return ((Gauge) annotation).name();
        } else if (ExceptionMetered.class.isInstance(annotation)) {
            return ((ExceptionMetered) annotation).name();
        } else if (Metered.class.isInstance(annotation)) {
            return ((Metered) annotation).name();
        } else if (Timed.class.isInstance(annotation)) {
            return ((Timed) annotation).name();
        } else if (CounterIncrement.class.isInstance(annotation)) {
            return ((CounterIncrement) annotation).name();
        } else if (CounterDecrement.class.isInstance(annotation)) {
            return ((CounterDecrement) annotation).name();
        } else {
            throw new IllegalArgumentException("Unsupported Metrics annotation (" + annotation.getClass().getName() + ")");
        }
    }

    private static boolean metricAnnotationAbsolute(Annotation annotation) {
        if (Gauge.class.isInstance(annotation))
            return ((Gauge) annotation).absolute();
        else if (ExceptionMetered.class.isInstance(annotation))
            return ((ExceptionMetered) annotation).absolute();
        else if (Metered.class.isInstance(annotation))
            return ((Metered) annotation).absolute();
        else if (Timed.class.isInstance(annotation))
            return ((Timed) annotation).absolute();
        else if (CounterIncrement.class.isInstance(annotation))
            return ((CounterIncrement) annotation).absolute();
        else if (CounterDecrement.class.isInstance(annotation))
            return ((CounterDecrement) annotation).absolute();
        else
            throw new IllegalArgumentException("Unsupported Metrics annotation (" + annotation.getClass().getName() + ")");
    }
}
