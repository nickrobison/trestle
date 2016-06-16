package com.nickrobison.trixie.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.hp.hpl.jena.query.ResultSet;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/15/16.
 */
public class LocalOntology implements ITrixieOntology {

    private final static Logger logger = LoggerFactory.getLogger(LocalOntology.class);
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;


    LocalOntology(OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner) {
        ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
    }

    public static Builder from(IRI iri) {
        return new Builder(iri);
    }

    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    public void applyChange(OWLAxiomChange... axiom) {
        applyChanges(axiom);
    }

    private void applyChanges(OWLAxiomChange... axioms) {
        ontology.getOWLOntologyManager().applyChanges(Arrays.asList(axioms));
    }

    public void close() {
        reasoner.dispose();
    }

    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        return reasoner.getInstances(owlClass, direct).getFlattened();
    }

//    TODO(nrobison): Does this actually work on a local ontology?
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        final Set<OWLNamedIndividual> entities = reasoner.getSameIndividuals(individual).getEntities();
        if (entities.contains(individual)) {
            return Optional.of(individual);
        } else {
            return Optional.empty();
        }
    }

    public IRI getFullIRI(IRI iri) {
        return pm.getIRI(iri.toString());
    }

    public IRI getFullIRI(String prefix, String suffix) {
        return getFullIRI(IRI.create(prefix, suffix));
    }

    public void initializeOntology(boolean oracle) {

    }

    public void initializeOracleOntology(IRI filename) {

    }

    public void initializeOracleOntology() {

    }

    public ResultSet executeSPARQL(String query) {
        return null;
    }
}
