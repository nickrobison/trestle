package com.nickrobison.trestle.server.modules;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import static com.nickrobison.trestle.server.tasks.UpdateInferenceTask.INFERENCE_TASK_NAME;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServerModule extends DropwizardAwareModule<TrestleServerConfiguration> {
  @Override
  protected void configure() {
    bindConstant().annotatedWith(Names.named(INFERENCE_TASK_NAME)).to("run inference task");
    bind(ManagedReasoner.class);
  }

  @Provides
  @Singleton
  public TrestleReasoner provideReasoner(ManagedReasoner m) {
    m.start();
    return m.getReasoner();
  }
}
