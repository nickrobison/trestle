package com.nickrobison.trestle.iri;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;

/**
 * Created by nrobison on 1/23/17.
 */
public class V1IRIBuilder implements ITrestleIRIBuilder {

    private static final String VERSION_HEADER = "V1";

    V1IRIBuilder() { }

    @Override
    public IRI encodeIRI(String prefix, String objectID, @Nullable String objectFact, @Nullable OffsetDateTime objectTemporal, @Nullable OffsetDateTime databaseTemporal) {
        StringBuilder trestleIRI = new StringBuilder();
        trestleIRI.append(String.format("%s:%s", VERSION_HEADER, objectID));
        if (objectFact != null) {
            trestleIRI.append(String.format("@%s", objectFact));
        }
        if (objectTemporal != null) {
            trestleIRI.append(String.format(":%s", objectTemporal.toEpochSecond()));
        }
        if (databaseTemporal != null) {
            trestleIRI.append(String.format(":%s", databaseTemporal.toEpochSecond()));
        }
        return IRI.create(prefix, trestleIRI.toString());
    }
}
