package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
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
    private final MergeStrategy defaultStrategy;

    public MergeEngineImpl() {
        config = ConfigFactory.load().getConfig("trestle.merge");
        defaultStrategy = MergeStrategy.valueOf(config.getString("defaultStrategy"));
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts) {
        return mergeFacts(newFacts, existingFacts, MergeStrategy.Default);
    }

    @Override
    public MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, MergeStrategy strategy) {
        if (strategy.equals(MergeStrategy.Default)) {
            strategy = defaultStrategy;
        }
        logger.debug("Merging facts using the {} strategy", defaultStrategy);
        switch (strategy) {
            case ContinuingFacts:
                return mergeContinuing(newFacts, existingFacts);
            case ExistingFacts:
                return mergeExisting(newFacts, existingFacts);
            case NoMerge:
                return noMerge(newFacts, existingFacts);
            default:
//                    Do default stuff
                return null;
        }
    }

    private static MergeScript mergeContinuing(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> currentFacts) {
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
//                            Build the Fact
                    return new TrestleOWLFact(factProperty, validTemporal.orElseThrow(() -> new RuntimeException("Fact valid temporal is null")), dbTemporal.orElseThrow(() -> new RuntimeException("Fact db temporal is null")));
                })
                .filter(fact -> divergingFacts
                        .stream()
                        .anyMatch(result -> result.getProperty().equals(fact.getAxiom().getProperty())
                        ))
                .collect(Collectors.toList());

        //        Check to ensure all the existing facts are continuing
        final List<TrestleOWLFact> nonContinuingFacts = factsToVersion
                .stream()
                .filter(fact -> !fact.getValidTemporal().isContinuing())
                .collect(Collectors.toList());
        if (!nonContinuingFacts.isEmpty()) {
            throw new TrestleMergeConflict("Not all interval facts are continuing");
        }


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

    private static MergeScript mergeExisting(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts) {
        return null;
    }

    private static MergeScript noMerge(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts) {
        return null;
    }
}
