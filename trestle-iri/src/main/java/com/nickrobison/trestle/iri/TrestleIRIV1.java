package com.nickrobison.trestle.iri;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Created by nrobison on 2/7/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TrestleIRIV1 extends TrestleIRI {

    private final Optional<String> objectFact;
    private final Optional<OffsetDateTime> objectTemporal;
    private final Optional<OffsetDateTime> dbTemporal;

    public TrestleIRIV1(IRIVersion version, String prefix, String objectID, @Nullable String objectFact, @Nullable OffsetDateTime objectTemporal, @Nullable OffsetDateTime databaseTemporal) {
        super(version, prefix, objectID);
        if (objectFact == null || objectFact.equals("")) {
            this.objectFact = Optional.empty();
        } else {
            this.objectFact = Optional.of(objectFact);
        }
        this.objectTemporal = Optional.ofNullable(objectTemporal);
        this.dbTemporal = Optional.ofNullable(databaseTemporal);

    }

    @Override
    public Optional<String> getObjectFact() {
        return objectFact;
    }

    @Override
    public Optional<OffsetDateTime> getObjectTemporal() {
        return objectTemporal;
    }

    @Override
    public Optional<OffsetDateTime> getDbTemporal() {
        return dbTemporal;
    }

    @Override
    public IRI getIRI() {
        final StringBuilder trestleIRI = new StringBuilder();
        trestleIRI.append(String.format("%s:%s", getVersion(), getObjectID()));
        getObjectFact().ifPresent(fact -> trestleIRI.append(String.format("@%s", fact)));
        getObjectTemporal().ifPresent(temporal -> trestleIRI.append(String.format(":%s", parseToEpochMilli(temporal))));
        getDbTemporal().ifPresent(temporal -> trestleIRI.append(String.format(":%s", parseToEpochMilli(temporal))));
        return IRI.create(this.prefix, trestleIRI.toString());
    }

    private static long parseToEpochMilli(OffsetDateTime temporal) {
        return temporal.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

}
