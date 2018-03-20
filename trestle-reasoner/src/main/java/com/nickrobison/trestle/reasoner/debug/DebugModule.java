package com.nickrobison.trestle.reasoner.debug;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Singleton;

/**
 * Created by nickrobison on 3/19/18.
 */
public class DebugModule extends AbstractModule {

    private final boolean enabled;
    private final int port;

    public DebugModule() {
        final Config debugConfig = ConfigFactory.load().getConfig("trestle.debug");
        this.enabled = debugConfig.getBoolean("repl");
        this.port = debugConfig.getInt("port");
    }

    @Override
    protected void configure() {
        if (enabled) {
            //        Bind the port annotation
            bind(int.class)
                    .annotatedWith(REPLPort.class)
                    .toInstance(this.port);
//            Bind to the provider
            bind(IDebugREPL.class)
                    .toProvider(ClojureREPLProvider.class)
                    .in(Singleton.class);
        } else {
//            Bind to the no-op class
            bind(IDebugREPL.class)
                    .to(NoOpREPL.class)
                    .in(Singleton.class);
        }
    }
}
