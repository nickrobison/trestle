package com.nickrobison.trestle.server.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * Created by nrobison on 11/28/16.
 */
@Path("/workspace/{seg: .*}")
@Produces(MediaType.TEXT_HTML)
public class WorkspaceSPAResource {
    private static final String ASSETS_INDEX_HTML = "build/index.html";

    public WorkspaceSPAResource() {
      // Not used
    }

    @GET
    public Response serveMainPage(@PathParam("seg") List<PathSegment> segments) {
        final InputStream webpage = WorkspaceSPAResource.class.getClassLoader().getResourceAsStream(ASSETS_INDEX_HTML);
        if (webpage == null) {
            final String errorMessage = String.format("Unable to load webpage from %s", ASSETS_INDEX_HTML);
            throw new WebApplicationException(new Throwable(errorMessage), Response.Status.NOT_FOUND);
        }

        return Response.ok(webpage).build();
    }
}
