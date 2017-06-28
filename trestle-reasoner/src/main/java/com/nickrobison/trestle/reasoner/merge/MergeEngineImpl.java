package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.reasoner.types.TemporalScope;
import com.nickrobison.trestle.reasoner.types.TrestleOWLFact;
import com.nickrobison.trestle.reasoner.types.temporal.TemporalObject;
import com.nickrobison.trestle.reasoner.types.temporal.TemporalObjectBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 6/13/17.
 */
public class MergeEngineImpl implements TrestleMergeEngine {

    private static final Logger logger = LoggerFactory.getLogger(MergeEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final String PROPERTY = "property";
    public static final String INDIVIDUAL = "individual";
    public static final String OBJECT = "object";
    private final Config config;
    private MergeStrategy defaultStrategy;
    private final boolean onLoad;

    public MergeEngineImpl() {
        config = ConfigFactory.load().getConfig("trestle.merge");
        defaultStrategy = MergeStrategy.valueOf(config.getString("defaultStrategy"));
        this.onLoad = config.getBoolean("onLoad");
    }

    @Override
    public void changeDefaultMergeStrategy(MergeStrategy strategy) {
        logger.info("Changing default merge strategy from {} to {}", this.defaultStrategy, strategy);
        this.defaultStrategy = strategy;
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal) {
        return mergeFacts(newFacts, existingFacts, eventTemporal, databaseTemporal, MergeStrategy.Default);
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, MergeStrategy strategy) {
        if (strategy.equals(MergeStrategy.Default)) {
            strategy = defaultStrategy;
        }
        logger.debug("Merging facts using the {} strategy", strategy);
        switch (strategy) {
            case ContinuingFacts:
                return mergeLogic(newFacts, existingFacts, eventTemporal, databaseTemporal, true);
            case ExistingFacts:
                return mergeLogic(newFacts, existingFacts, eventTemporal, databaseTemporal, false);
            case NoMerge:
                return noMerge(newFacts, existingFacts);
            default:
//                    Do default stuff
                throw new TrestleMergeException("Can't execute default merge, should use strategy from config");
        }
    }

    @Override
    public boolean mergeEnabled() {
        return true;
    }

    @Override
    public boolean mergeOnLoad() {
        return this.onLoad;
    }

    private static MergeScript mergeLogic(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> currentFacts, Temporal eventTemporal, Temporal databaseTemporal, boolean continuingOnly) {
//        Determine if any of the currently valid facts diverge (have different values) from the newFacts we're attempting to write
        final List<OWLDataPropertyAssertionAxiom> divergingFacts = newFacts
                .stream()
                .filter(fact -> currentFacts
                        .stream()
                        .noneMatch(result -> {
                            final OWLDataPropertyAssertionAxiom resultFact = df.getOWLDataPropertyAssertionAxiom(
                                    df.getOWLDataProperty(result.getIndividual(PROPERTY).orElseThrow(() -> new RuntimeException("Property is null")).asOWLNamedIndividual().getIRI()),
                                    result.getIndividual(INDIVIDUAL).orElseThrow(() -> new RuntimeException("Individual is null")),
                                    result.getLiteral(OBJECT).orElseThrow(() -> new RuntimeException("Object is null")));
                            return resultFact.equals(fact);
                        }))
                .collect(Collectors.toList());

//                Do it the other way to find existing facts that will need to get a version increment
        final List<TrestleOWLFact> factsToVersion = currentFacts
                .stream()
                .map(result -> {
//                            Valid temporal
                    final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.VALID, result.getLiteral("va"), result.getLiteral("vf"), result.getLiteral("vt"));
//                            Parse the literal
                    final OWLDataPropertyAssertionAxiom factProperty = df.getOWLDataPropertyAssertionAxiom(
                            df.getOWLDataProperty(result.getIndividual(PROPERTY).orElseThrow(() -> new RuntimeException("Property is null")).asOWLNamedIndividual().getIRI()),
                            result.getIndividual(INDIVIDUAL).orElseThrow(() -> new RuntimeException("Individual is null")),
                            result.getLiteral(OBJECT).orElseThrow(() -> new RuntimeException("Object is null")));

//                    Override the valid temporal with the new ending date
                    final TemporalObject validTemporal1 = validTemporal.orElseThrow(() -> new RuntimeException("Fact valid temporal is null"));
                    if (continuingOnly && !validTemporal1.isContinuing()) {
                        throw new TrestleMergeConflict("Not all interval facts are continuing");
                    }
                    TemporalObject overriddenTemporal = overrideTemporal(validTemporal1, eventTemporal);
//                    And the database temporal as well
                    final TemporalObject newDbTemporal = TemporalObjectBuilder.database().from(databaseTemporal).build();
//                            Build the Fact
                    return new TrestleOWLFact(factProperty, overriddenTemporal, newDbTemporal);
                })
                .filter(fact -> divergingFacts
                        .stream()
                        .anyMatch(result -> result.getProperty().equals(fact.getAxiom().getProperty())
                        ))
                .collect(Collectors.toList());

        final List<OWLNamedIndividual> individualsToUpdate = currentFacts
                .stream()
                .filter(result -> {
                    final OWLDataPropertyAssertionAxiom existingFactValue = df.getOWLDataPropertyAssertionAxiom(
                            df.getOWLDataProperty(result.getIndividual(PROPERTY).orElseThrow(() -> new RuntimeException("property is null")).asOWLNamedIndividual().getIRI()),
                            result.getIndividual(INDIVIDUAL).orElseThrow(() -> new RuntimeException("individual is null")),
                            result.getLiteral(OBJECT).orElseThrow(() -> new RuntimeException("object is null")));
                    return divergingFacts
                            .stream()
                            .map(OWLPropertyAssertionAxiom::getProperty)
                            .anyMatch(property -> property.equals(existingFactValue.getProperty()));
                })
                .map(result -> result.getIndividual("fact").orElseThrow(() -> new RuntimeException("Fact is null")).asOWLNamedIndividual())
                .collect(Collectors.toList());

        return new MergeScript(individualsToUpdate, factsToVersion, divergingFacts);
    }

