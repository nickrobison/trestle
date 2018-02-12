package com.nickrobison.trestle.server.resources;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * Created by nrobison on 6/22/17.
 */
@Path("/static/{seg: .*}")
public class StaticResource {

    private static final String BROTLI = "br";
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String GZIP = "gzip";
    private final ClassLoader classLoader;

    public StaticResource() {
        classLoader = StaticResource.class.getClassLoader();
    }

    @GET
    public Response serveJS(@HeaderParam("Accept-Encoding") String encoding, @Context HttpServletResponse response, @PathParam("seg") List<PathSegment> segments) {
        if (segments.size() == 1) {
            final String jsPath = String.format("%s/%s", "build", segments.get(segments.size() - 1).getPath());
            final InputStream jsStream = this.fetchJSResource(encoding, jsPath, response);
            return Response.ok(jsStream).build();
        }
        StringBuilder resourcePath = new StringBuilder();
        segments.forEach(seg -> {
            resourcePath.append("/");
            resourcePath.append(seg.getPath());
        });
        final String jsPath = String.format("%s%s", "build", resourcePath.toString());
        final InputStream jsStream = classLoader.getResourceAsStream(jsPath);
        return Response.ok(jsStream).build();
    }

    /**
     * Looks at the Accept-Encoding headers from the {@link HttpServletResponse} and tries to serve either Brotli or GZIP compressed assets
     * If it can't find a matching compressed asset, it attempts to return the original uncompressed version, if it exists
     *
     * @param acceptEncoding - {@link String} of accepted encodings. e.g: br, gzip, deflate
     * @param jsPath         - {@link String} asset path to append compression suffix to
     * @param response       - {@link HttpServletResponse} to set Content-Encoding headers on
     * @return - {@link InputStream} of asset response, returns a null if no asset exists
     */
    private InputStream fetchJSResource(String acceptEncoding, String jsPath, HttpServletResponse response) {
//        Try to serve compressed resources, starting with Brotli and then going down to GZIP

        if (acceptEncoding.contains(BROTLI)) {
            final InputStream brotliResource = classLoader.getResourceAsStream(jsPath + ".br");
            if (brotliResource != null) {
                response.setHeader(CONTENT_ENCODING, BROTLI);
                return brotliResource;
            }
        }

        if (acceptEncoding.contains(GZIP)) {
            final InputStream gzipResource = classLoader.getResourceAsStream(jsPath + ".gz");
            if (gzipResource != null) {
                response.setHeader(CONTENT_ENCODING, GZIP);
                return gzipResource;
            }
        }

//        Finally, fetch the jsResource and return it, even if it's null
        return classLoader.getResourceAsStream(jsPath);
    }
}
