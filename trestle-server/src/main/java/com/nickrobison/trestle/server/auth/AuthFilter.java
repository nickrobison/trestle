package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Created by nickrobison on 6/26/20.
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter extends io.dropwizard.auth.AuthFilter<String, User> {

  @Inject
  public AuthFilter(Authenticator<String, User> authenticator, Authorizer<User> authorizer) {
    this.authenticator = authenticator;
    this.authorizer = authorizer;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    final String token = containerRequestContext.getHeaderString("Authorization");
    if (token == null || token.isEmpty()) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    this.authenticate(containerRequestContext, token, "");
  }
}
