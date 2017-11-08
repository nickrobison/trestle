package com.nickrobison.trestle.server.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * Created by nrobison on 6/22/17.
 */
@Path("/static/{seg: .*}")
public class StaticResource {

    public StaticResource() {

    }

    @GET
    public Response serveJS(@PathParam("seg") List<PathSegment> segments) {
        if (segments.size() == 1) {
            final String jsPath = String.format("%s/%s", "build", segments.get(segments.size() - 1).getPath());
            final InputStream jsStream = ServerSPAResource.class.getClassLoader().getResourceAsStream(jsPath);
            return Response.ok(jsStream).build();
        }
        StringBuilder resourcePath = new StringBuilder();
        segments.forEach(seg -> {
            resourcePath.append("/");
            resourcePath.append(seg.getPath());
        });
        final String jsPath = String.format("%s%s", "build", resourcePath.toString());
        final InputStream jsStream = ServerSPAResource.class.getClassLoader().getResourceAsStream(jsPath);
        return Response.ok(jsStream).build();
    }
}
