package com.nickrobison.trestle.types.temporal;

import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.*;

/**
 * Created by nrobison on 6/30/16.
 */
public class TemporalObjectBuilder {


    private TemporalObjectBuilder() {
    }

    public static ValidTemporal.Builder valid() {
        return new ValidTemporal.Builder();
    }

    public static ExistsTemporal.Builder exists() {
        return new ExistsTemporal.Builder();
    }

    public static Optional<TemporalObject> buildTemporalFromProperties(Set<OWLDataPropertyAssertionAxiom> properties, boolean isDefault) {

//        Try for valid first
        final Optional<OWLDataPropertyAssertionAxiom> valid_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI))
                .findFirst();

        if (valid_from.isPresent()) {
            final LocalDateTime validFromTemporal = LocalDateTime.parse(valid_from.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME);
//            Try for valid_to
            final Optional<OWLDataPropertyAssertionAxiom> valid_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI))
                    .findFirst();
            if (valid_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .from(validFromTemporal)
                        .to(LocalDateTime.parse(valid_to.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME))
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
                        .at(LocalDateTime.parse(valid_at.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME))
                        .withRelations(valid_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

//        Try for exists

        final Optional<OWLDataPropertyAssertionAxiom> exists_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsFromIRI))
                .findFirst();

        if (exists_from.isPresent()) {
            final LocalDateTime existsFromTemporal = LocalDateTime.parse(exists_from.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME);
//            Try for exists_to
            final Optional<OWLDataPropertyAssertionAxiom> exists_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI))
                    .findFirst();
            if (exists_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.exists()
                        .from(existsFromTemporal)
                        .to(LocalDateTime.parse(exists_to.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME))
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
                        .at(LocalDateTime.parse(exists_at.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME))
                        .withRelations(exists_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

        return Optional.empty();
    }
}
