package com.nickrobison.trestle.reasoner.individual;

import com.google.inject.PrivateModule;

public class IndividualEngineModule extends PrivateModule {

    public IndividualEngineModule() {
//        Not needed
    }

    @Override
    protected void configure() {
        bind(IndividualEngine.class).to(IndividualEngineImpl.class).asEagerSingleton();
        expose(IndividualEngine.class);
    }
}
