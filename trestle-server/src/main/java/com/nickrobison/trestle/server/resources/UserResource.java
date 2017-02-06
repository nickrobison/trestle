package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 1/18/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserDAO userDAO;

    @Inject
    public UserResource(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GET
    @UnitOfWork
    @Valid
    public List<User> findByName(@AuthRequired({Privilege.ADMIN}) User user, @QueryParam("name") Optional<String> name) {
        if (name.isPresent()) {
            return userDAO.findByName(name.get());
        }
        return userDAO.findAll();
    }

    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_JSON)
    public long upsertUser(@AuthRequired({Privilege.ADMIN}) User admin, @NotNull @Valid User user) {
        return this.userDAO.create(user);
    }

    @GET
    @Path(("/{id}"))
    @UnitOfWork
    @Valid
    public Optional<User> findByID(@AuthRequired({Privilege.ADMIN}) User user, @PathParam("id") LongParam id) {
        return userDAO.findById(id.get());
    }

    @DELETE
    @Path(("/{id}"))
    @UnitOfWork
    public void deleteUser(@AuthRequired({Privilege.ADMIN}) User admin, @PathParam("id") LongParam id) {
        userDAO.deleteUser(id.get());
    }
}
