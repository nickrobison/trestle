package com.nickrobison.trestle.server.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * Created by nickrobison on 1/5/18.
 */
@Path("/evaluation/{seg: .*}")
@Produces(MediaType.TEXT_HTML)
public class EvaluationSPAResource {
    private static final String ASSETS_INDEX_HTML = "build/evaluation.index.html";

    public EvaluationSPAResource() {

    }

    @GET
    public Response serveEvaluationIndex(@PathParam("seg") List<PathSegment> segments) {
        final InputStream webpage = WorkspaceSPAResource.class.getClassLoader().getResourceAsStream(ASSETS_INDEX_HTML);
        if (webpage == null) {
            final String errorMessage = String.format("Unable to load webpage from %s", ASSETS_INDEX_HTML);
            throw new WebApplicationException(new Throwable(errorMessage), javax.ws.rs.core.Response.Status.NOT_FOUND);
        }

        return Response.ok(webpage).build();
    }
}
