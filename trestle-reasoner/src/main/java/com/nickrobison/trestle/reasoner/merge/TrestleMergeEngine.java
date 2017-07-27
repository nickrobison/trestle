package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 6/13/17.
 */
public interface TrestleMergeEngine {
    /**
     * Change to default {@link MergeStrategy}
     *
     * @param strategy - new {@link MergeStrategy} to use for objects and facts
     */
    void changeDefaultMergeStrategy(MergeStrategy strategy);

    /**
     * Perform merge calculation using the default {@link MergeStrategy} set in the reasoner config
     * Provides two {@link Temporal}s to determine where to set the valid/db merge points
     * Throws a {@link TrestleMergeConflict} is the strategy is violated
     *  @param newFacts         - List of {@link OWLDataPropertyAssertionAxiom} to merge with existing facts
     * @param existingFacts    - List of {@link TrestleResult} representing existing, currently valid facts
     * @param eventTemporal    - {@link Temporal} to use as the new valid temporal
     * @param databaseTemporal - {@link Temporal} to use as the new database temporal
     * @param existsTemporal - {@link Optional} {@link TemporalObject} to use to fulfill {@link ExistenceStrategy}
     */
    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal);

    /**
     * Perform merge calculation using provided {@link MergeStrategy}
     * Provides two {@link Temporal}s to determine where to set the valid/db merge points
     * Throws a {@link TrestleMergeConflict} is the strategy is violated
     *  @param newFacts         - List of {@link OWLDataPropertyAssertionAxiom} to merge with existing facts
     * @param existingFacts    - List of {@link TrestleResult} representing existing, currently valid facts
     * @param eventTemporal    - {@link Temporal} to use as the new valid temporal
     * @param databaseTemporal - {@link Temporal} to use as the new database temporal
     * @param existsTemporal - {@link Optional} {@link TemporalObject} to use to fulfill {@link ExistenceStrategy}
     * @param strategy         - {@link MergeStrategy} to use when merging facts  @return - {@link MergeScript}
     */
    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal, MergeStrategy strategy);

    /**
     * Determine if the {@link TrestleMergeEngine} is enabled or not.
     * @return - {@code true} Merge is enabled and engine is a {@link MergeEngineImpl}. {@code false} Merge is disabled and engine is a {@link MergeEngineNoOp}
     */
    boolean mergeEnabled();

    /**
     * Merge on load?
     * @return - {@code true} merge objects on merge. {@code false} merge only facts
     */
    boolean mergeOnLoad();
}
