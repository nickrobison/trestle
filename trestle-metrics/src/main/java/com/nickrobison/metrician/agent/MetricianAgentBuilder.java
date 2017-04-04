package com.nickrobison.metrician.agent;

import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.nickrobison.metrician.instrumentation.transformers.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 3/28/17.
 */

/**
 * Simple builder class for {@link net.bytebuddy.agent.ByteBuddyAgent}
 */
public class MetricianAgentBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MetricianAgentBuilder.class);

    public static AgentBuilder.Identified.Extendable BuildAgent() {
        logger.info("Building Metrician Metrics Agent");
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
