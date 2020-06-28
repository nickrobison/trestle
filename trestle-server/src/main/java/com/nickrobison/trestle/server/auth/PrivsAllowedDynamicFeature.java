package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.annotations.PrivilegesAllowed;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * Created by nickrobison on 6/26/20.
 *
 * This {@link DynamicFeature} is pulled directly from the {@link org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature} from Dropwizard.
 * It's slightly modified to allow us to handle {@link PrivilegesAllowed} annotations.
 */
@Provider
public class PrivsAllowedDynamicFeature implements DynamicFeature {

  @Override
  public void configure(final ResourceInfo resourceInfo, final FeatureContext configuration) {
    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

    // DenyAll on the method take precedence over RolesAllowed and PermitAll
    if (am.isAnnotationPresent(DenyAll.class)) {
      configuration.register(new PrivsAllowedRequestFilter());
      return;
    }

    // RolesAllowed on the method takes precedence over PermitAll
    PrivilegesAllowed ra = am.getAnnotation(PrivilegesAllowed.class);
    if (ra != null) {
      configuration.register(new PrivsAllowedRequestFilter(ra.value()));
      return;
    }

    // PermitAll takes precedence over RolesAllowed on the class
    if (am.isAnnotationPresent(PermitAll.class)) {
      // Do nothing.
      return;
    }

    // DenyAll can't be attached to classes

    // RolesAllowed on the class takes precedence over PermitAll
    ra = resourceInfo.getResourceClass().getAnnotation(PrivilegesAllowed.class);
    if (ra != null) {
      configuration.register(new PrivsAllowedRequestFilter(ra.value()));
    }
  }

  @Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
  private static class PrivsAllowedRequestFilter implements ContainerRequestFilter {

    private final boolean denyAll;
    private final Privilege[] privsAllowed;

    PrivsAllowedRequestFilter() {
      this.denyAll = true;
      this.privsAllowed = null;
    }

    PrivsAllowedRequestFilter(final Privilege[] privsAllowed) {
      this.denyAll = false;
      this.privsAllowed = (privsAllowed != null) ? privsAllowed : new Privilege[]{};
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
      if (!denyAll && privsAllowed != null) {
        if (privsAllowed.length > 0 && !isAuthenticated(requestContext)) {
          throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED());
        }

        for (final Privilege role : privsAllowed) {
          if (requestContext.getSecurityContext().isUserInRole(role.getName())) {
            return;
          }
        }
      }

      throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED());
    }

    private static boolean isAuthenticated(final ContainerRequestContext requestContext) {
      return requestContext.getSecurityContext().getUserPrincipal() != null;
    }
  }
}
