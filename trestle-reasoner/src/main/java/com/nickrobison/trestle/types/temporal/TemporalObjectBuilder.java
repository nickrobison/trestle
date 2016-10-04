package com.nickrobison.trestle.types.temporal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.parser.TemporalParser.parseToTemporal;

/**
 * Created by nrobison on 6/30/16.
 */
// I can ignore this because I implement my own type check class with the isPresent() methods
@SuppressWarnings("unchecked")
public class TemporalObjectBuilder {


    private TemporalObjectBuilder() {
    }

    public static ValidTemporal.Builder valid() {
        return new ValidTemporal.Builder();
    }

    public static ExistsTemporal.Builder exists() {
        return new ExistsTemporal.Builder();
    }

    public static Optional<TemporalObject> buildTemporalFromProperties(Set<OWLDataPropertyAssertionAxiom> properties, boolean isDefault, @Nullable  Class<? extends Temporal> temporalType) {
        if (temporalType == null) {
            temporalType = LocalDateTime.class;
        }

//        Try for valid first
        final Optional<OWLDataPropertyAssertionAxiom> valid_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI))
                .findFirst();

        if (valid_from.isPresent()) {
//            final Temporal validFromTemporal = LocalDateTime.parse(valid_from.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME);
            final Temporal validFromTemporal = parseToTemporal(valid_from.get().getObject(), temporalType);
//            Try for valid_to
            final Optional<OWLDataPropertyAssertionAxiom> valid_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI))
                    .findFirst();
            if (valid_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .from(validFromTemporal)
                        .to(parseToTemporal(valid_to.get().getObject(), temporalType))
                        .isDefault(isDefault)
                        .withRelations(valid_from.get().getSubject().asOWLNamedIndividual()));
            } else {
                return Optional.of(TemporalObjectBuilder.valid()
                        .from(validFromTemporal)
                        .withRelations(valid_from.get().getSubject().asOWLNamedIndividual()));
            }
        } else {
            final Optional<OWLDataPropertyAssertionAxiom> valid_at = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI))
                    .findFirst();
            if (valid_at.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .at(parseToTemporal(valid_at.get().getObject(), temporalType))
                        .withRelations(valid_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

//        Try for exists

        final Optional<OWLDataPropertyAssertionAxiom> exists_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsFromIRI))
                .findFirst();

        if (exists_from.isPresent()) {
            final Temporal existsFromTemporal = parseToTemporal(exists_from.get().getObject(), temporalType);
//            Try for exists_to
            final Optional<OWLDataPropertyAssertionAxiom> exists_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI))
                    .findFirst();
            if (exists_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.exists()
                        .from(existsFromTemporal)
                        .to(parseToTemporal(exists_to.get().getObject(), temporalType))
                        .isDefault(isDefault)
                        .withRelations(exists_from.get().getSubject().asOWLNamedIndividual()));
            } else {
                return Optional.of(TemporalObjectBuilder.exists()
                        .from(existsFromTemporal)
                        .withRelations(exists_from.get().getSubject().asOWLNamedIndividual()));
            }
        } else {
            final Optional<OWLDataPropertyAssertionAxiom> exists_at = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsAtIRI))
                    .findFirst();
            if (exists_at.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .at(parseToTemporal(exists_at.get().getObject(), temporalType))
                        .withRelations(exists_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

        return Optional.empty();
    }
}
