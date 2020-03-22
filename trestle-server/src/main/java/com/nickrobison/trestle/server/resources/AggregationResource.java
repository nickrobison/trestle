package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.exporter.GeoJsonWriter;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.TrestleReasonerImpl;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationOperation;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationType;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.server.resources.requests.AggregationRequest;
import org.locationtech.jts.geom.Geometry;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Created by nickrobison on 3/24/18.
 */
@Path("/aggregate")
@AuthRequired({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
public class AggregationResource {

    private final TrestleReasoner reasoner;

    @Inject
    public AggregationResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @POST
    public Response aggregateDataset(@Valid AggregationRequest request) throws UnregisteredClassException {
        final AggregationEngine aggregationEngine = ((TrestleReasonerImpl) reasoner).getAggregationEngine();
        final Class<?> dataset = this.reasoner.getDatasetClass(request.getRestriction().getDataset());
        final AggregationEngine.AggregationRestriction ar = new AggregationEngine.AggregationRestriction();
        ar.setFact(request.getRestriction().getProperty());
        ar.setValue(request.getRestriction().getValue());
        final AggregationOperation ao = new AggregationOperation(request.getStrategy().getField(),
                AggregationType.valueOf(request.getStrategy().getOperation()),
                request.getStrategy().getValue());
        final Optional<Geometry> geometry = aggregationEngine.aggregateDataset(dataset, ar, ao);
        if (geometry.isPresent()) {
            final String GeoJSONString = new GeoJsonWriter().write(geometry.get());
            return Response.ok().entity(GeoJSONString).build();
        }
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Unable to aggregate dataset")
                .build();
    }
}
