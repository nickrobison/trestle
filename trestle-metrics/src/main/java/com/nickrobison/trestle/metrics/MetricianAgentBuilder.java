package com.nickrobison.trestle.metrics;

import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.nickrobison.trestle.metrics.transformer.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Created by nrobison on 3/28/17.
 */
public class MetricianAgentBuilder {

    public static AgentBuilder.Identified.Extendable BuildAgent() {
        return new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.inheritsAnnotation(Metriced.class))
                .transform(new GaugeTransformer().getTransformer())
                .transform(new TimerTransformer().getTransformer())
                .transform(new MeterTransformer().getTransformer())
                .transform(new CounterTransformer().getTransformer())
                .transform(new ExceptionMeterTransformer().getTransformer());
    }
}
