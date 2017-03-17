package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Metric;

import java.lang.annotation.Annotation;

/**
 * Created by nrobison on 3/17/17.
 */
public interface AnnotatedMetric<T extends Metric> {

    boolean isPresent();

    T getMetric();

    <A extends Annotation> A getAnnotation(Class<A> clazz);

    class IsPresent<T extends Metric> implements AnnotatedMetric<T> {
        private final T metric;
        private final Annotation annotation;

        public IsPresent(T metric, Annotation annotation) {
            this.metric = metric;
            this.annotation = annotation;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public T getMetric() {
            return this.metric;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends Annotation> A getAnnotation(Class<A> clazz) {
            return (A) this.annotation;
        }
    }

    class IsNotPresent<T extends Metric> implements AnnotatedMetric<T> {

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public T getMetric() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> clazz) {
            throw new UnsupportedOperationException();
        }
    }

}
