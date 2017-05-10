package com.nickrobison.trestle.reasoner.types;

/**
 * Created by nrobison on 2/20/17.
 */

import org.semanticweb.owlapi.model.IRI;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Return type of the ReadTrestleObject implementation, provides both the object as well as the minimum valid/database state it's valid for
 */
public class TrestleObjectResult<T> {
    private final IRI individual;
    private final T object;
    private final OffsetDateTime validFrom;
    private final OffsetDateTime validTo;
    private final OffsetDateTime dbFrom;
    private final OffsetDateTime dbTo;

    public TrestleObjectResult(IRI individual, T object, Temporal validFrom, Temporal validTo, Temporal dbFrom, Temporal dbTo) {
        this.individual = individual;
        this.object = object;
        this.validFrom = parseTemporalToOntologyDateTime(validFrom, ZoneOffset.UTC);
        this.validTo = parseTemporalToOntologyDateTime(validTo, ZoneOffset.UTC);
        this.dbFrom = parseTemporalToOntologyDateTime(dbFrom, ZoneOffset.UTC);
        this.dbTo = parseTemporalToOntologyDateTime(dbTo, ZoneOffset.UTC);
    }

    public IRI getIndividual() {
        return this.individual;
    }

    public T getObject() {
        return object;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public OffsetDateTime getDbFrom() {
        return dbFrom;
    }

    public OffsetDateTime getDbTo() {
        return dbTo;
    }
}
