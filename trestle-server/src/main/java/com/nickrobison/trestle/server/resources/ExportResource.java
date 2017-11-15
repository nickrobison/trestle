package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.server.resources.requests.ExportRequest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static javax.ws.rs.core.Response.ok;

@Path("/export")
@AuthRequired({Privilege.USER})
public class ExportResource {

    private final TrestleReasoner reasoner;


    @Inject
    public ExportResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @POST
    public Response exportData(@Valid ExportRequest request) {
        final ITrestleExporter.DataType dataType = ITrestleExporter.DataType.valueOf(request.getType());

        final String responseType;
        switch (dataType) {
            case GEOJSON: {
                responseType = MediaType.APPLICATION_JSON;
                break;
            }
            case KML: {
                responseType = MediaType.APPLICATION_XML;
                break;
            }
            default: {
                responseType = MediaType.APPLICATION_OCTET_STREAM;
            }

        }
        final Class<?> datasetClass;
        try {
            datasetClass = this.reasoner.getDatasetClass(request.getDataset());
        } catch (UnregisteredClassException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        try {
            final File dataSetObjects = this.reasoner.exportDataSetObjects(datasetClass, request.getIndividuals(), dataType);
            return ok(dataSetObjects).type(responseType).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
