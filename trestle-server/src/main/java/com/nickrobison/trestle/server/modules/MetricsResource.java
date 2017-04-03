package com.nickrobison.trestle.server.modules;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by nrobison on 3/24/17.
 */
@Path("/metrics")
@AuthRequired({Privilege.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger logger = LoggerFactory.getLogger(MetricsResource.class);
    private final Metrician metrician;

    @Inject
    public MetricsResource(ReasonerModule reasonerModule) {
        this.metrician = reasonerModule.getReasoner().getMetricsEngine();
    }

    @GET
    public Response getMetrics() {
        return Response.ok(this.metrician.getMetricsHeader()).build();
    }

    @GET
    @Path("/metric/{metricID}")
    public Response getMetricValues(@NotEmpty @PathParam("metricID") String metricID) {
        logger.debug("Values for {}", metricID);
        return Response.ok().build();
    }
}
