package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Counter;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/19/17.
 */
public aspect CounterIncrementAspect {

    pointcut counterInc(Profiled object) : execution(@CounterIncrement !static * (@Metriced Profiled+).*(..)) && this(object);

    before(Profiled object) : counterInc(object) {
        final Method method = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod();
        final CounterIncrement annotation = method.getAnnotation(CounterIncrement.class);
        final Counter counter = object.counters.get(method.getName()).getMetric();
        counter.inc(annotation.amount());
    }
}
