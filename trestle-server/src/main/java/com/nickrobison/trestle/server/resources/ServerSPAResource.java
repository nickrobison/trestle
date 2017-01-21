package com.nickrobison.trestle.server.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 11/28/16.
 */
@Path("/admin/{seg: .*}")
@Produces(MediaType.TEXT_HTML)
public class ServerSPAResource {
    private static final Logger logger = LoggerFactory.getLogger(ServerSPAResource.class);

    public static final String ASSETS_INDEX_HTML = "build/index.html";

    public ServerSPAResource() {
    }

    @GET
    public Response serveMainPage(@PathParam("seg") List<PathSegment> segments) {
        final Optional<String> staticPath = segments.stream().map(PathSegment::getPath).filter(path -> path.contains("static")).findAny();
        if (staticPath.isPresent()) {
            final String jsPath = String.format("%s/%s", "build", segments.get(segments.size() - 1).getPath());
            logger.info("Returning JS at path {}", jsPath);
            final InputStream jsStream = ServerSPAResource.class.getClassLoader().getResourceAsStream(jsPath);
            return Response.ok(jsStream).build();


        }

        final InputStream webpage = ServerSPAResource.class.getClassLoader().getResourceAsStream(ASSETS_INDEX_HTML);
        if (webpage == null) {
            final String errorMessage = String.format("Unable to load webpage from %s", ASSETS_INDEX_HTML);
            throw new WebApplicationException(errorMessage, Response.Status.NOT_FOUND);
        }

        return Response.ok(webpage).build();
//
//        try {
//            return Response.status(Response.Status.OK).entity(IOUtils.toString(webpage, "UTF-8")).build();
//        } catch (IOException e) {
//            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
//        }
    }
}
