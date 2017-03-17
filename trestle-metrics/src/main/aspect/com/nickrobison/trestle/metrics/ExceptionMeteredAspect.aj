package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect ExceptionMeteredAspect {

    pointcut exceptionMetered(Profiled object) : execution(@ExceptionMetered !static * (@Metriced Profiled+).*(..)) && this(object);

    after(Profiled object) throwing (Throwable throwable) : exceptionMetered(object) {
        final String methodSignature = ((MethodSignature) thisEnclosingJoinPointStaticPart.getSignature()).getMethod().toString();
        final AnnotatedMetric<Meter> annotatedMetric = object.meters.get(methodSignature);
        if(annotatedMetric.getAnnotation(ExceptionMetered.class).cause().isInstance(throwable)) {
            annotatedMetric.getMetric().mark();
        }
    }
}
