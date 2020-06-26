package com.nickrobison.trestle.server.auth;

import com.google.inject.TypeLiteral;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import com.nickrobison.trestle.server.models.User;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

/**
 * Created by nickrobison on 6/26/20.
 */
public class AuthModule extends DropwizardAwareModule<TrestleServerConfiguration> {

  @Override
  protected void configure() {
    final TypeLiteral<Authenticator<String, User>> authenticatorTypeLiteral = new TypeLiteral<Authenticator<String, User>>() {
    };
    final TypeLiteral<Authorizer<User>> authorizerTypeLiteral = new TypeLiteral<Authorizer<User>>() {
    };
    bind(authenticatorTypeLiteral).to(TrestleAuthenticator.class);
    bind(authorizerTypeLiteral).to(TrestleAuthorizer.class);
  }
}
