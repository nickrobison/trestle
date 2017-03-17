package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Created by nrobison on 3/17/17.
 */
public aspect TimedAspect {

    pointcut timed(Profiled object) : execution(@Timed !static * (@Metriced Profiled+).*(..)) && this(object);

    Object around(Profiled object) : timed(object) {
        final String methodSignature = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod().getName();
        final Timer timer = object.timers.get(methodSignature).getMetric();
        final Timer.Context context = timer.time();
        try {
            return proceed(object);
        } finally {
            context.stop();
        }
    }
}
