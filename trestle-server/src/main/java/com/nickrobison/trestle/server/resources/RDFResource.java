package com.nickrobison.trestle.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.server.modules.ReasonerModule;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * Created by nrobison on 11/28/16.
 */
@Path("/{iri}")
@Produces(MediaType.TEXT_PLAIN)
public class RDFResource {

    private final TrestleReasoner reasoner;

    @Inject
    public RDFResource(ReasonerModule reasoner) {
        this.reasoner = reasoner.getReasoner();
    }

    @GET
    @Timed
    public String returnRDFIRI(@Context UriInfo uri, @PathParam("iri") String iri) {
        return String.format("Returning RDF IRI %s", iri);
    }
}
