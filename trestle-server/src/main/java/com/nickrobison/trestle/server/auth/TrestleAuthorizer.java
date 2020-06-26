package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;

import java.util.Set;

/**
 * Created by nickrobison on 6/26/20.
 */
public class TrestleAuthorizer implements io.dropwizard.auth.Authorizer<User> {

  @Override
  public boolean authorize(User principal, String role) {
    final Set<Privilege> requiredPrivs = Privilege.parsePrivileges(Integer.parseInt(role));
    return principal.getPrivilegeSet().containsAll(requiredPrivs);
  }
}
