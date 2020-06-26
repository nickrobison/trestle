package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.config.JWTConfig;
import com.nickrobison.trestle.server.models.User;
import jwt4j.JWTHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by nickrobison on 6/26/20.
 */
public class TrestleAuthenticator implements io.dropwizard.auth.Authenticator<String, User> {

  private static final Logger logger = LoggerFactory.getLogger(TrestleAuthenticator.class);

  JWTConfig config;
  JWTHandler<User> handler;

  @Inject
  public TrestleAuthenticator(JWTConfig config, JWTHandler<User> handler) {
    this.config = config;
    this.handler = handler;
  }



  @Override
  public Optional<User> authenticate(String s) {
    final User user;
    try {
      user = this.handler.decode(s);
      return Optional.of(user);
    } catch (Exception e) {
      logger.error("Unable to decode token", e);
    }
    return Optional.empty();
  }
}
