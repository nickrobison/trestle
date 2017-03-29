package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.metrics.backends.ITrestleMetricsBackend;
import com.nickrobison.trestle.metrics.instrumentation.MetricianAgentBuilder;
import com.nickrobison.trestle.metrics.instrumentation.MetricianInventory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

import javax.inject.Singleton;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 3/21/17.
 */
public class MetricsModule extends PrivateModule {

    private Config config;

    MetricsModule() {
//        Setup/Reset bytebuddy
        SharedMetricRegistries.clear();
        MetricianInventory.reset();
//        Try to attach, unless already attached
//        We do this in order to be able to run our test suite correctly, simply installing ByteBuddy will result in duplicate Transformers on the classpath, so this helps avoid that.
//        This should have no effect when running normally.
        try {
            ByteBuddyAgent.getInstrumentation();
        } catch (IllegalStateException e) {
            ByteBuddyAgent.install();
            MetricianAgentBuilder.BuildAgent().installOnByteBuddyAgent();
        }
    }

    @Override
    protected void configure() {

        config = ConfigFactory.load().getConfig("trestle.metrics");
        final String backendClass = config.getString("backend");
        try {
            final Class<? extends ITrestleMetricsBackend> backend = Class.forName(backendClass).asSubclass(ITrestleMetricsBackend.class);

            bind(ITrestleMetricsBackend.class).to(backend).asEagerSingleton();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load metrics backend class", e);
        }
        bind(TrestleMetrician.class).asEagerSingleton();
        expose(TrestleMetrician.class);
    }

    @Provides
    MetricRegistry provideRegistry() {
        return SharedMetricRegistries.getOrCreate(config.getString("registryName"));
    }

    @Provides
    @Singleton
    BlockingQueue<TrestleMetricsReporter.DataAccumulator> provideDataQueue() {
        return new ArrayBlockingQueue<>(this.config.getInt("queueSize"));
    }
}
