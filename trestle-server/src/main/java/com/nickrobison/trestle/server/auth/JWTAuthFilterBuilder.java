package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;
import io.dropwizard.auth.AuthFilter;

/**
 * Created by nickrobison on 6/26/20.
 */
public class JWTAuthFilterBuilder extends AuthFilter.AuthFilterBuilder<String, User, AuthFilter<String, User>> {

  public JWTAuthFilterBuilder() {
    // Not used
  }

  @Override
  protected AuthFilter<String, User> newInstance() {
    return new com.nickrobison.trestle.server.auth.AuthFilter();
  }
}
