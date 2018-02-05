package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.models.UIError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public ErrorResource() {

    }


    @POST
    @Path("/report")
    public Response reportError(@Valid UIError error) {
        logger.error("UI Error logged at {} with message: {}", error.getLocation(), error.getMessage());
        return Response.ok().entity("Logged as 12353").build();
    }
}
