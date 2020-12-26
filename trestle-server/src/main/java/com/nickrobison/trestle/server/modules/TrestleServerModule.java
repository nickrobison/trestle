package com.nickrobison.trestle.server.modules;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServerModule extends DropwizardAwareModule<TrestleServerConfiguration> {
  @Override
  protected void configure() {
    bind(ManagedReasoner.class);
  }

  @Provides
  @Singleton
  public TrestleReasoner provideReasoner(ManagedReasoner m) {
    return m.getReasoner();
  }
}
