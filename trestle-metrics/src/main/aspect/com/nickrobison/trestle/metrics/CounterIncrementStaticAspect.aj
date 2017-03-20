package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Counter;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/19/17.
 */
public aspect CounterIncrementStaticAspect {

    pointcut counterInc() : execution(@CounterIncrement static * (@Metriced *).*(..));

    before() : counterInc() {
        final Method method = ((MethodSignature) thisEnclosingJoinPointStaticPart.getSignature()).getMethod();
        final CounterIncrement annotation = method.getAnnotation(CounterIncrement.class);
        final Counter counter = MetricStaticAspect.COUNTERS.get(method.getName()).getMetric();
        counter.inc(annotation.amount());
    }
}
