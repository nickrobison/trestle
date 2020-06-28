package com.nickrobison.trestle.server.resources;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.engines.AbstractComparisonReport;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialComparisonReport;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.server.annotations.PrivilegesAllowed;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ManagedReasoner;
import com.nickrobison.trestle.server.resources.requests.ComparisonRequest;
import com.nickrobison.trestle.server.resources.requests.DatasetValueRequest;
import com.nickrobison.trestle.server.resources.requests.IntersectRequest;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import io.dropwizard.jersey.params.NonEmptyStringParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nrobison on 3/7/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Path("/visualize")
@PrivilegesAllowed({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "visualize")
public class VisualizationResource {

    private static final Logger logger = LoggerFactory.getLogger(VisualizationResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    //    This should be a config parameter
    public static final double MATCH_THRESHOLD = 0.95;
    public static final String VALID_FROM = "validFrom";
    public static final String VALID_TO = "validTo";
    public static final String VALID_ID = "validID";
    private final DateTimeFormatter localDateTimeToJavascriptFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final TrestleReasoner reasoner;

    @Inject
    public VisualizationResource(ManagedReasoner managedReasoner) {
        this.reasoner = managedReasoner.getReasoner();
        mapper.registerModule(new JtsModule());
    }

    @GET
    @Path("/search")
    @ApiOperation(value = "Search for individual matching query string",
            notes = "Performs a search against the database for any individuals with an id matching the query string",
            response = String.class,
            responseContainer = "List")
    public Response searchForIndividual(@NotNull @QueryParam("name") String name, @QueryParam("dataset") NonEmptyStringParam dataset, @QueryParam("limit") Optional<Integer> limit) {
        if (!name.equals("")) {
            final List<String> individuals = this.reasoner.searchForIndividual(name, dataset.get().orElse(null), limit.orElse(null));
            return ok(individuals).build();
        }
        return ok(new ArrayList<String>()).build();
    }

    @POST
    @Path("/values")
    public Response getDatasetValues(@Valid DatasetValueRequest request) {
        final Class<?> datasetClass;
        try {
            datasetClass = this.reasoner.getDatasetClass(request.getDataset());
            final List<Object> factValues = this.reasoner.sampleFactValues(datasetClass, request.getFact(), request.getLimit());
            return ok(factValues).build();
        } catch (UnregisteredClassException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Fact does not exist on dataset").build();
        }
    }

    @POST
    @Path("/intersect")
    @ApiOperation(value = "Retrieve all objects, from a specified dataset, valid at the specified time point, that intersects the provided GeoJSON object",
            notes = "Performs a spatial-temporal intersection for all matching objects for the given dataset. " +
                    "Allows the user to specify both valid temporal and database temporal intersection points. " +
                    "This method returns a specific object state, not the entirety of the object properties",
            response = Object.class,
            responseContainer = "List")
    @ApiResponses({
            @ApiResponse(code = 440, message = "Object class is not registered with the database"),
            @ApiResponse(code = 500, message = "Problem while performing spatial intersection")
    })
    public Response intersect(@NotNull IntersectRequest request) {

        final Class<?> datasetClass;
        try {
            datasetClass = this.getClassFromRequest(request);
        } catch (UnregisteredClassException e) {
            logger.error("Unable to find class", e);
            return Response.status(Response.Status.BAD_REQUEST).entity("Class does not exist").build();
        }

        final Geometry read;
        try {
            read = this.getGeometryFromRequest(request);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        final Optional<? extends List<?>> intersectedObjects = this.reasoner.spatialIntersect(datasetClass,
                read.buffer(request.getBuffer()).toString(),
                request.getBuffer(),
                request.getValidAt(), null);
        return intersectedObjects.map(list -> Response.ok(list).build()).orElseGet(() -> Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Empty response from server, something went wrong")
                .build());
    }

    @POST
    @Path("/compare")
    @ApiOperation(value = "Performs a spatial comparison between a set of individuals",
            notes = "Compares the specified individual against the given set of individuals",
            response = ComparisonReport.class)
    @ApiResponses({
            @ApiResponse(code = 500, message = "Error while performing comparison")
    })
    public Response compareIndividuals(@NotNull ComparisonRequest request) {

        final ComparisonReport comparisonReport = new ComparisonReport();
        try {
//                Do a piecewise comparison for each individual
            logger.debug("Beginning piecewise comparison");
            final Instant compareStart = Instant.now();
            final Optional<List<SpatialComparisonReport>> spatialComparisonReports = this.reasoner.compareTrestleObjects("GAUL", request.getCompare(), request.getCompareAgainst(), 4326, MATCH_THRESHOLD);
            logger.debug("Comparison took {} ms", Duration.between(compareStart, Instant.now()).toMillis());
            spatialComparisonReports.ifPresent(comparisonReport::addAllReports);

//            Filter out objects that don't overlap with the base individual, and do the union calculation
            final List<String> compareIndividuals = comparisonReport.getReports()
                    .stream()
                    .filter(report -> report.getRelations().contains(ObjectRelation.SPATIAL_OVERLAPS))
                    .map(AbstractComparisonReport::getObjectBID)
                    .collect(Collectors.toList());

            compareIndividuals.add(request.getCompare());


            //        Look for spatial union
            logger.debug("Executing union");
            final Instant unionStart = Instant.now();
            final Optional<UnionContributionResult> objectUnionEqualityResult = this.reasoner.calculateSpatialUnionWithContribution("GAUL", compareIndividuals, 4326, MATCH_THRESHOLD);
            logger.debug("Union computation took {} ms", Duration.between(unionStart, Instant.now()).toMillis());
            objectUnionEqualityResult.ifPresent(comparisonReport::setUnion);


            return Response.ok(comparisonReport).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }


    private Class<?> getClassFromRequest(IntersectRequest request) throws UnregisteredClassException {
        return this.reasoner.getDatasetClass(request.getDataset());
    }

    private Geometry getGeometryFromRequest(IntersectRequest request) throws JsonProcessingException {
        final GeoJSONReader reader = new GeoJSONReader();
        return reader.read(mapper.writeValueAsString(request.getGeojson()));
//        return SpatialEngineUtils.reprojectGeometry(geometry, 3857, 4326);
    }

}
