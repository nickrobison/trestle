package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

import java.util.List;

/**
 * Created by nrobison on 6/13/17.
 */
public interface TrestleMergeEngine {
    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts);

    /**
     * Perform merge calculation using provided {@link MergeStrategy}
     * Throws a {@link TrestleMergeConflict} is the strategy is violated
     * @param newFacts - List of {@link OWLDataPropertyAssertionAxiom} to merge with existing facts
     * @param existingFacts - List of {@link TrestleResult} representing existing, currently valid facts
     * @param strategy - {@link MergeStrategy} to use when merging facts
     * @return - {@link MergeScript}
     */
    MergeScript mergeFacts(List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, MergeStrategy strategy);
}
