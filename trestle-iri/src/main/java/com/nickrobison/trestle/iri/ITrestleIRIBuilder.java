package com.nickrobison.trestle.iri;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;

/**
 * Created by nrobison on 1/23/17.
 */
public interface ITrestleIRIBuilder {
    /**
     * Builds a TrestleIRI from a given set of object parameters
     * If a Fact is specified, then the IRI points not to a TrestleObject, but to a fact of that object
     * If an object temporal is specified, but no Fact is given, the temporal refers to the state of the object at the given time, otherwise, it refers to the valid point of the Fact
     * @param prefix - IRI prefix to use
     * @param objectID - Identifier of object
     * @param objectFact - Optional Fact of TrestleObject to refer to
     * @param objectTemporal - Optional temporal to specify the object state
     * @param databaseTemporal Optional temporal to specify the database state
     * @return - Fully encoded IRI {@link IRI} using TrestleIRI scheme
     */
    IRI encodeIRI(String prefix, String objectID, @Nullable String objectFact, @Nullable OffsetDateTime objectTemporal, @Nullable OffsetDateTime databaseTemporal);
}
