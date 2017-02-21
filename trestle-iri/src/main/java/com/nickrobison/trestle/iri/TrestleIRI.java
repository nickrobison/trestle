package com.nickrobison.trestle.iri;

import org.semanticweb.owlapi.model.HasIRI;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Created by nrobison on 2/7/17.
 */
public abstract class TrestleIRI implements HasIRI, Serializable {

    protected final IRIVersion version;
    protected final String prefix;
    protected final String objectID;

    TrestleIRI(IRIVersion version, String prefix, String objectID) {
     this.version = version;
     this.prefix = prefix;
     this.objectID = objectID;
    }

    public IRIVersion getVersion() {
        return version;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getObjectID() {
        return objectID;
    }

    public abstract Optional<String> getObjectFact();

    public abstract Optional<OffsetDateTime> getObjectTemporal();

    public abstract Optional<OffsetDateTime> getDbTemporal();

    @Override
    public String toString() {
        return getIRI().toString();
    }
}
