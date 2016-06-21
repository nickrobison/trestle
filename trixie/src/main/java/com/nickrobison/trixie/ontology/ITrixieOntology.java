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

    /**
     * Checks whether or not the ontology is consistent
     * @return - Is the ontology consistent?
     */
    boolean isConsistent();

    /**
     * Write underlying ontology to disk
     * @param path - IRI of path to write ontology
     * @param validate - Validate ontology before writing?
     * @throws OWLOntologyStorageException
     */
    void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException;

    /**
     * Apply change to OWL Ontology
     * @param axiom - OWLAxiomChange to apply
     */
    void applyChange(OWLAxiomChange... axiom);

    /**
     * Close all the open resource handles. Disposes of the reasoner and disconnects from any databases.
     */
    void close();

    /**
     * Get the underlying ontology, if needed. Should really use specific APIs
     *
     * @return - OWLOntology
     */
    OWLOntology getUnderlyingOntology();

    /**
     * Get the underlying prefix manager, if needed. Should really use specific APIs
     *
     * @return - DefaultPrefixManager
     */
    DefaultPrefixManager getUnderlyingPrefixManager();

    /**
     * Get all the instances of an OWL Class
     *
     * @param owlClass - OWLClass to retrieve members of
     * @param direct   - Return only the direct class members?
     * @return - Set of OWLNamedIndividual(s) either directly related or inferred members of given class
     */
    Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct);

    /**
     * Get an OWLNamedIndividual if it exists in the ontology
     *
     * @param individual - OWLNamedIndividual to retrieve
     * @return - Optional of OWLNamedIndividual, if it exists in the ontology
     */
    Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual);

    /**
     * Get the full IRI expanded from the DefaultPrefixManager
     *
     * @param iri - Abbreviated IRI
     * @return - Fully expanded IRI
     */
    IRI getFullIRI(IRI iri);

    /**
     * Get the full IRI expanded from the DefaultPrefixManager
     *
     * @param prefix - String prefix of the IRI
     * @param suffix - String suffix of the IRI
     * @return - IRI, fully expanded IRI
     */
    IRI getFullIRI(String prefix, String suffix);

    /**
     * Initialize the ontology from the base ontology provided by the Builder
     */
    void initializeOntology();

    /**
     * Execute a raw SPARQL query against the ontology
     * @param query - String representing SPARQL query
     * @return - ResultSet from given query
     */
    ResultSet executeSPARQL(String query);

}
