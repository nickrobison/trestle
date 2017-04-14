package com.nickrobison.trestle;

import com.google.inject.AbstractModule;
import com.nickrobison.metrician.MetricianModule;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 4/12/17.
 */
public class TrestleModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(TrestleModule.class);

    TrestleModule() {
//        Setup the ByteBuddy agent, for class path retransformation
        try {
            ByteBuddyAgent.getInstrumentation();
        } catch (IllegalStateException e) {
            try {
                ByteBuddyAgent.install();
            } catch (IllegalStateException es) {
                throw new IllegalStateException("Unable to attach Metrics Agent, possibly not running on JDK?", es);
            }
//            MetricianAgentBuilder.BuildAgent().installOnByteBuddyAgent();
        }
    }
    @Override
    protected void configure() {
//        install(new TrestleCacheModule());
        install(new MetricianModule());
    }
}
