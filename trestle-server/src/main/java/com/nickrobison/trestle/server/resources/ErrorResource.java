package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.models.UIError;
import com.nickrobison.trestle.server.models.UIErrorDAO;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Created by nickrobison on 2/5/18.
 */
@Path("/error")
@Api(value = "error")
public class ErrorResource {

    private static final Logger logger = LoggerFactory.getLogger("UIErrorLogger");
    private final UIErrorDAO errorDAO;

    @Inject
    public ErrorResource(UIErrorDAO errorDAO) {
        this.errorDAO = errorDAO;
    }


    @POST
    @Path("/report")
    @UnitOfWork
    @ApiOperation(value = "Reports a UI error to the server backend",
    notes = "Adds an error, along with the associated statck trace to the backend logging system. Returns a UUID of the logged error",
    response = UUID.class)
    public Response reportError(@Valid UIError error) throws Exception {
        final UUID errorID = this.errorDAO.create(error);
        logger.error("UI Error logged at {} with message: {}", error.getLocation(), error.getMessage());
        return Response.ok().entity(errorID).build();
    }
    }
