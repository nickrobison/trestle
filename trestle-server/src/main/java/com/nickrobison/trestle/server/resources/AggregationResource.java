package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.TrestleReasonerImpl;
import com.nickrobison.trestle.reasoner.engines.spatial.AggregationEngine;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.server.resources.requests.AggregationRequest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
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
        final Class<?> dataset = this.reasoner.getDatasetClass(request.getDataset());
        final Optional<? extends List<?>> objects = aggregationEngine.aggregateDataset(dataset, request.getWkt());
        return Response.ok().build();
    }
}
