package com.nickrobison.metrician;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.nickrobison.metrician.backends.ITrestleMetricsBackend;
import com.nickrobison.metrician.agent.MetricianAgentBuilder;
import com.nickrobison.metrician.instrumentation.MetricianInventory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

import javax.inject.Singleton;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 3/21/17.
 */

/**
 * DI Module which sets up the {@link ByteBuddyAgent} to handle the class transformaion and provides the various service injectors
 */
public class MetricsModule extends PrivateModule {

    private final Config config;

    public MetricsModule() {
        config = ConfigFactory.load().getConfig("trestle.metrics");
//        Setup/Reset bytebuddy
        SharedMetricRegistries.clear();
        MetricianInventory.reset();
//        Try to attach, unless already attached
//        We do this in order to be able to run our test suite correctly, simply installing ByteBuddy will result in duplicate Transformers on the classpath, so this helps avoid that.
//        This should have no effect when running normally.
        try {
            ByteBuddyAgent.getInstrumentation();
        } catch (IllegalStateException e) {
            try {
                ByteBuddyAgent.install();
            } catch (IllegalStateException es) {
                throw new IllegalStateException("Unable to attach Metrics Agent, possibly not running on JDK?", es);
            }
            MetricianAgentBuilder.BuildAgent().installOnByteBuddyAgent();
        }
    }

    @Override
    protected void configure() {
        final String backendClass = config.getString("backend.class");
        try {
            final Class<? extends ITrestleMetricsBackend> backend = Class.forName(backendClass).asSubclass(ITrestleMetricsBackend.class);

            bind(ITrestleMetricsBackend.class).to(backend).asEagerSingleton();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load metrics backend class", e);
        }
        bind(Metrician.class).asEagerSingleton();
        expose(Metrician.class);
    }

    @Provides
    MetricRegistry provideRegistry() {
        return SharedMetricRegistries.getOrCreate(config.getString("registryName"));
    }

    @Provides
    @Singleton
    BlockingQueue<MetricianReporter.DataAccumulator> provideDataQueue() {
        return new ArrayBlockingQueue<>(this.config.getInt("queueSize"));
    }
}
