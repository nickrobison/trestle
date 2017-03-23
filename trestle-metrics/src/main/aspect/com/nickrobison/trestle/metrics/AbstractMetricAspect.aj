package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/17/17.
 */
public abstract aspect AbstractMetricAspect {

    protected interface MetricFactory<T extends Metric> {
        T metric(String name, boolean absolute);
    }

    protected <T extends Metric> AnnotatedMetric<T> metricAnnotation(Method method, Class<? extends Annotation> clazz, MetricFactory<T> factory) {
        if (method.isAnnotationPresent(clazz)) {
            final Annotation annotation = method.getAnnotation(clazz);
            final T metric = factory.metric(metricAnnotationName(annotation), metricAnnotationAbsolute(annotation));
            if (metric != null) {
                return new AnnotatedMetric.IsPresent<>(metric, annotation);
            } else {
                return new AnnotatedMetric.IsNotPresent<>();
            }
        } else {
            return new AnnotatedMetric.IsNotPresent<>();
        }
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
            return invokeObject(this.method, this.object);
        }

        private static Object invokeObject(Method method, Object object) {
            try {
                return method.invoke(object);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Error while calling method (" + method + ")", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Error while calling method (" + method + ")", e);
            }
        }
    }
}
