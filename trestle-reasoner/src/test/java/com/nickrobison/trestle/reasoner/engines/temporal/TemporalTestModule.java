package com.nickrobison.trestle.reasoner.engines.temporal;

import com.google.inject.AbstractModule;
import com.nickrobison.trestle.ontology.ReasonerPrefix;

/**
 * Created by nickrobison on 2/12/18.
 */
public class TemporalTestModule extends AbstractModule {

    TemporalTestModule() {

    }

    @Override
    protected void configure() {
        bind(String.class)
                .annotatedWith(ReasonerPrefix.class)
                .toInstance("http://test-prefix.com/");
    }
}
