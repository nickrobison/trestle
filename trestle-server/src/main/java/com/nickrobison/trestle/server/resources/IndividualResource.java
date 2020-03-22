package com.nickrobison.trestle.server.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.server.resources.requests.IntersectRequest;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.locationtech.jts.geom.Geometry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nickrobison on 1/9/18.
 */
@Path("/individual")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "visualize")
public class IndividualResource {

    private static final Logger logger = LoggerFactory.getLogger(IndividualResource.class);
    public static final String TEMPORAL_FROM = "From";
    public static final String TEMPORAL_TO = "To";
    public static final String TEMPORAL_ID = "ID";
    private static final GeoJSONReader reader = new GeoJSONReader();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final DateTimeFormatter localDateTimeToJavascriptFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final TrestleReasoner reasoner;

    @Inject
    public IndividualResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @GET
    @Path("/retrieve")
    @ApiOperation(value = "Retrieve specified individual",
            notes = "Retrieves all properties for the specified individual, at all time points",
            response = TrestleIndividual.class)
    @ApiResponses({
            @ApiResponse(code = 404, message = "Cannot find individual with the specified ID")
    })
    public Response getIndividual(@NotNull @QueryParam("name") String individualName) {
        try {
            final TrestleIndividual trestleIndividual = this.reasoner.getTrestleIndividual(individualName);
            return ok(this.buildIndividualFromJSON(trestleIndividual)).build();
        } catch (TrestleMissingIndividualException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(String.format("Cannot find individual %s", individualName)).build();
        }
    }


