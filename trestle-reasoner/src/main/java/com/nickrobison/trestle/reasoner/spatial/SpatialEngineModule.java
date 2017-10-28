package com.nickrobison.trestle.reasoner.spatial;

import com.google.inject.PrivateModule;

public class SpatialEngineModule extends PrivateModule {

    public SpatialEngineModule() {
//        Not needed
    }

    @Override
    protected void configure() {
        bind(SpatialEngine.class).to(SpatialEngineImpl.class).asEagerSingleton();
        expose(SpatialEngine.class);
    }
}
