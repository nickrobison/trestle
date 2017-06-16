package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.reasoner.types.TemporalScope;
import com.nickrobison.trestle.reasoner.types.TrestleOWLFact;
import com.nickrobison.trestle.reasoner.types.temporal.TemporalObject;
import com.nickrobison.trestle.reasoner.types.temporal.TemporalObjectBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 6/13/17.
 */
public class MergeEngineImpl implements TrestleMergeEngine, Serializable {

    private static final long serialVersionUID = 42L;
    private static final Logger logger = LoggerFactory.getLogger(MergeEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private final Config config;
    private MergeStrategy defaultStrategy;

    public MergeEngineImpl() {
        config = ConfigFactory.load().getConfig("trestle.merge");
        defaultStrategy = MergeStrategy.valueOf(config.getString("defaultStrategy"));
    }

    @Override
    public void changeDefaultMergeStrategy(MergeStrategy strategy) {
        logger.info("Changin default merge strategy from {} to {}", this.defaultStrategy, strategy);
        this.defaultStrategy = strategy;
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal) {
        return mergeFacts(newFacts, existingFacts, eventTemporal, MergeStrategy.Default);
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, MergeStrategy strategy) {
        if (strategy.equals(MergeStrategy.Default)) {
            strategy = defaultStrategy;
        }
        logger.debug("Merging facts using the {} strategy", strategy);
        switch (strategy) {
            case ContinuingFacts:
                return mergeLogic(newFacts, existingFacts, eventTemporal, true);
            case ExistingFacts:
                return mergeLogic(newFacts, existingFacts, eventTemporal, false);
            case NoMerge:
                return noMerge(newFacts, existingFacts);
            default:
//                    Do default stuff
                return null;
        }
    }

    private static MergeScript mergeLogic(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> currentFacts, Temporal eventTemporal, boolean continuingOnly) {
//        Determine if any of the currently valid facts diverge (have different values) from the newFacts we're attempting to write
        final List<OWLDataPropertyAssertionAxiom> divergingFacts = newFacts
                .stream()
                .filter(fact -> currentFacts
                        .stream()
                        .noneMatch(result -> {
                            final OWLDataPropertyAssertionAxiom resultFact = df.getOWLDataPropertyAssertionAxiom(
                                    df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new RuntimeException("Property is null")).asOWLNamedIndividual().getIRI()),
                                    result.getIndividual("individual").orElseThrow(() -> new RuntimeException("Individual is null")),
                                    result.getLiteral("object").orElseThrow(() -> new RuntimeException("Object is null")));
                            return resultFact.equals(fact);
                        }))
                .collect(Collectors.toList());

//                Do it the other way to find existing facts that will need to get a version increment
        final List<TrestleOWLFact> factsToVersion = currentFacts
                .stream()
                .map(result -> {
//                            Valid temporal
                    final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.VALID, result.getLiteral("va"), result.getLiteral("vf"), result.getLiteral("vt"));
//                            DB Temporal
                    final Optional<TemporalObject> dbTemporal = TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.DATABASE, Optional.empty(), result.getLiteral("df"), result.getLiteral("dt"));
//                            Parse the literal
                    final OWLDataPropertyAssertionAxiom factProperty = df.getOWLDataPropertyAssertionAxiom(
                            df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new RuntimeException("Property is null")).asOWLNamedIndividual().getIRI()),
                            result.getIndividual("individual").orElseThrow(() -> new RuntimeException("Individual is null")),
                            result.getLiteral("object").orElseThrow(() -> new RuntimeException("Object is null")));

//                    Override the valid temporal with the new ending date
                    final TemporalObject validTemporal1 = validTemporal.orElseThrow(() -> new RuntimeException("Fact valid temporal is null"));
                    if (continuingOnly && !validTemporal1.isContinuing()) {
                        throw new TrestleMergeConflict("Not all interval facts are continuing");
                    }
                    TemporalObject overriddenTemporal = overrideTemporal(validTemporal1, eventTemporal);

//                            Build the Fact
                    return new TrestleOWLFact(factProperty, overriddenTemporal, dbTemporal.orElseThrow(() -> new RuntimeException("Fact db temporal is null")));
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
                            df.getOWLDataProperty(result.getIndividual("property").orElseThrow(() -> new RuntimeException("property is null")).asOWLNamedIndividual().getIRI()),
                            result.getIndividual("individual").orElseThrow(() -> new RuntimeException("individual is null")),
                            result.getLiteral("object").orElseThrow(() -> new RuntimeException("object is null")));
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
        return null;
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
                throw new RuntimeException("Wrong temporal type");
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
                throw new RuntimeException("Wrong temporal type");
        }
    }
}