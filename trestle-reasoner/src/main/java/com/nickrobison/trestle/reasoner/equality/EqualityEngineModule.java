package com.nickrobison.trestle.reasoner.equality;

import com.google.inject.PrivateModule;

public class EqualityEngineModule extends PrivateModule {

    public EqualityEngineModule() {
//        Not needed
    }
    @Override
    protected void configure() {
        bind(EqualityEngine.class).to(EqualityEngineImpl.class).asEagerSingleton();
        expose(EqualityEngine.class);
    }
}
