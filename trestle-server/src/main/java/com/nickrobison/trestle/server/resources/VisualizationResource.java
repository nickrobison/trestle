package com.nickrobison.trestle.server.resources;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import io.dropwizard.jersey.params.NonEmptyStringParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
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
    private static final DateTimeFormatter LocalDateTimeToJavascriptFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final TrestleReasoner reasoner;
    private final ObjectMapper mapper;

    @Inject
    public VisualizationResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
        mapper = new ObjectMapper();
        mapper.registerModule(new JtsModule());
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
    @Path("/retrieve")
    public Response getIndividual(@NotNull @QueryParam("name") String individualName) {
        final TrestleIndividual trestleIndividual = this.reasoner.getTrestleIndividual(individualName);

//        Build a simplified JSON implementation
        final ObjectNode individualNode = mapper.createObjectNode();
        final ArrayNode factArrayNode = mapper.createArrayNode();
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
                    validTemporal.put("validID", validTemporalObject.getID());
                    validTemporal.put("validFrom", LocalDateTimeToJavascriptFormatter.format(validTemporalObject.asInterval().getFromTime()));
                    if (validTemporalObject.asInterval().isContinuing()) {
                        validTemporal.put("validTo", "");
                    } else {
                        validTemporal.put("validTo", LocalDateTimeToJavascriptFormatter.format(((Temporal) validTemporalObject.asInterval().getToTime().get())));
                    }

                    final TemporalObject databaseTemporalObject = fact.getDatabaseTemporal();
                    databaseTemporal.put("validID", databaseTemporalObject.getID());
                    databaseTemporal.put("validFrom", LocalDateTimeToJavascriptFormatter.format(databaseTemporalObject.asInterval().getFromTime()));
                    if (databaseTemporalObject.asInterval().isContinuing()) {
                        databaseTemporal.put("validTo", "");
                    } else {
                        databaseTemporal.put("validTo", LocalDateTimeToJavascriptFormatter.format(((Temporal) databaseTemporalObject.asInterval().getToTime().get())));
                    }

                    factNode.set("validTemporal", validTemporal);
                    factNode.set("databaseTemporal", databaseTemporal);
                    factArrayNode.add(factNode);
                });

        individualNode.set("facts", factArrayNode);
//        Now the individual temporal
        final ObjectNode individualTemporal = mapper.createObjectNode();
        final TemporalObject individualTemporalObject = trestleIndividual.getExistsTemporal();
        individualTemporal.put("validID", individualTemporalObject.getID());
        individualTemporal.put("validFrom", LocalDateTimeToJavascriptFormatter.format(individualTemporalObject.asInterval().getFromTime()));
        if (individualTemporalObject.asInterval().isContinuing()) {
            individualTemporal.put("validTo", "");
        } else {
            individualTemporal.put("validTo", LocalDateTimeToJavascriptFormatter.format(((Temporal) individualTemporalObject.asInterval().getToTime().get())));
        }
        individualNode.set("individualTemporal", individualTemporal);
        return ok(individualNode).build();
    }

}
