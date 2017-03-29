package com.nickrobison.trestle.metrics.instrumentation;

import com.nickrobison.trestle.annotations.metrics.Metriced;
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
                .transform(new com.nickrobison.trestle.metrics.transformer.GaugeTransformer().getTransformer())
                .transform(new com.nickrobison.trestle.metrics.transformer.TimerTransformer().getTransformer())
                .transform(new com.nickrobison.trestle.metrics.transformer.MeterTransformer().getTransformer())
                .transform(new com.nickrobison.trestle.metrics.transformer.CounterTransformer().getTransformer())
                .transform(new com.nickrobison.trestle.metrics.transformer.ExceptionMeterTransformer().getTransformer());
    }
}
