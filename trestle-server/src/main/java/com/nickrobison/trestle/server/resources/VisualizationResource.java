package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.types.TrestleIndividual;
import io.dropwizard.jersey.params.NonEmptyStringParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nrobison on 3/7/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/visualize")
@AuthRequired({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
public class VisualizationResource {

    private static final Logger logger = LoggerFactory.getLogger(VisualizationResource.class);
    private final TrestleReasoner reasoner;

    @Inject
    public VisualizationResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @GET
    @Path("/search")
    public Response searchForIndividual(@NotNull @QueryParam("name") String name, @QueryParam("dataset") NonEmptyStringParam dataset, @QueryParam("limit") Optional<Integer> limit) {
        if (!name.equals("")) {
            final List<String> individuals = this.reasoner.searchForIndividual(name, dataset.get().orElse(null), limit.orElse(null));
            return ok(individuals).build();
        }
        return ok(new ArrayList<String>()).build();
    }


    @GET
    @Path("{name}")
    public Response getIndividual(@PathParam("name") String individualName) {
        final TrestleIndividual trestleIndividual = this.reasoner.getTrestleIndividual(individualName);
        return ok(trestleIndividual).build();
    }

}
