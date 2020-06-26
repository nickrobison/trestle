package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

/**
 * Created by nickrobison on 6/26/20.
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter extends io.dropwizard.auth.AuthFilter<String, User> {

  public AuthFilter() {
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
