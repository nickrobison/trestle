package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TrestleOWLFact;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.temporalExistsFromIRI;
import static com.nickrobison.trestle.common.StaticIRI.temporalExistsToIRI;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Created by nrobison on 6/13/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MergeEngineImpl implements TrestleMergeEngine {

    private static final Logger logger = LoggerFactory.getLogger(MergeEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final String PROPERTY = "property";
    public static final String INDIVIDUAL = "individual";
    public static final String OBJECT = "object";
    private final Config config;
    private MergeStrategy defaultStrategy;
    private ExistenceStrategy existenceStrategy;
    private final boolean onLoad;

    public MergeEngineImpl() {
        config = ConfigFactory.load().getConfig("trestle.merge");
        defaultStrategy = MergeStrategy.valueOf(config.getString("defaultStrategy"));
        existenceStrategy = ExistenceStrategy.valueOf(config.getString("existenceStrategy"));
        this.onLoad = config.getBoolean("onLoad");
    }

    @Override
    public void changeDefaultMergeStrategy(MergeStrategy strategy) {
        logger.info("Changing default merge strategy from {} to {}", this.defaultStrategy, strategy);
        this.defaultStrategy = strategy;
    }

    @Override
    public void changeDefaultExistenceStrategy(ExistenceStrategy strategy) {
        logger.info("Changing default existence strategy from {} to {}", this.existenceStrategy, strategy);
        this.existenceStrategy = strategy;
    }

    @Override
    public MergeScript mergeFacts(OWLNamedIndividual individual, TemporalObject validTemporal, List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal) {
        return mergeFacts(individual, validTemporal, newFacts, existingFacts, eventTemporal, databaseTemporal, existsTemporal, MergeStrategy.Default);
    }

    @Override
    public MergeScript mergeFacts(OWLNamedIndividual individual, TemporalObject validTemporal, List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal, MergeStrategy strategy) {
        final MergeStrategy methodStrategy;
        if (strategy.equals(MergeStrategy.Default)) {
            methodStrategy = this.defaultStrategy;
        } else {
            methodStrategy = strategy;
        }

        final ExistenceStrategy eStrategy;
        if (existenceStrategy.equals(ExistenceStrategy.Default)) {
            eStrategy = this.existenceStrategy;
        } else {
            eStrategy = existenceStrategy;
        }
        logger.debug("Merging facts using the {} strategy", strategy);

        switch (methodStrategy) {
            case ContinuingFacts:
                return mergeLogic(individual, newFacts, existingFacts, validTemporal, databaseTemporal, existsTemporal, eStrategy, true);
            case ExistingFacts:
                return mergeLogic(individual, newFacts, existingFacts, validTemporal, databaseTemporal, existsTemporal, eStrategy, false);
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

    @Override
    public boolean existenceEnabled() {
        return !this.existenceStrategy.equals(ExistenceStrategy.Ignore);
    }

    private static MergeScript mergeLogic(OWLNamedIndividual individual, List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> currentFacts, TemporalObject eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existenceTemporal, ExistenceStrategy existenceStrategy, boolean continuingOnly) {

//        Start with existence
        final List<OWLDataPropertyAssertionAxiom> existenceAxioms = validateExistence(existenceStrategy, individual, existenceTemporal, eventTemporal);
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
                    TemporalObject overriddenTemporal = overrideTemporal(validTemporal1, eventTemporal.getIdTemporal());
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

        return new MergeScript(existenceAxioms, individualsToUpdate, factsToVersion, divergingFacts);
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

    private static List<OWLDataPropertyAssertionAxiom> validateExistence(ExistenceStrategy strategy, OWLNamedIndividual individual, Optional<TemporalObject> existsTemporal, TemporalObject validTemporal) {

        switch (strategy) {
            case Ignore:
                return Collections.emptyList();
            case During:
                return validDuringExistence(existsTemporal, validTemporal);
            case Extend:
                return extendExistence(individual, existsTemporal, validTemporal);
            case Default:
//                We shouldn't be able to end up here
                throw new TrestleMergeException("Can't execute default merge, should use strategy from config");
        }
        return Collections.emptyList();
    }

    private static List<OWLDataPropertyAssertionAxiom> validDuringExistence(Optional<TemporalObject> existsTemporalOptional, TemporalObject validTemporal) {
        if (existsTemporalOptional.isPresent()) {
            final TemporalObject existsTemporal = existsTemporalOptional.get();
            if (validTemporal.during(existsTemporal)) {
                return Collections.emptyList();
            }
            throw new TrestleMergeException(String.format("Merge temporal %s is not during exists temporal %s", validTemporal, existsTemporal));
        }
        throw new TrestleMergeException("Missing exists temporal");
    }

    private static List<OWLDataPropertyAssertionAxiom> extendExistence(OWLNamedIndividual individual, Optional<TemporalObject> existsTemporalOptional, TemporalObject validTemporal) {
        if (existsTemporalOptional.isPresent()) {
            final TemporalObject existsTemporal = existsTemporalOptional.get();
//            If valid temporal is entirely within the exists temporal, hurrah! Nothing to do
            if (validTemporal.during(existsTemporal)) {
                return Collections.emptyList();
            }
            if (!existsTemporal.isContinuing() && validTemporal.isContinuing()) {
                throw new TrestleMergeException("Can't merge a non-continuing object with a continuing fact");
            }
            List<OWLDataPropertyAssertionAxiom> individualAxioms = new ArrayList<>();
//            Extend the exists start?
            if (TemporalUtils.compareTemporals(validTemporal.getIdTemporal(), existsTemporal.getIdTemporal()) == -1) {
                individualAxioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsFromIRI),
                        individual,
                        df.getOWLLiteral(parseTemporalToOntologyDateTime(validTemporal.getIdTemporal(), ZoneOffset.UTC).toString(), OWL2Datatype.XSD_DATE_TIME)));
            }
            if (!existsTemporal.isContinuing() && validTemporal.isInterval() && !validTemporal.isContinuing()) {
                final int toCompare = TemporalUtils.compareTemporals((Temporal) validTemporal.asInterval().getToTime().get(), (Temporal) existsTemporal.asInterval().getToTime().get());
                if (toCompare != -1) {
                    individualAxioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsToIRI),
                            individual,
                            df.getOWLLiteral(parseTemporalToOntologyDateTime((Temporal) validTemporal.asInterval().getAdjustedToTime(1).get(), ZoneOffset.UTC).toString(), OWL2Datatype.XSD_DATE_TIME)));
                }
            }
            return individualAxioms;
        }
        throw new TrestleMergeException("Missing exists temporal");
    }

    private static TemporalObject overrideTemporal(TemporalObject temporal, Temporal eventTemporal) {
        switch (temporal.getScope()) {
            case VALID: {
                return overrideTemporalScope(temporal, eventTemporal, TemporalScope.VALID);
            }
            case DATABASE: {
                return overrideTemporalScope(temporal, eventTemporal, TemporalScope.DATABASE);
            }
            case EXISTS: {
                return overrideTemporalScope(temporal, eventTemporal, TemporalScope.EXISTS);
            }
            default:
                throw new IllegalArgumentException("Wrong temporal type");
        }
    }

    private static TemporalObject overrideTemporalScope(TemporalObject temporal, Temporal eventTemporal, TemporalScope valid) {
        if (temporal.isInterval()) {
            return overrideIntervalTemporal(valid, temporal, eventTemporal);
        } else {
            return temporal;
        }
    }

    @SuppressWarnings("unchecked")
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