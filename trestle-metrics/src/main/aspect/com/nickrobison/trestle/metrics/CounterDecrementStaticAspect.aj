package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Counter;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/19/17.
 */
public aspect CounterDecrementStaticAspect {

    pointcut counterDec() : execution(@CounterDecrement static * (@Metriced *).*(..));

    before() : counterDec() {
        final Method method = ((MethodSignature) thisEnclosingJoinPointStaticPart.getSignature()).getMethod();
        final CounterDecrement annotation = method.getAnnotation(CounterDecrement.class);
        final Counter counter = MetricStaticAspect.COUNTERS.get(method.getName()).getMetric();
        counter.dec(annotation.amount());
    }
}
