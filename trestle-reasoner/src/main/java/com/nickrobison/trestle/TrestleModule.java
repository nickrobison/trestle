package com.nickrobison.trestle;

import com.google.inject.AbstractModule;
import com.nickrobison.metrician.MetricianModule;

import com.nickrobison.trestle.caching.TrestleCacheModule;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 4/12/17.
 */
public class TrestleModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(TrestleModule.class);
    private final boolean metricsEnabled;
    private final boolean cachingEnabled;

    TrestleModule(boolean metricsEnabled, boolean cachingEnabled) {
        logger.debug("Building Trestle Module");
        this.metricsEnabled = metricsEnabled;
        this.cachingEnabled = cachingEnabled;
    }

//    TrestleModule() {
////        Setup the ByteBuddy agent, for class path retransformation
//        try {
//            ByteBuddyAgent.getInstrumentation();
//        } catch (IllegalStateException e) {
//            try {
//                ByteBuddyAgent.install();
//            } catch (IllegalStateException es) {
//                throw new IllegalStateException("Unable to attach Metrics Agent, possibly not running on JDK?", es);
//            }
////            MetricianAgentBuilder.BuildAgent().installOnByteBuddyAgent();
//        }
//    }
    @Override
    protected void configure() {
        install(new MetricianModule(metricsEnabled));
        if (cachingEnabled) {
            logger.debug("Installing Trestle Cache Module");
            install(new TrestleCacheModule());
        }
    }
}
