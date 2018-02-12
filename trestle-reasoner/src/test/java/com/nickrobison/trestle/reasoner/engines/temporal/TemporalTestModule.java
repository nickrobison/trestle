package com.nickrobison.trestle.reasoner.engines.temporal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Named;

/**
 * Created by nickrobison on 2/12/18.
 */
public class TemporalTestModule extends AbstractModule {

    TemporalTestModule() {

    }

    @Override
    protected void configure() {

    }

    @Provides
    @Named("reasonerPrefix")
    public String reasonerPrefix() {
        return "http://test-prefix.com/";
    }
}
