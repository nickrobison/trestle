package com.nickrobison.trestle.reasoner.merge;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 6/16/17.
 */
public class MergeEngineNoOp implements TrestleMergeEngine {
    private static final Logger logger = LoggerFactory.getLogger(MergeEngineNoOp.class);

    public MergeEngineNoOp() {
        logger.info("Merging disabled, creating No-Op merge engine");
    }

    @Override
    public void changeDefaultMergeStrategy(MergeStrategy strategy) {

    }

    @Override
    public void changeDefaultExistenceStrategy(ExistenceStrategy strategy) {

    }

    @Override
    public MergeScript mergeFacts(OWLNamedIndividual individual, TemporalObject validTemporal, List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal) {
        return new MergeScript(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public MergeScript mergeFacts(OWLNamedIndividual individual, TemporalObject validTemporal, List<OWLDataPropertyAssertionAxiom> newFacts, List<TrestleResult> existingFacts, Temporal eventTemporal, Temporal databaseTemporal, Optional<TemporalObject> existsTemporal, MergeStrategy strategy) {
        return new MergeScript(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public boolean mergeEnabled() {
        return false;
    }

    @Override
    public boolean mergeOnLoad() {
        return false;
    }

    @Override
    public boolean existenceEnabled() {
        return false;
    }
}
