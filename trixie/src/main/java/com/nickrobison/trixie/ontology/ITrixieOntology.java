package com.nickrobison.trixie.ontology;

import com.hp.hpl.jena.query.ResultSet;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public interface ITrixieOntology {

    boolean isConsistent();
    void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException;
    void applyChange(OWLAxiomChange... axiom);
    void close();

    OWLOntology getUnderlyingOntology();

    DefaultPrefixManager getUnderlyingPrefixManager();

    Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct);

    Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual);

    IRI getFullIRI(IRI iri);

    void initializeOntology(boolean oracle);

    void initializeOracleOntology(IRI filename);

    void initializeOracleOntology();

    ResultSet executeSPARQL(String query);
}
