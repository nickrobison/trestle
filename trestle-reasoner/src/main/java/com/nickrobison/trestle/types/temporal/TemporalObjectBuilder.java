package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.types.TemporalScope;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;

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

    public static DatabaseTemporal.Builder database() {
        return new DatabaseTemporal.Builder();
    }

    public static Optional<TemporalObject> buildTemporalFromResults(TemporalScope scope, Optional<OWLLiteral> atAssertion, Optional<OWLLiteral> fromAssertion, Optional<OWLLiteral> toAssertion) {
        switch (scope) {
            case VALID:
                if (atAssertion.isPresent()) {
                    return Optional.of(TemporalObjectBuilder.valid().at(parseToTemporal(atAssertion.get(), OffsetDateTime.class)).build());
                }
                if (fromAssertion.isPresent()) {
                    final IntervalTemporal.Builder intervalTemporal = TemporalObjectBuilder.valid().from(parseToTemporal(fromAssertion.get(), OffsetDateTime.class));
                    if (toAssertion.isPresent()) {
                        return Optional.of(intervalTemporal.to(parseToTemporal(toAssertion.get(), OffsetDateTime.class)).build());
                    }
                    return Optional.of(intervalTemporal.build());
                }
                return Optional.empty();
            case EXISTS:
                if (atAssertion.isPresent()) {
                    return Optional.of(TemporalObjectBuilder.exists().at(parseToTemporal(atAssertion.get(), OffsetDateTime.class)).build());
                }
                if (fromAssertion.isPresent()) {
                    final IntervalTemporal.Builder intervalTemporal = TemporalObjectBuilder.exists().from(parseToTemporal(fromAssertion.get(), OffsetDateTime.class));
                    if (toAssertion.isPresent()) {
                        return Optional.of(intervalTemporal.to(parseToTemporal(toAssertion.get(), OffsetDateTime.class)).build());
                    }
                    return Optional.of(intervalTemporal.build());
                }
                return Optional.empty();
            case DATABASE:
                if (atAssertion.isPresent()) {
                    return Optional.of(TemporalObjectBuilder.database().at(parseToTemporal(atAssertion.get(), OffsetDateTime.class)).build());
                }
                if (fromAssertion.isPresent()) {
                    final IntervalTemporal.Builder intervalTemporal = TemporalObjectBuilder.database().from(parseToTemporal(fromAssertion.get(), OffsetDateTime.class));
                    if (toAssertion.isPresent()) {
                        return Optional.of(intervalTemporal.to(parseToTemporal(toAssertion.get(), OffsetDateTime.class)).build());
                    }
                    return Optional.of(intervalTemporal.build());
                }
                return Optional.empty();
        }
        return Optional.empty();
    }

    public static Optional<TemporalObject> buildTemporalFromProperties(Set<OWLDataPropertyAssertionAxiom> properties, @Nullable Class<? extends Temporal> temporalType, String temporalID) {
        return buildTemporalFromProperties(properties, temporalType, null, temporalID);
    }

    public static Optional<TemporalObject> buildTemporalFromProperties(Set<OWLDataPropertyAssertionAxiom> properties, @Nullable Class<? extends Temporal> temporalType, Class<?> clazz) {
        return buildTemporalFromProperties(properties, temporalType, clazz, null);
    }

    public static Optional<TemporalObject> buildTemporalFromProperties(Set<OWLDataPropertyAssertionAxiom> properties, @Nullable Class<? extends Temporal> temporalType, @Nullable Class<?> clazz, @Nullable String temporalID) {
        if (temporalType == null) {
            temporalType = LocalDateTime.class;
        }

//        Set the TemporalID
        final String temporalIdentifier;
        if (temporalID == null) {
            temporalIdentifier = UUID.randomUUID().toString();
        } else {
            temporalIdentifier = temporalID;
        }

        final boolean isDefault;

//        If the class is null, assume it's not a default temporal
        if (clazz == null) {
            isDefault = false;
        } else {
            isDefault = TemporalParser.IsDefault(clazz);
        }

//        Try for valid first
        final Optional<OWLDataPropertyAssertionAxiom> valid_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI))
                .findFirst();

        if (valid_from.isPresent()) {
//            final Temporal validFromTemporal = LocalDateTime.parse(valid_from.get().getObject().getLiteral(), DateTimeFormatter.ISO_DATE_TIME);
            final Temporal validFromTemporal = parseToTemporal(valid_from.get().getObject(), temporalType, TemporalParser.GetStartZoneID(clazz));
//            Try for valid_to
            final Optional<OWLDataPropertyAssertionAxiom> valid_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI))
                    .findFirst();
            if (valid_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .from(validFromTemporal)
                        .to(parseToTemporal(valid_to.get().getObject(), temporalType, TemporalParser.GetEndZoneID(clazz)))
                        .isDefault(isDefault)
                        .withID(temporalIdentifier)
                        .withFromTimeZone(TemporalParser.GetStartZoneID(clazz))
                        .withToTimeZone(TemporalParser.GetEndZoneID(clazz))
                        .build());
