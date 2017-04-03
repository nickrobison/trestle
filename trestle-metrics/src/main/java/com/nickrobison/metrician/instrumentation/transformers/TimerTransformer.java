package com.nickrobison.metrician.instrumentation.transformers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.AnnotatedMetric;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static com.nickrobison.metrician.instrumentation.MetricianInventory.*;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * Created by nrobison on 3/28/17.
 */

/**
 * Byte-code transformation class to implement the {@link Timed} annotation
 */
public class TimerTransformer extends AbstractMetricianTransformer {

    private static final Logger logger = LoggerFactory.getLogger(TimerTransformer.class);

    @Override
    public AgentBuilder.Transformer getTransformer() {
        return ((builder, typeDescription, classLoader, module) -> builder.visit(Advice.to(getAdviceClass()).on(isAnnotatedWith(Timed.class))));
    }

    @Override
    protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
        return isAnnotatedWith(Timed.class);
    }

    @Advice.OnMethodEnter
    public static Timer.Context enter(@Advice.Origin Method method) {
        return handleTimer(method);
    }


    public static Timer.Context handleTimer(Method method) {
        final AnnotatedMetric<Timer> annotatedMetric = timers.get(method.getName());
        if (annotatedMetric == null) {
            final AnnotatedMetric<Timer> timer = metricAnnotation(method, Timed.class, (name, absolute) -> {
                String finalName = name.isEmpty() ? method.getName() : strategy.resolveMetricName(name);
                return registry.timer(absolute ? finalName : MetricRegistry.name(method.getDeclaringClass(), finalName));
            });
            timers.put(method.getName(), timer);
            logger.debug("Registered Timer on {}", method);
            return timer.getMetric().time();
        } else {
            return annotatedMetric.getMetric().time();
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter Timer.Context context) {
        context.stop();
    }
}
