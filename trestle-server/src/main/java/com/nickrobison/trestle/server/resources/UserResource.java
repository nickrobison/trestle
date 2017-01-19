package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    @Valid
    public List<User> findByName(@QueryParam("name") Optional<String> name) {
        if (name.isPresent()) {
            return userDAO.findByname(name.get());
        }
        return userDAO.findAll();
    }

    @POST
    @UnitOfWork
    public long putUser(@NotNull @Valid User user) {
        return userDAO.create(user);
    }

    @GET
    @Path(("/{id}"))
    @UnitOfWork
    @Valid
    public Optional<User> findByID(@PathParam("id") LongParam id) {
        return userDAO.findById(id.get());
    }
}
