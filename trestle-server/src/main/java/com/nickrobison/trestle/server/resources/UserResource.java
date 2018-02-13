package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.mindrot.BCrypt;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nrobison on 1/18/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/users")
@Api(value = "users")
public class UserResource {

    private final UserDAO userDAO;
    private final String salt;

    @Inject
    public UserResource(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.salt = BCrypt.gensalt();
    }

    @GET
    @UnitOfWork
    @Valid
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns a list of users",
            notes = "If a name fragment is provided, returns a list of users matching that name, otherwise returns all users",
            response = User.class,
            responseContainer = "List")
    public Response findByName(@AuthRequired({Privilege.ADMIN}) User user, @QueryParam("name") Optional<String> name) {
        if (name.isPresent()) {
            return ok(userDAO.findByName(name.get())).build();
        }
        return ok(userDAO.findAll()).build();
    }

    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new user or updates and existing one, returns the ID of the user record",
            notes = "If the specified user already exists, the existing record is updated. Otherwise, a new record is inserted into the database",
            response = Long.class)
    public Response createUser(@AuthRequired({Privilege.ADMIN}) User admin, User user) {
//        If the length is 60, we know that it's an unhashed (and thus modified password)
//        We enforce a maximum length of 59 for the passwords, at the UI level
        if (user.getPassword().length() != 60) {
            user.setPassword(BCrypt.hashpw(user.getPassword(), this.salt));
        }
        return ok(this.userDAO.create(user)).build();
    }

    @GET
    @Path(("/{id}"))
    @UnitOfWork
    @Valid
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves a user record by the specified ID",
            notes = "Return a user object by its given id",
            response = User.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot find user with specified id")
    })
    public Response findByID(@AuthRequired({Privilege.ADMIN}) User user, @PathParam("id") LongParam id) {
        final Optional<User> userOptional = userDAO.findById(id.get());
        if (userOptional.isPresent()) {
            return ok(userOptional.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity(String.format("Cannot find user with specified id: %s", id)).build();
    }

    @DELETE
    @Path(("/{id}"))
    @UnitOfWork
    @ApiOperation(value = "Delete user from the database",
            notes = "Deletes the specified user from the database")
    public void deleteUser(@AuthRequired({Privilege.ADMIN}) User admin, @PathParam("id") LongParam id) {
        userDAO.deleteUser(id.get());
    }
}
