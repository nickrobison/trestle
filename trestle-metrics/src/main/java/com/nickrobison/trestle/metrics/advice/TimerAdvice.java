package com.nickrobison.trestle.metrics.advice;

import com.codahale.metrics.Timer;
import com.nickrobison.trestle.metrics.MetricianInventory;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * Created by nrobison on 3/27/17.
 */
public class TimerAdvice {

    @Advice.OnMethodEnter
    public static Timer.Context enter(@Advice.Origin Method method) {
        final String name = method.getName();
        System.out.println(String.format("Starting %s timer", name));
        final Timer timer = MetricianInventory.timers.get(name).getMetric();
        return timer.time();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter Timer.Context context) {
        System.out.println("Stopping timer");
        context.stop();
    }
}
