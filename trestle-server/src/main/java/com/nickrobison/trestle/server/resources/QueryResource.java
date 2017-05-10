package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.server.annotations.AuthRequired;
import com.nickrobison.trestle.server.auth.Privilege;
import com.nickrobison.trestle.server.modules.ReasonerModule;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.Response.ok;

/**
 * Created by nrobison on 2/27/17.
 */
@Path("/query")
@AuthRequired({Privilege.USER})
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {

    private static final Logger logger = LoggerFactory.getLogger(QueryResource.class);
    private final TrestleReasoner reasoner;


    @Inject
    public QueryResource(ReasonerModule reasonerModule) {
        this.reasoner = reasonerModule.getReasoner();
    }

    @GET
    public Map<String, String> getPrefixes() {
        return reasoner.getReasonerPrefixes();
    }

    @POST
    public Response executeQuery(@NotEmpty String queryString) {
        logger.debug("Executing query {}", queryString);
        try {
            final TrestleResultSet results = this.reasoner.executeSPARQLSelect(queryString);

            return ok(results).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
