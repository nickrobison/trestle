package com.nickrobison.trestle.server.resources;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import io.dropwizard.jersey.params.LongParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

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
    public Response getMetricValues(@NotEmpty @PathParam("metricID") String metricID, @NotNull @QueryParam("limit") long timeLimit ) {
        logger.debug("Values for {}, from {}", metricID, timeLimit);
        final Map<Long, Object> metricValues = this.metrician.getMetricValues(metricID, timeLimit);
        return Response.ok(metricValues).build();
    }
}
