package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.auth.UserCredentials;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jwt4j.JWTHandler;
import org.mindrot.BCrypt;
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
import java.util.Optional;

import static javax.ws.rs.core.Response.*;

/**
 * Created by nrobison on 1/19/17.
 */
@Path("/auth")
@Api(value = "auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResource.class);
    public static final String USER_IS_NOT_AUTHORIZED = "User is not authorized";
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
    @ApiOperation(value = "Authorize user, returning JWT token",
            notes = "Authorize user and return a new JWT token, if the username/password is correct",
            response = String.class)
    @ApiResponses({
            @ApiResponse(code = 401, message = USER_IS_NOT_AUTHORIZED)
    })
    public Response login(@Context ContainerRequestContext context, @Valid UserCredentials user) {
        final String name = user.getUsername();
        final Optional<User> userOptional = userDAO.findByUsername(name);
        if (userOptional.isPresent()) {
            final User dbUser = userOptional.get();
//            Validate password
            if (BCrypt.checkpw(user.getPassword(), dbUser.getPassword())) {
                logger.info("Logging in {}", name);
                final String jwtToken = jwtHandler.encode(dbUser);
                return ok(jwtToken).build();
            } else {
                logger.warn("Wrong password for user {}", name);
                return status(Status.UNAUTHORIZED).entity(USER_IS_NOT_AUTHORIZED).build();
            }
        }
        logger.warn("Unauthorized user {}", name);
        return status(Status.UNAUTHORIZED).entity(USER_IS_NOT_AUTHORIZED).build();
    }

    @POST
    @Path("/logout")
    @ApiOperation(value = "Logout user", notes = "Log user out of the system")
    public Response logout(@Context ContainerRequestContext context) {
        return ok().build();
    }
}
