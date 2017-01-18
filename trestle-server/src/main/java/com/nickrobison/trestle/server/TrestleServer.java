package com.nickrobison.trestle.server;

import com.hubspot.dropwizard.guice.GuiceBundle;
import com.nickrobison.trestle.server.modules.TrestleServerModule;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServer extends Application<TrestleServerConfiguration> {

    public static void main(String[] args) throws Exception {
        new TrestleServer().run(args);
    }

    @Override
    public String getName() {
        return "trestle-server";
    }

    @Override
    public void initialize(Bootstrap<TrestleServerConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/build/", "/admin", "index.html"));

        final GuiceBundle<TrestleServerConfiguration> guiceBundle = GuiceBundle.<TrestleServerConfiguration>newBuilder()
                .addModule(new TrestleServerModule())
                .setConfigClass(TrestleServerConfiguration.class)
                .enableAutoConfig(getClass().getPackage().getName())
//                .setInjectorFactory((stage, modules) -> LifecycleInjector.builder()
//                        .inStage(stage)
//                        .withModules(modules)
//                        .build()
//                        .createInjector())
                .build();

        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public void run(TrestleServerConfiguration trestleServerConfiguration, Environment environment) throws Exception {
    }
}
