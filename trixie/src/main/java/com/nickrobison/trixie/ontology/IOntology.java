package com.nickrobison.trixie.ontology;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public interface IOntology {

    boolean isConsistent();
    void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException;
    void applyChange(OWLAxiomChange... axiom);

    OWLOntology getUnderlyingOntology();

    DefaultPrefixManager getUnderlyingPrefixManager();

    Set<OWLNamedIndividual> getInstances(OWLClass gaulObject);

    void initializeOracleOntology(IRI filename);
}