    private static MergeScript noMerge(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts) {
        final List<OWLDataProperty> newProperties = newFacts
                .stream()
                .map(fact -> fact.getProperty().asOWLDataProperty())
                .collect(Collectors.toList());
        final List<OWLDataProperty> existingProperties = existingFacts
                .stream()
                .map(fact -> fact.getIndividual(PROPERTY))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(property -> df.getOWLDataProperty(property.asOWLNamedIndividual().getIRI()))
                .collect(Collectors.toList());
        final boolean overlappingFacts = newProperties
                .stream()
                .anyMatch(existingProperties::contains);
        if (overlappingFacts) {
            throw new TrestleMergeConflict("Overlapping facts");
        }
        return new MergeScript(Collections.emptyList(), Collections.emptyList(), newFacts);
    }

    private static TemporalObject overrideTemporal(TemporalObject temporal, Temporal eventTemporal) {
        switch (temporal.getScope()) {
            case VALID: {
                if (temporal.isInterval()) {
                    return overrideIntervalTemporal(TemporalScope.VALID, temporal, eventTemporal);
                } else {
                    return temporal;
                }
            }
            case DATABASE: {
                if (temporal.isInterval()) {
                    return overrideIntervalTemporal(TemporalScope.DATABASE, temporal, eventTemporal);
                } else {
                    return temporal;
                }
            }
            case EXISTS: {
                if (temporal.isInterval()) {
                    return overrideIntervalTemporal(TemporalScope.EXISTS, temporal, eventTemporal);
                } else {
                    return temporal;
                }
            }
            default:
                throw new IllegalArgumentException("Wrong temporal type");
        }
    }

    private static TemporalObject overrideIntervalTemporal(TemporalScope scope, TemporalObject temporal, Temporal eventTemporal) {
        switch (scope) {
            case VALID:
                return TemporalObjectBuilder.valid().from(temporal.asInterval().getFromTime()).to(eventTemporal).build();
            case EXISTS:
                return TemporalObjectBuilder.exists().from(temporal.asInterval().getFromTime()).to(eventTemporal).build();
            case DATABASE:
                return TemporalObjectBuilder.database().from(temporal.asInterval().getFromTime()).to(eventTemporal).build();
            default:
                throw new IllegalArgumentException("Wrong temporal type");
        }
    }
}