package com.nickrobison.trestle.server.modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import static com.nickrobison.trestle.server.tasks.UpdateInferenceTask.INFERENCE_TASK_NAME;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServerModule extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named(INFERENCE_TASK_NAME)).to("run inference task");
    }
}
