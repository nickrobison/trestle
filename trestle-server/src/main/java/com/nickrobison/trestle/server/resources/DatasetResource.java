package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.PrivilegesAllowed;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ManagedReasoner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
@PrivilegesAllowed({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "datasets")
public class DatasetResource {

    private final TrestleReasoner reasoner;

    @Inject
    public DatasetResource(ManagedReasoner managedReasoner) {
        this.reasoner = managedReasoner.getReasoner();
    }

    @GET
    @ApiOperation(value = "Get all datasets currently registered with the database",
            notes = "Returns a Set of all the datasets currently registered with the database",
            response = String.class,
            responseContainer = "Set")
    public Response getDatasets() {
        return ok(this.reasoner.getAvailableDatasets()).build();
    }

    @GET
    @Path("/{dataset}")
    @ApiOperation(value = "Get all data properties for the given dataset",
            notes = "Returns a list of all data properties associated with the given datataset. " +
                    "List is sorted alphabetically.",
            response = String.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Object class is not registered with the database"),
            @ApiResponse(code = 500, message = "Cannot get properties for the given dataset")
    })
    public Response getDatasetProperties(@PathParam("dataset") String dataset) {
        try {
            final Class<?> datasetClass = this.reasoner.getDatasetClass(dataset);
            return ok(this.reasoner.getDatasetProperties(datasetClass)).build();
        } catch (UnregisteredClassException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Cannot get properties for dataset").build();
        }
    }

    @GET
    @Path("/{dataset}/{property}/values")
    @ApiOperation(value = "Get a sampling of unique values for the given dataset property",
            notes = "Returns a subset of unique values for the given dataset property. " +
                    "Allows the user to specify a limit to the values returned. If no limit is specified, defaults to 100 values.",
            response = Object.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Object class is not registered with the database"),
            @ApiResponse(code = 404, message = "Fact does not exist on dataset")
    })
    public Response getDatasetValues(@PathParam("dataset") String dataset,
                                     @PathParam("property") String property,
                                     @DefaultValue("100") @QueryParam("limit") Integer limit) {

        try {
            final Class<?> datasetClass = this.reasoner.getDatasetClass(dataset);
            final List<Object> factValues = this.reasoner.sampleFactValues(datasetClass, property, limit).toList().blockingGet();
            return ok(factValues).build();
        } catch (UnregisteredClassException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Fact does not exist on dataset").build();
        }
    }
}
