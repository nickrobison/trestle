package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 1/18/17.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserDAO userDAO;

    @Inject
    public UserResource(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GET
    @UnitOfWork
    public List<User> findByName(@QueryParam("name") Optional<String> name) {
        if (name.isPresent()) {
            return userDAO.findByname(name.get());
        }
        return userDAO.findAll();
    }

    @POST
    @UnitOfWork
    public Response putUser(User user) {
        final long userID = userDAO.create(user);
        return Response.status(Response.Status.CREATED).entity(userID).build();
    }

    @GET
    @Path(("/{id}"))
    @UnitOfWork
    public Response findByID(@PathParam("id") LongParam id) {
        final Optional<User> user = userDAO.findById(id.get());
        return Response.status(Response.Status.OK).entity(user).build();
    }
}
