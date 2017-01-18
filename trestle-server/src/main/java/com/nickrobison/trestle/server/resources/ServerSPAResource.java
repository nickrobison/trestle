//package com.nickrobison.trestle.server.resources;
//
//import org.apache.commons.io.IOUtils;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.WebApplicationException;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import java.io.IOException;
//import java.io.InputStream;
//
///**
// * Created by nrobison on 11/28/16.
// */
//@Path("/")
//@Produces(MediaType.TEXT_HTML)
//public class ServerSPAResource {
//
//    public static final String ASSETS_INDEX_HTML = "build/index.html";
//
//    public ServerSPAResource() {}
//
//    @GET
//    public String serveMainPage() {
//        final InputStream webpage = ServerSPAResource.class.getClassLoader().getResourceAsStream(ASSETS_INDEX_HTML);
//        if (webpage == null) {
//            final String errorMessage = String.format("Unable to load webpage from %s", ASSETS_INDEX_HTML);
//            throw new WebApplicationException(errorMessage, Response.Status.NOT_FOUND);
//        }
//
//        try {
//            return IOUtils.toString(webpage, "UTF-8");
//        } catch (IOException e) {
//            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
//        }
//    }
//}
