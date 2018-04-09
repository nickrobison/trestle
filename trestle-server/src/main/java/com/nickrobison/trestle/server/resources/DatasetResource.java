package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nickrobison on 4/9/18.
 */
@Path("/datasets")
@AuthRequired({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {

    private final TrestleReasoner reasoner;

    @Inject
    public DatasetResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @GET
    public Response getDatasets() {
      return ok(this.reasoner.getAvailableDatasets()).build();
    }

    @GET
    @Path("/{dataset}/{property}/values")
    public Response getDatasetValues(@PathParam("dataset") String dataset,
                                     @PathParam("property") String property,
                                     @DefaultValue("100") @QueryParam("limit") Integer limit) {

        try {
            final Class<?> datasetClass = this.reasoner.getDatasetClass(dataset);
            final List<Object> factValues = this.reasoner.sampleFactValues(datasetClass, property, limit);
            return ok(factValues).build();
        } catch (UnregisteredClassException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Fact does not exist on dataset").build();
        }
    }
}
