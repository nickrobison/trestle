package com.nickrobison.trestle.server.resources;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.metrician.backends.MetricianExportedValue;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Created by nrobison on 3/24/17.
 */
@Path("/metrics")
@AuthRequired({Privilege.ADMIN})
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger logger = LoggerFactory.getLogger(MetricsResource.class);
    public static final String CSV_SEPARATOR = ",";
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
    public Response getMetricValues(@NotEmpty @PathParam("metricID") String metricID, @NotNull @QueryParam("start") Long startTemporal, @QueryParam("end") Long endTemporal) {
        logger.debug("Values for {}, from {}, to {}", metricID, startTemporal, endTemporal);
        final Map<Long, Object> metricValues = this.metrician.getMetricValues(metricID, startTemporal, endTemporal);
        return Response.ok(metricValues).build();
    }

    @POST
    @Path("/export")
    @Produces(MediaType.TEXT_PLAIN)
    public Response exportMetricValues(@Valid MetricsQueryRequest metrics) {
        final List<MetricianExportedValue> exportedMetrics = this.metrician.exportMetrics(metrics.getMetrics(), metrics.getStart(), metrics.getEnd());
        final StreamingOutput metricsOutput = output -> {
            final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(output, Charset.defaultCharset()));
            for (final MetricianExportedValue metric : exportedMetrics) {
                final StringBuilder resultRow = new StringBuilder();
                resultRow.append(metric.getMetric());
                resultRow.append(CSV_SEPARATOR);
                resultRow.append(metric.getTimestamp());
                resultRow.append(CSV_SEPARATOR);
                resultRow.append(metric.getValue());
                bufferedWriter.write(resultRow.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        };

        return Response.ok(metricsOutput).build();
    }
}
