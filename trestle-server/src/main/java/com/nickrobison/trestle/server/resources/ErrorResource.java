package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.models.UIError;
import com.nickrobison.trestle.server.models.UIErrorDAO;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by nickrobison on 2/5/18.
 */
@Path("/error")
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
    public Response reportError(@Valid UIError error) throws Exception {
        final Long errorID = this.errorDAO.create(error);
        logger.error("UI Error logged at {} with message: {}", error.getLocation(), error.getMessage());
        return Response.ok().entity(errorID).build();
    }
}
