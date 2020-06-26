package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.Collections;

/**
 * Created by nickrobison on 6/26/20.
 */
public class TrestleAuthorizer implements io.dropwizard.auth.Authorizer<User> {

  @Override
  public boolean authorize(User principal, String role, @Nullable ContainerRequestContext requestContext) {
    final int mask = Privilege.buildPrivilageMask(Collections.singleton(Privilege.valueOf(role)));
    return (principal.getPrivileges() & mask) > 0;
  }

  @Override
  public boolean authorize(User principal, String role) {
    // Not used, deprecated
    return false;
  }
}
