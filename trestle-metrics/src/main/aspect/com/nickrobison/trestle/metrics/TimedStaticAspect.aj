package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect TimedStaticAspect {

    pointcut timed() : execution(@Timed static * (@Metriced *).*(..));

    Object around(): timed() {
        final String methodSignature = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod().getName();
        final Timer timer = MetricStaticAspect.TIMERS.get(methodSignature).getMetric();
        final Timer.Context context = timer.time();
        try {
            return proceed();
        } finally {
            context.stop();
        }
    }
}
