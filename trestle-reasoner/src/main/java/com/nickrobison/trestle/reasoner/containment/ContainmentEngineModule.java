package com.nickrobison.trestle.reasoner.containment;

import com.google.inject.PrivateModule;

/**
 * Created by detwiler on 8/31/17.
 */
public class ContainmentEngineModule extends PrivateModule
{
    public ContainmentEngineModule() {
//        Not needed
    }
    @Override
    protected void configure() {
        bind(ContainmentEngine.class).to(ContainmentEngineImpl.class).asEagerSingleton();
        expose(ContainmentEngine.class);
    }
}
