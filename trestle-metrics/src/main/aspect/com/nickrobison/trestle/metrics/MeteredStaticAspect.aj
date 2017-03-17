package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect MeteredStaticAspect {

    pointcut metered() : execution(@Metered static * (@Metriced *).*(..));

    before() : metered() {
        final String methodSignature = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod().getName();
        final Meter metric = MetricStaticAspect.METERS.get(methodSignature).getMetric();
        metric.mark();
    }
}
