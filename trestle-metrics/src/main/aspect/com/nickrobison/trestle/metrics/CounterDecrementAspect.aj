package com.nickrobison.trestle.metrics;

import com.codahale.metrics.Counter;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/19/17.
 */
public aspect CounterDecrementAspect {

    pointcut counterDec(Profiled object) : execution(@CounterDecrement !static * (@Metriced Profiled+).*(..)) && this(object);

    before(Profiled object) : counterDec(object) {
        final Method method = ((MethodSignature) thisJoinPointStaticPart.getSignature()).getMethod();
        final CounterDecrement annotation = method.getAnnotation(CounterDecrement.class);
        final Counter counter = object.counters.get(method.getName()).getMetric();
        counter.dec(annotation.amount());
    }
}
