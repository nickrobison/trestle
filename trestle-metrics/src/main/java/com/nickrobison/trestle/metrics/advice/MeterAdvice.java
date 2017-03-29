package com.nickrobison.trestle.metrics.advice;

import com.codahale.metrics.Meter;
import com.nickrobison.trestle.metrics.MetricianInventory;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/27/17.
 */
public class MeterAdvice {

    @Advice.OnMethodEnter
    public static void enter(@Advice.Origin Method method) {
        final String name = method.getName();
        System.out.println(String.format("Marking meter %s", name));
        final Meter meter = MetricianInventory.meters.get(name).getMetric();
        meter.mark();
    }
}
