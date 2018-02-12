package com.nickrobison.trestle.reasoner.engines.merge;

import com.nickrobison.trestle.types.TrestleOWLFact;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collections;
import java.util.List;

/**
 * Created by nrobison on 6/13/17.
 */

/**
 * List of operations to perform in order to accurately update facts and objects
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MergeScript {
    private final List<OWLDataPropertyAssertionAxiom> individualToUpdate;
    private final List<OWLNamedIndividual> factsToVersion;
    private final List<TrestleOWLFact> newFactVersions;
    private final List<OWLDataPropertyAssertionAxiom> newFacts;

    public MergeScript(List<OWLNamedIndividual> factsToVersion, List<TrestleOWLFact> newFactVersions, List<OWLDataPropertyAssertionAxiom> newFacts) {
        this.individualToUpdate = Collections.emptyList();
        this.factsToVersion = factsToVersion;
        this.newFactVersions = newFactVersions;
        this.newFacts = newFacts;
    }

    public MergeScript(List<OWLDataPropertyAssertionAxiom> individualAxioms, List<OWLNamedIndividual> factsToVersion, List<TrestleOWLFact> newFactVersions, List<OWLDataPropertyAssertionAxiom> newFacts) {
        this.individualToUpdate = individualAxioms;
        this.factsToVersion = factsToVersion;
        this.newFactVersions = newFactVersions;
        this.newFacts = newFacts;
    }

    public List<OWLDataPropertyAssertionAxiom> getIndividualExistenceAxioms() {
        return this.individualToUpdate;
    }

    public List<OWLNamedIndividual> getFactsToVersion() {
        return factsToVersion;
    }

    public OWLNamedIndividual[] getFactsToVersionAsArray() {
        return this.factsToVersion.toArray(new OWLNamedIndividual[this.factsToVersion.size()]);
    }

    public List<TrestleOWLFact> getNewFactVersions() {
        return newFactVersions;
    }

    public List<OWLDataPropertyAssertionAxiom> getNewFacts() {
        return newFacts;
    }
}
