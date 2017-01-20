package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.config.JWTConfig;
import com.nickrobison.trestle.server.models.User;
import jwt4j.JWTHandler;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 * Created by nrobison on 1/19/17.
 */
public class AuthDynamicFeature implements DynamicFeature {

    @Inject JWTConfig config;
    @Inject JWTHandler<User> handler;

    public AuthDynamicFeature() {
    }
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
//        Register class?
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass != null) {
            Arrays.stream(resourceClass.getAnnotations())
                    .filter(annotation -> annotation.annotationType().equals(AuthRequired.class))
                    .map(AuthRequired.class::cast)
                    .findAny()
                    .ifPresent(authRequired -> featureContext.register(getAuthFilter(authRequired.value())));
        }


        final Method resourceMethod = resourceInfo.getResourceMethod();
//        Look for parameters annotations
        if (resourceMethod != null) {
            Stream.of(resourceMethod.getParameterAnnotations())
                    .flatMap(Arrays::stream)
                    .filter(annotation -> annotation.annotationType().equals(AuthRequired.class))
                    .map(AuthRequired.class::cast)
                    .findAny()
                    .ifPresent(authRequired -> featureContext.register(getAuthFilter(authRequired.value())));
        }

    }

    private ContainerRequestFilter getAuthFilter(final Privilege[] requiredPrivileges) {
        return containerRequestContext -> {
            final String headerString = containerRequestContext.getHeaderString(config.getAuthHeader());
            if (headerString == null) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            final User user;
            try {
                user = this.handler.decode(headerString);
            } catch (Exception e) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            if (!user.getPrivileges().containsAll(Arrays.asList(requiredPrivileges))) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            containerRequestContext.setProperty("user", user);
        };
    }
}
