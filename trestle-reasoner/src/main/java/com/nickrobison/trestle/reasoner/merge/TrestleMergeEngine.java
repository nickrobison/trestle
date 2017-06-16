package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

import java.time.temporal.Temporal;
import java.util.List;

/**
 * Created by nrobison on 6/13/17.
 */
public interface TrestleMergeEngine {
    /**
     * Change to default {@link MergeStrategy}
     * @param strategy - new {@link MergeStrategy} to use for objects and facts
     */
    void changeDefaultMergeStrategy(MergeStrategy strategy);

    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal);

    /**
     * Perform merge calculation using provided {@link MergeStrategy}
     * Throws a {@link TrestleMergeConflict} is the strategy is violated
     * @param newFacts - List of {@link OWLDataPropertyAssertionAxiom} to merge with existing facts
     * @param existingFacts - List of {@link TrestleResult} representing existing, currently valid facts
     * @param eventTemporal
     * @param strategy - {@link MergeStrategy} to use when merging facts  @return - {@link MergeScript}
     */
    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, MergeStrategy strategy);
}
