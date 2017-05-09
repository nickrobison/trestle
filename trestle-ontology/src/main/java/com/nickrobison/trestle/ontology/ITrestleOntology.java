package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public interface ITrestleOntology {

    /**
     * Checks whether or not the ontology is consistent
     * @return - Is the ontology consistent?
     */
    boolean isConsistent();

    /**
     * Returns an optional list of asserted property values from a given individual
     * @param individual - OWLNamedIndividual to query
     * @param propertyIRI - IRI of property to retrieve
     * @return - Optional list of all asserted property values
     */
    Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI);

    /**
     * Returns an optional list of asserted property values from a given individual
     * @param individualIRI - IRI of individual to query
     * @param objectPropertyIRI - IRI of property to retrieve
     * @return - Optional list of all asserted property values
     */
    Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI);

    /**
     * Returns an optional list of asserted property values from a given individual
     *
     * @param individual - OWLNamedIndividual to query
     * @param property   - OWLObjectProperty to retrieve
     * @return - Optional list of all asserted property values
     */
    Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property);

    /**
     * Store an OWLNamedIndividual in the ontology from a given classAxiom
     *
     * @param owlClassAssertionAxiom - Class axiom to store in the ontology with RDF.type relation
     */
//    FIXME(nrobison): This should have the ability to be locked to avoid polluting the ontology
    void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom);

    /**
     * Store an OWLNamedIndividual in the ontology associated with a given OWLClass.
     * @param individual - OWLNamedIndividual to store in ontology
     * @param owlClass - OWLClass to associate individual with
     */
    void createIndividual(OWLNamedIndividual individual, OWLClass owlClass);

    /**
     * Store an OWLNamedIndividual in the ontology from a given pair of IRIs
     * @param individualIRI - IRI of OWLIndividual
     * @param classIRI - IRI of OWLClass
     */
    void createIndividual(IRI individualIRI, IRI classIRI);

    /**
     * Create a subclass association from express sub/super pair
     * @param subClass - OWLClass to associated with super-class
     * @param superClass - OWLClass to assocate sub-class to
     */
    void associateOWLClass(OWLClass subClass, OWLClass superClass);

    /**
     * Create a subclass association directly from OWL Axiom
     * @param subClassOfAxiom - OWLSubClassOfAxiom to create in ontology
     */
    void associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom);

    /**
     * Create a property in the underlying model.
     * Determines if the property is an Object or Data Property
     *
     * @param property - Property to store in the model
     */
    //    FIXME(nrobison): This should have the ability to be locked to avoid polluting the ontology
    void createProperty(OWLProperty property);

    /**
     * Write an individual data property axiom to the ontology from individual IRIs
     * @param individualIRI - IRI of OWLNamedIndividual to associate property
     * @param dataPropertyIRI - IRI of OWLDataProperty to associate with individual
     * @param owlLiteralString - String of raw data property value
     * @param owlLiteralIRI - IRI of OWLDatatype of raw property value
     * @throws MissingOntologyEntity - Throws if individual doesn't exist
     */
    void writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI) throws MissingOntologyEntity;

    /**
     * Write an individual data property axiom to the ontology from OWLExpressions
     *
     * @param individual - OWLNameIndividual to associate property
     * @param property   - OWLDataProperty to associate with individual
     * @param value      - OWLLiteral value of data property
     * @throws MissingOntologyEntity - Throws if individual doesn't exist
     */
    void writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value) throws MissingOntologyEntity;

    /**
     * Write an individual data property axiom to the model.
     * Creates the data property if it doesn't exist
     *
     * @param dataProperty - Data property axiom to store in the more
     * @throws MissingOntologyEntity - Throws an exception if the subject doesn't exist.
     */
    void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity;

    /**
     *
     * Create object association between two OWLNamedIndividuals using base IRIs
     * @param owlSubject - OWLNamedIndividual subject
     * @param propertyIRI - IRI of OWLObjectProperty
     * @param owlObject - OWLNamedIndividual object
     * @throws MissingOntologyEntity - Throws if individual doesn't exist
     */
    void writeIndividualObjectProperty(OWLNamedIndividual owlSubject, IRI propertyIRI, OWLNamedIndividual owlObject) throws MissingOntologyEntity;

    /**
     * Create object association between two OWLNamedIndividuals using base IRIs
     * @param owlSubject - IRI of OWLNamedIndividual subject
     * @param owlProperty - IRI of OWLObjectProperty
     * @param owlObject - IRI of OWLNamedIndividual object
     * @throws MissingOntologyEntity - Throws if individual doesn't exist
     */
    void writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject) throws MissingOntologyEntity;

    /**
     * Create object association between two OWLNamedIndividuals
     * @param property - OWLObjectPropertyAssertionAxiom defining relationship between the two objects
     * @throws MissingOntologyEntity - Throws if individual doesn't exist
     */
    void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity;

    /**
     * Removes a given OWL individual and all related assertions
     * @param individual - OWLNamedIndividual to remove
     */
    void removeIndividual(OWLNamedIndividual individual);

    /**
     * Check whether the ontology contains an individual with the given IRI
     * @param individualIRI - IRI of individual to check
     * @return - boolean, individual exists?
     */
    boolean containsResource(IRI individualIRI);

    /**
     * Check whether the underlying model contains the given OWLEntity
     *
     * @param individual - OWLNamedObject to verify existence
     * @return - boolean object exists?
     */
    boolean containsResource(OWLNamedObject individual);

    /**
     * Write underlying ontology to disk
     * @param path - IRI of path to write ontology
     * @param validate - Validate ontology before writing?
     * @throws OWLOntologyStorageException - Throws if it can't write the ontology
     */
    void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException;

    /**
     * Close all the open resource handles. Disposes of the reasoner and disconnects from any databases.
     * @param drop - Drop the ontology on close?
     */
    void close(boolean drop);

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
     * Open a transaction to facilitate rapid loading of ontology objects
     * @param write - Open a writable transaction
     */
    void openTransaction(boolean write);

    /**
     * Close open transaction
     * @param write - Is this a write transaction?
     */
    void commitTransaction(boolean write);

    /**
     * Manually run inference on the ontology
     */
    void runInference();

    /**
     * Get all the instances of an OWL Class
     *
     * @param owlClass - OWLClass to retrieve members of
     * @param inferred   - Return inferred class members?
     * @return - Set of OWLNamedIndividual(s) either directly related or inferred members of given class
     */
    Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred);

    /**
     * Return data properties for a given individual
     * @param individualIRI - IRI of individual to retrieve properties of
     * @param properties - List of OWLDataProperties to retrieve
     * @return - Set of OWLDataPropertyAssertionAxioms from given individual
     */
    Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, List<OWLDataProperty> properties);

    /**
     * Return all the data properties and values for a given individual
     * @param individual - OWLNamedIndividual to get properties for
     * @param properties - List of OWLDataProperties to retrieve for individual
     * @return - Set of OWLDataPropertyAssertionAxioms from individual
     */
    Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, List<OWLDataProperty> properties);

    /**
     * Get all asserted properties for a given individual
     * @param individualIRI - IRI of individual to get properties for
     * @return Set of OWLDataPropertyAssertionAxioms
     */
    Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI);

    /**
     * Get all asserted properties for a given individual
     * @param individual - OWLNamedIndividual to get properties for
     * @return - Set of OWLDataPropertyAssertionAxioms
     */
    Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual);

    /**
     * Get all object properties for a given individual IRI
     * @param individual - IRI of individual to retrieve properties for
     * @return - Set of OWLObjectPropertyAssertionAxioms for a given individual IRI
     */
    Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual);

    /**
     * Get all object properties for a given individual
     * @param individual - OWLNamedIndividual to retrieve properties for
     * @return - Set of OWLObjectPropertyAssertionAxioms for a given individual
     */
    Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual);

    /**
     * Get an OWLNamedIndividual if it exists in the ontology
     *
     * @param individual - OWLNamedIndividual to retrieve
     * @return - Optional of OWLNamedIndividual, if it exists in the ontology
     */
    Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual);

    /**
     * Return a set of values for a given data property
     * @param individual - OWLNamedIndividual to retrieve properties for
     * @param propertyIRI - IRI of dataproperty to retrieve
     * @return - Optional Set of OWLLiteral values for given property
     */
    Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI);

    /**
     * Return a set of values for a given data property
     * @param individualIRI - IRI of individual to retrieve properties for
     * @param property - OWLDataProperty to retrieve values from
     * @return - Optional Set of OWLLiteral values for given property of specific individual
     */
    Optional<Set<OWLLiteral>> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property);

    /**
     * Return the set of data property values
     * @param individual - OWLNamedIndividual to get properties from
     * @param property - OWLDataProperty to access
     * @return - Optional set of OWLLiteral if a property exists on that member
     */
    Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property);

    /**
     * Get all the related facts for an individual, valid at a specific valid/database point
     * If no temporals are specified, we retrieve the currently valid facts
     * @param individual - {@link OWLNamedIndividual} to get facts for
     * @param validTemporal - Nullable {@link OffsetDateTime} representing valid-at temporal
     * @param databaseTemporal - Nullable {@link OffsetDateTime} representing database-at temporal
     * @param filterTemporals - {@code true} remove temporals from the result set
     * @return - {@link Set} of {@link OWLDataPropertyAssertionAxiom} which represent all asserted Facts on the individual
     */
    Set<OWLDataPropertyAssertionAxiom> getFactsForIndividual(OWLNamedIndividual individual, OffsetDateTime validTemporal, OffsetDateTime databaseTemporal, boolean filterTemporals);

    /**
     * Get data properties for temporal from given individuals
     * @param individual - Individual to retrieve temporal properties from
     * @return - Set of OWLDataPropertyAssertionAxioms representing temporal properties
     */
    Set<OWLDataPropertyAssertionAxiom> getTemporalsForIndividual(OWLNamedIndividual individual);

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
     * Get the full IRI of the OWL Object expanded from the DefaultPrefixManager
     * @param owlNamedObject - OWL Object to extract IRI from
     * @return - IRI of OWL Object
     */
    IRI getFullIRI(OWLNamedObject owlNamedObject);

    /**
     * Get the full IRI expanded from the DefaultPrefixManager as a String
     * @param owlNamedObject - OWL Object to extract IRI from
     * @return - String of OWL Object IRI
     */
    String getFullIRIString(OWLNamedObject owlNamedObject);

    /**
     * Excecute a raw SPARQL query against the ontology
     * @param queryString - String representing SPARQL query
     * @return - {@link TrestleResultSet} for given query
     */
    TrestleResultSet executeSPARQLResults(String queryString);

    /**
     * Execute a writing SPARQL query, without returning a {@link TrestleResultSet}
     * @param queryString - SPARQL Query String
     */
    void executeUpdateSPARQL(String queryString);

