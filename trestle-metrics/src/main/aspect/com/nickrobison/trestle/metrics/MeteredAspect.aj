package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect MeteredAspect {

    pointcut metered(Profiled object) : execution(@Metered !static * (@Metriced Profiled+).*(..)) && this(object);

    before(Profiled object) : metered(object) {
        final String methodSignature = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod().getName();
        final Meter meter = object.meters.get(methodSignature).getMetric();
        meter.mark();
    }
}
