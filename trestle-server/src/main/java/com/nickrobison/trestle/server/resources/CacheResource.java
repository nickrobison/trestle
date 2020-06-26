package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.caching.TrestleCacheStatistics;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/cache")
@AuthRequired({Privilege.DBA})
@Produces(MediaType.APPLICATION_JSON)
@Api("cache")
public class CacheResource {
    private final TrestleCache reasoner;


    @Inject
    public CacheResource(TrestleReasoner reasoner) {
        this.reasoner = reasoner.getCache();
    }

    @GET
    @Path("/index")
    @ApiOperation(value = "Get statistics for indexes",
            notes = "Gets all relevant statistics for Trestle indexes",
            response = TrestleCacheStatistics.class)
    @ApiResponses({
            @ApiResponse(code = 501, message = "Caching is disabled for this instance")
    })
    public Response getIndexStatistics() {
        @Nullable final TrestleCacheStatistics cacheStatistics = this.reasoner.getCacheStatistics();
        if (cacheStatistics == null) {
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                    .entity("Cache does not support statistics").build();
        }
        return Response.ok()
                .entity(cacheStatistics).build();
    }

    @GET
    @Path("/rebuild/{index}")
    @ApiOperation(value = "Rebuild index",
            notes = "Rebuild the specified index. Which will requiring locking the indexes for all users")
    @ApiResponses({
            @ApiResponse(code = 500, message = "Unable to rebuild the index"),
            @ApiResponse(code = 404, message = "Not a registered index")
    })
    public Response rebuildIndex(@NotNull @PathParam(value = "index") String index) {
        switch (index) {
            case "valid": {
                try {
                    this.reasoner.rebuildValidIndex();
                    return Response.ok().entity("Rebuilt Valid index").build();
                } catch (Exception e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
                }
            }
            case "db": {
                try {
                    this.reasoner.rebuildDBIndex();
                    return Response.ok().entity("Rebuilt DB index").build();
                } catch (Exception e) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
                }
            }
            default:
                return Response.status(Response.Status.NOT_FOUND).entity("Not a recognized cache").build();
        }
    }

    @GET
    @Path("/purge/{cache}")
    @ApiOperation(value = "Purge cache",
            notes = "Purge entries from the specified cache, and drop the relevant indexes")
    @ApiResponses({
            @ApiResponse(code = 404, message = "Not a registered cache")
    })
    public Response purgeCache(@PathParam("cache") String cacheToPurge) {
        if (cacheToPurge.equals("individual")) {
            this.reasoner.purgeIndividualCache();
            return Response.ok().entity("Successfully purged Individual cache").build();
        } else if (cacheToPurge.equals("object")) {
            this.reasoner.purgeObjectCache();
            return Response.ok().entity("Successfully purged Object cache").build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Not a recognized cache").build();
    }
}