//                        .withRelations(valid_from.get().getSubject().asOWLNamedIndividual()));
            } else {
                return Optional.of(TemporalObjectBuilder.valid()
                        .from(validFromTemporal)
                        .withFromTimeZone(TemporalParser.GetStartZoneID(clazz))
                        .withID(temporalIdentifier)
                        .build());
//                        .withRelations(valid_from.get().getSubject().asOWLNamedIndividual()));
            }
        } else {
            final Optional<OWLDataPropertyAssertionAxiom> valid_at = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI))
                    .findFirst();
            if (valid_at.isPresent()) {
                return Optional.of(TemporalObjectBuilder.valid()
                        .at(parseToTemporal(valid_at.get().getObject(), temporalType, TemporalParser.GetAtZoneID(clazz)))
                        .withTimeZone(TemporalParser.GetAtZoneID(clazz))
                        .withID(temporalIdentifier)
                        .build());
//                        .withRelations(valid_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

//        Try for exists

        final Optional<OWLDataPropertyAssertionAxiom> exists_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsFromIRI))
                .findFirst();

        if (exists_from.isPresent()) {
            final Temporal existsFromTemporal = parseToTemporal(exists_from.get().getObject(), temporalType, TemporalParser.GetStartZoneID(clazz));
//            Try for exists_to
            final Optional<OWLDataPropertyAssertionAxiom> exists_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI))
                    .findFirst();
            if (exists_to.isPresent()) {
                return Optional.of(TemporalObjectBuilder.exists()
                        .from(existsFromTemporal)
                        .to(parseToTemporal(exists_to.get().getObject(), temporalType, TemporalParser.GetEndZoneID(clazz)))
                        .withFromTimeZone(TemporalParser.GetStartZoneID(clazz))
                        .withToTimeZone(TemporalParser.GetEndZoneID(clazz))
                        .withID(temporalIdentifier)
                        .isDefault(isDefault)
                        .build());
//                        .withRelations(exists_from.get().getSubject().asOWLNamedIndividual()));
            } else {
                return Optional.of(TemporalObjectBuilder.exists()
                        .from(existsFromTemporal)
                        .withFromTimeZone(TemporalParser.GetStartZoneID(clazz))
                        .withID(temporalIdentifier)
                        .build());
//                        .withRelations(exists_from.get().getSubject().asOWLNamedIndividual()));
            }
        } else {
            final Optional<OWLDataPropertyAssertionAxiom> exists_at = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsAtIRI))
                    .findFirst();
            if (exists_at.isPresent()) {
                return Optional.of(TemporalObjectBuilder.exists()
                        .at(parseToTemporal(exists_at.get().getObject(), temporalType, TemporalParser.GetAtZoneID(clazz)))
                        .withTimeZone(TemporalParser.GetAtZoneID(clazz))
                        .withID(temporalIdentifier)
                        .build());

//                        .withRelations(exists_at.get().getSubject().asOWLNamedIndividual()));
            }
        }

//        Try for Database
//        Database has to be an interval type, so we don't need to check for a point temporal
        final Optional<OWLDataPropertyAssertionAxiom> db_from = properties.stream()
                .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI))
                .findFirst();

        if (db_from.isPresent()) {
//            Try for DB to
            final Optional<OWLDataPropertyAssertionAxiom> db_to = properties.stream()
                    .filter(dp -> dp.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI))
                    .findFirst();
            if (db_to.isPresent()) {
                return Optional.of(
                        TemporalObjectBuilder.database()
                                .from(parseToTemporal(db_from.get().getObject(), temporalType, TemporalParser.GetAtZoneID(clazz)))
                                .withFromTimeZone(TemporalParser.GetAtZoneID(clazz))
                                .to(parseToTemporal(db_to.get().getObject(), temporalType, TemporalParser.GetAtZoneID(clazz)))
                                .withToTimeZone(TemporalParser.GetAtZoneID(clazz))
                                .build());
            } else {
                return Optional.of(
                        TemporalObjectBuilder.database()
                                .from(parseToTemporal(db_from.get().getObject(), temporalType, TemporalParser.GetAtZoneID(clazz)))
                                .withFromTimeZone(TemporalParser.GetAtZoneID(clazz))
                                .build());
            }
        }

        return Optional.empty();
    }
}
