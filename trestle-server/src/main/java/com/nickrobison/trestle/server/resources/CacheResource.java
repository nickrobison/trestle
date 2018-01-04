package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.caching.TrestleCacheStatistics;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/cache")
@AuthRequired({Privilege.DBA})
@Produces(MediaType.APPLICATION_JSON)
public class CacheResource {
    private final TrestleCache cache;


    @Inject
    public CacheResource(ReasonerModule reasonerModule) {
        this.cache = reasonerModule.getReasoner().getCache();
    }

    @GET
    @Path("/index")
    public Response getIndexStatistics() {
        @Nullable final TrestleCacheStatistics cacheStatistics = this.cache.getCacheStatistics();
        if (cacheStatistics == null) {
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                    .entity("Cache does not support statistics").build();
        }
        return Response.ok()
                .entity(cacheStatistics).build();
    }

    @GET
    @Path("/rebuild/{index}")
    public Response rebuildIndex(@NotNull @PathParam(value = "index") String index) {
        switch (index) {
            case "valid": {
                try {
                    this.cache.rebuildValidIndex();
                    return Response.ok().entity("Rebuilt Valid index").build();
                } catch (Exception e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
                }
            }
            case "db": {
                try {
                    this.cache.rebuildDBIndex();
                    return Response.ok().entity("Rebuilt DB index").build();
                } catch (Exception e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
                }
            }
            default: return Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not a recognized cache").build();
        }
    }
}