//    /**
//     * Open a transaction and lock it, for lots of bulk action
//     */
//    void lock();

    /**
     * Takes an existing transaction object and inherits from it
     *
     * @param transactionObject - Transaction Object to take ownership of thread transaction
     * @param write - Writable transaction?
     * @return - Transaction Object passed in as argument
     */
    TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject, boolean write);

    /**
     * Takes an existing transaction object and inherits from it
     * @param transactionObject - Existing TrestleTransactionObject
     * @return - Transaction Object passed in as argument
     */
    TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject);

    TrestleTransaction createandOpenNewTransaction(boolean write);

    /**
     * Try to commit the current thread transaction, if the object owns the currently open transaction
     *
     * @param transaction - Transaction object to try to commit current transaction with
     */
    void returnAndCommitTransaction(TrestleTransaction transaction);

    /**
     * Return a TrestleTransaction object and attempt to abort the current Transaction
     * If the TrestleTransaction object does not own the current transaction, we continue without aborting
     * @param transaction - Transaction object to try to abort current transaction with
     */
    void returnAndAbortTransaction(TrestleTransaction transaction);

    /**
     * Open a transaction and lock it
     *
     * @param write - Open writable transaction?
     */
    void openAndLock(boolean write);
//
//    /**
//     * Unlock the model to allow for closing the transaction
//     */
//    void unlock();

    /**
     * Unlock the transaction and commit it
     * @param write - Is this a write transaction?
     */
    void unlockAndCommit(boolean write);
}
