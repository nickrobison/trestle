package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.auth.UserCredentials;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import jwt4j.JWTHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by nrobison on 1/19/17.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResource.class);
    private final UserDAO userDAO;
    private final JWTHandler<User> jwtHandler;

    @Inject
    public AuthenticationResource(UserDAO userDAO, JWTHandler<User> jwtHandler) {

        this.userDAO = userDAO;
        this.jwtHandler = jwtHandler;
    }

    @POST
    @UnitOfWork
    @Path("/login")
    public Response login(@Context ContainerRequestContext context, @Valid UserCredentials user) {
        final String name = user.getUsername();
        final List<User> users = userDAO.findByname(name);
        if (!users.isEmpty()) {
            logger.info("Logging in {}", name);
            final String jwtToken = jwtHandler.encode(users.get(0));
            return Response.ok(jwtToken).build();
        }
        logger.warn("Unauthorized user {}", name);
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@Context ContainerRequestContext context) {
        return Response.ok().build();
    }
}