    @POST
    @Path("/intersect-individuals")
    @ApiOperation(value = "Retrieve all individuals, from a specified dataset, valid at the specified time point, that intersects the provided GeoJSON object",
            notes = "Performs a spatial-temporal intersection for all matching objects for the given dataset. " +
                    "Allows the user to specify both valid temporal and database temporal intersection points. " +
                    "This method returns a TrestleIndividual, which represents the entirety of all individual properties",
            response = TrestleIndividual.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 440, message = "Object class is not registered with the database"),
            @ApiResponse(code = 500, message = "Problem while performing spatial intersection")
    })
    public Response intersectIndividuals(@NotNull IntersectRequest request) {
        final Class<?> datasetClass;
        try {
            datasetClass = this.getClassFromRequest(request);
        } catch (UnregisteredClassException e) {
            logger.error("Unable to find class", e);
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        }

        final Geometry geom;
        try {
            geom = this.getGeometryFromRequest(request);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        final Optional<List<TrestleIndividual>> trestleIndividuals = this.reasoner.spatialIntersectIndividuals(datasetClass,
                geom.toString(),
                request.getBuffer(),
                request.getValidAt(),
                request.getDatabaseAt());
        if (trestleIndividuals.isPresent()) {
            final List<ObjectNode> builtIndividuals = trestleIndividuals.get()
                    .stream()
                    .map(this::buildIndividualFromJSON)
                    .collect(Collectors.toList());
            return Response.ok(builtIndividuals).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Unable get intersected individuals")
                .build();
    }

    private Class<?> getClassFromRequest(IntersectRequest request) throws UnregisteredClassException {
        return this.reasoner.getDatasetClass(request.getDataset());
    }

    private Geometry getGeometryFromRequest(IntersectRequest request) throws JsonProcessingException {
        return reader.read(mapper.writeValueAsString(request.getGeojson()));
    }

    @SuppressWarnings("Duplicates")
//    This is copied from the IndividualResource, but whatever, this will go away eventually
    private ObjectNode buildIndividualFromJSON(TrestleIndividual trestleIndividual) {
        //        Build a simplified JSON implementation
        final ObjectNode individualNode = mapper.createObjectNode();
        final ArrayNode factArrayNode = mapper.createArrayNode();
        final ArrayNode relationArrayNode = mapper.createArrayNode();
        final ArrayNode eventArrayNode = mapper.createArrayNode();
        individualNode.put("individualID", trestleIndividual.getIndividualID());
        trestleIndividual.getFacts()
                .forEach(fact -> {
                    final ObjectNode factNode = mapper.createObjectNode();
                    final ObjectNode validTemporal = mapper.createObjectNode();
                    final ObjectNode databaseTemporal = mapper.createObjectNode();
                    factNode.put("identifier", fact.getIdentifier());
                    factNode.put("name", fact.getName());
                    factNode.put("value", fact.getValue().toString());
                    factNode.put("type", fact.getValue().getClass().getName());

//                                Now the temporals
//                                FIXME(nrobison): This is disgusting. Fix it.
                    final TemporalObject validTemporalObject = fact.getValidTemporal();
                    validTemporal.put(TEMPORAL_ID, validTemporalObject.getID());
                    validTemporal.put(TEMPORAL_FROM, localDateTimeToJavascriptFormatter.format(validTemporalObject.asInterval().getFromTime()));
                    if (validTemporalObject.asInterval().isContinuing()) {
                        validTemporal.put(TEMPORAL_TO, "");
                    } else {
                        validTemporal.put(TEMPORAL_TO, localDateTimeToJavascriptFormatter.format(((Temporal) validTemporalObject.asInterval().getToTime().get())));
                    }

                    final TemporalObject databaseTemporalObject = fact.getDatabaseTemporal();
                    databaseTemporal.put(TEMPORAL_ID, databaseTemporalObject.getID());
                    databaseTemporal.put(TEMPORAL_FROM, localDateTimeToJavascriptFormatter.format(databaseTemporalObject.asInterval().getFromTime()));
                    if (databaseTemporalObject.asInterval().isContinuing()) {
                        databaseTemporal.put(TEMPORAL_TO, "");
                    } else {
                        databaseTemporal.put(TEMPORAL_TO, localDateTimeToJavascriptFormatter.format(((Temporal) databaseTemporalObject.asInterval().getToTime().get())));
                    }

                    factNode.set("validTemporal", validTemporal);
                    factNode.set("databaseTemporal", databaseTemporal);
                    factArrayNode.add(factNode);
                });

        individualNode.set("facts", factArrayNode);
//        Relationships
        trestleIndividual.getRelations().forEach(relation -> {
            ObjectNode relationNode = mapper.createObjectNode();
            relationNode.put("subject", relation.getSubject());
            relationNode.put("object", relation.getObject());
            relationNode.put("relation", relation.getType());
            relationArrayNode.add(relationNode);
        });
        individualNode.set("relations", relationArrayNode);

//        Events
        trestleIndividual.getEvents().forEach(event -> {
            final ObjectNode eventNode = mapper.createObjectNode();
            eventNode.put("individual", event.getIndividual().getIRI().toString());
            eventNode.put("type", event.getType().toString());
            eventNode.put("temporal", event.getAtTemporal().toString());
            eventArrayNode.add(eventNode);
        });
        individualNode.set("events", eventArrayNode);
//        Now the individual temporal
        final ObjectNode existsTemporal = mapper.createObjectNode();
        final TemporalObject individualTemporalObject = trestleIndividual.getExistsTemporal();
        existsTemporal.put(TEMPORAL_ID, individualTemporalObject.getID());
        existsTemporal.put(TEMPORAL_FROM, localDateTimeToJavascriptFormatter.format(individualTemporalObject.asInterval().getFromTime()));
        if (individualTemporalObject.asInterval().isContinuing()) {
            existsTemporal.put(TEMPORAL_TO, "");
        } else {
            existsTemporal.put(TEMPORAL_TO, localDateTimeToJavascriptFormatter.format(((Temporal) individualTemporalObject.asInterval().getToTime().get())));
        }
        individualNode.set("existsTemporal", existsTemporal);

        return individualNode;
    }

}
