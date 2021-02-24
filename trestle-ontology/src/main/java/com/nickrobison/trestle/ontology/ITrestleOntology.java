package com.nickrobison.trestle.ontology;

import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nrobison on 5/23/16.
 */
public interface ITrestleOntology {

    /**
     * Get the underlying {@link QueryBuilder} setup for the appropriate {@link QueryBuilder.Dialect}
     *
     * @return - {@link QueryBuilder}
     */
    QueryBuilder getUnderlyingQueryBuilder();

    /**
     * Returns an optional list of asserted property values from a given individual
     *
     * @param individual  - OWLNamedIndividual to query
     * @param propertyIRI - IRI of property to retrieve
     * @return - {@link Flowable} list of all asserted property values
     */
    Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI);

    /**
     * Returns an optional list of asserted property values from a given individual
     *
     * @param individualIRI     - IRI of individual to query
     * @param objectPropertyIRI - IRI of property to retrieve
     * @return - {@link Flowable} list of all asserted property values
     */
    Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI);

    /**
     * Returns an optional list of asserted property values from a given individual
     *
     * @param individual - OWLNamedIndividual to query
     * @param property   - OWLObjectProperty to retrieve
     * @return - Optional list of all asserted property values
     */
    Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property);

    /**
     * Store an OWLNamedIndividual in the ontology from a given classAxiom
     *
     * @param owlClassAssertionAxiom - Class axiom to store in the ontology with RDF.type relation
     * @return - {@link Completable} when finished
     */
    Completable createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom);

    /**
     * Store an OWLNamedIndividual in the ontology associated with a given OWLClass.
     *
     * @param individual - OWLNamedIndividual to store in ontology
     * @param owlClass   - OWLClass to associate individual with
     * @return - {@link Completable} when finished
     */
    Completable createIndividual(OWLNamedIndividual individual, OWLClass owlClass);

    /**
     * Store an OWLNamedIndividual in the ontology from a given pair of IRIs
     *
     * @param individualIRI - IRI of OWLIndividual
     * @param classIRI      - IRI of OWLClass
     * @return - {@link Completable} when finished
     */
    Completable createIndividual(IRI individualIRI, IRI classIRI);

    /**
     * Create a subclass association from express sub/super pair
     *
     * @param subClass   - OWLClass to associated with super-class
     * @param superClass - OWLClass to assocate sub-class to
     * @return - {@link Completable} when finished
     */
    Completable associateOWLClass(OWLClass subClass, OWLClass superClass);

    /**
     * Create a subclass association directly from OWL Axiom
     *
     * @param subClassOfAxiom - OWLSubClassOfAxiom to create in ontology
     * @return - {@link Completable} when finished
     */
    Completable associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom);

    /**
     * Create a property in the underlying model.
     * Determines if the property is an Object or Data Property
     *
     * @param property - Property to store in the model
     * @return - {@link Completable} when finished
     */
    Completable createProperty(OWLProperty property);

    /**
     * Write an individual data property axiom to the ontology from individual IRIs
     *
     * @param individualIRI    - IRI of OWLNamedIndividual to associate property
     * @param dataPropertyIRI  - IRI of OWLDataProperty to associate with individual
     * @param owlLiteralString - String of raw data property value
     * @param owlLiteralIRI    - IRI of OWLDatatype of raw property value
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI);

    /**
     * Write an individual data property axiom to the ontology from OWLExpressions
     *
     * @param individual - OWLNameIndividual to associate property
     * @param property   - OWLDataProperty to associate with individual
     * @param value      - OWLLiteral value of data property
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value);

    /**
     * Write an individual data property axiom to the model.
     * Creates the data property if it doesn't exist
     *
     * @param dataProperty - Data property axiom to store in the more
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty);

    /**
     * Create object association between two OWLNamedIndividuals using base IRIs
     *
     * @param owlSubject  - OWLNamedIndividual subject
     * @param propertyIRI - IRI of OWLObjectProperty
     * @param owlObject   - OWLNamedIndividual object
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualObjectProperty(OWLNamedIndividual owlSubject, IRI propertyIRI, OWLNamedIndividual owlObject);

    /**
     * Create object association between two OWLNamedIndividuals using base IRIs
     *
     * @param owlSubject  - IRI of OWLNamedIndividual subject
     * @param owlProperty - IRI of OWLObjectProperty
     * @param owlObject   - IRI of OWLNamedIndividual object
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject);

    /**
     * Create object association between two OWLNamedIndividuals
     *
     * @param property - OWLObjectPropertyAssertionAxiom defining relationship between the two objects
     * @return - {@link Completable} when finished
     */
    Completable writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property);

    /**
     * Removes a given OWL individual and all related assertions
     *
     * @param individual - OWLNamedIndividual to remove
     * @return - {@link Completable} when finished
     */
    Completable removeIndividual(OWLNamedIndividual individual);

    /**
     * Remove the given {@link OWLObjectProperty} for the specified individual
     * If a {@link OWLNamedIndividual} is provided as the object, only that assertion is remove.
     * Otherwise, all matching assertions are removed.
     *
     * @param subject  - {@link OWLNamedIndividual} subject
     * @param property - {@link OWLObjectProperty} property
     * @param object   - {@link OWLNamedIndividual} optional object
     * @return - {@link Completable} when finished
     */
    Completable removeIndividualObjectProperty(OWLNamedIndividual subject, OWLObjectProperty property, @Nullable OWLNamedIndividual object);

    /**
     * Remove the given {@link OWLDataPropertyAssertionAxiom} for the specified individual
     * If no {@link OWLLiteral} is provided, all matching assertions will be removed.
     *
     * @param individual - {@link OWLNamedIndividual} of subject
     * @param property   - {@link OWLDataProperty} of property
     * @param literal    - {@link OWLLiteral} optional literal value to remove
     * @return - {@link Completable} when finished
     */
    Completable removeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, @Nullable OWLLiteral literal);

    /**
     * Check whether the ontology contains an individual with the given IRI
     *
     * @param individualIRI - IRI of individual to check
     * @return - {@link Single} {@code true} individual exists. {@code false} individual does not exist
     */
    Single<Boolean> containsResource(IRI individualIRI);

    /**
     * Check whether the underlying model contains the given OWLEntity
     *
     * @param individual - OWLNamedObject to verify existence
     * @return - {@link Single} {@code true} individual exists. {@code false} individual does not exist
     */
    Single<Boolean> containsResource(OWLNamedObject individual);

    /**
     * Write underlying ontology to disk
     *
     * @param path     - IRI of path to write ontology
     * @param validate - Validate ontology before writing?
     * @throws OWLOntologyStorageException - Throws if it can't write the ontology
     */
    void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException;

    /**
     * Close all the open resource handles. Disposes of the reasoner and disconnects from any databases.
     *
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
     *
     * @param write - Open a writable transaction
     */
    void openTransaction(boolean write);

    /**
     * Close open transaction
     *
     * @param write - Is this a write transaction?
     */
    void commitTransaction(boolean write);

    /**
     * Get all the instances of an OWL Class
     *
     * @param owlClass - OWLClass to retrieve members of
     * @param inferred - Return inferred class members?
     * @return - {@link Flowable} of {@link OWLNamedIndividual}(s) either directly related or inferred members of given class
     */
    Flowable<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred);

    /**
     * Return data properties for a given individual
     *
     * @param individualIRI - IRI of individual to retrieve properties of
     * @param properties    - {@link Collection} of {@link OWLDataProperty} to retrieve for individual
     * @return - Set of OWLDataPropertyAssertionAxioms from given individual
     */
    Flowable<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, Collection<OWLDataProperty> properties);

    /**
     * Return all the data properties and values for a given individual
     *
     * @param individual - OWLNamedIndividual to get properties for
     * @param properties - {@link Collection} of {@link OWLDataProperty} to retrieve for individual
     * @return - Set of OWLDataPropertyAssertionAxioms from individual
     */
    Flowable<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, Collection<OWLDataProperty> properties);

    /**
     * Get all asserted properties for a given individual
     *
     * @param individualIRI - IRI of individual to get properties for
     * @return Set of OWLDataPropertyAssertionAxioms
     */
    Flowable<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI);

    /**
     * Get all asserted properties for a given individual
     *
     * @param individual - OWLNamedIndividual to get properties for
     * @return - Set of OWLDataPropertyAssertionAxioms
     */
    Flowable<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual);

    /**
     * Get all object properties for a given individual IRI
     *
     * @param individual - IRI of individual to retrieve properties for
     * @return - Set of OWLObjectPropertyAssertionAxioms for a given individual IRI
     */
    Flowable<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual);

    /**
     * Get all object properties for a given individual
     *
     * @param individual - OWLNamedIndividual to retrieve properties for
     * @return - Set of OWLObjectPropertyAssertionAxioms for a given individual
     */
    Flowable<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual);

    /**
     * Return a set of values for a given data property
     *
     * @param individual  - OWLNamedIndividual to retrieve properties for
     * @param propertyIRI - IRI of dataproperty to retrieve
     * @return - {@link Flowable} of {@link OWLLiteral} values for given property of specific individual
     */
    Flowable<OWLLiteral> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI);

    /**
     * Return a set of values for a given data property
     *
     * @param individualIRI - IRI of individual to retrieve properties for
     * @param property      - OWLDataProperty to retrieve values from
     * @return - {@link Flowable} of {@link OWLLiteral} values for given property of specific individual
     */
    Flowable<OWLLiteral> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property);

    /**
     * Return the set of data property values
     *
     * @param individual - OWLNamedIndividual to get properties from
     * @param property   - OWLDataProperty to access
     * @return - {@link Flowable} of {@link OWLLiteral} values for given property of specific individual
     */
    Flowable<OWLLiteral> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property);

    /**
     * Get all the related facts for an individual, valid at a specific valid/database point
     * If no temporals are specified, we retrieve the currently valid facts
     *
     * @param individual       - {@link OWLNamedIndividual} to get facts for
     * @param validTemporal    - Nullable {@link OffsetDateTime} representing valid-at temporal
     * @param databaseTemporal - Nullable {@link OffsetDateTime} representing database-at temporal
     * @param filterTemporals  - {@code true} remove temporals from the result set
     * @return - {@link Flowable} of {@link OWLDataPropertyAssertionAxiom} which represent all asserted Facts on the individual
     */
    Flowable<OWLDataPropertyAssertionAxiom> getFactsForIndividual(OWLNamedIndividual individual, OffsetDateTime validTemporal, OffsetDateTime databaseTemporal, boolean filterTemporals);

    /**
     * Get data properties for temporal from given individuals
     *
     * @param individual - Individual to retrieve temporal properties from
     * @return -{@link Flowable} of {@link OWLDataPropertyAssertionAxiom} representing temporal properties
     */
    Flowable<OWLDataPropertyAssertionAxiom> getTemporalsForIndividual(OWLNamedIndividual individual);

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
     *
     * @param owlNamedObject - OWL Object to extract IRI from
     * @return - IRI of OWL Object
     */
    IRI getFullIRI(OWLNamedObject owlNamedObject);

    /**
     * Get the full IRI expanded from the DefaultPrefixManager as a String
     *
     * @param owlNamedObject - OWL Object to extract IRI from
     * @return - String of OWL Object IRI
     */
    String getFullIRIString(OWLNamedObject owlNamedObject);

    /**
     * Excecute a raw SPARQL query against the ontology
     *
     * @param queryString - String representing SPARQL query
     * @return - {@link Flowable} of {@link TrestleResult} for given query
     */
    Flowable<TrestleResult> executeSPARQLResults(String queryString);

    /**
     * Execute a writing SPARQL query, without returning a {@link TrestleResultSet}
     *
     * @param queryString - SPARQL Query String
     * @return - {@link Completable} when finished
     */
    Completable executeUpdateSPARQL(String queryString);

    /**
     * Takes an existing transaction object and inherits from it
     *
     * @param transactionObject - Transaction Object to take ownership of thread transaction
     * @param write             - Writable transaction?
     * @return - Transaction Object passed in as argument
     */
    TrestleTransaction createandOpenNewTransaction(@Nullable TrestleTransaction transactionObject, boolean write);

    /**
     * Takes an existing transaction object and inherits from it
     *
     * @param transactionObject - Existing TrestleTransactionObject
     * @return - Transaction Object passed in as argument
     */
    TrestleTransaction createandOpenNewTransaction(@Nullable TrestleTransaction transactionObject);

    /**
     * Create a new {@link TrestleTransaction}
     *
     * @param write - {@code true} create write transaction. {@code false} create read-only transaction
     * @return - {@link TrestleTransaction}
     */
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
     *
     * @param transaction - Transaction object to try to abort current transaction with
     */
    void returnAndAbortTransaction(TrestleTransaction transaction);

    /**
     * Returns {@link TrestleTransaction} and forces the transaction to abort
     * This is a stop-gap solution to deal with the fact that sometimes quickly executed operations will fail to clear the thread transaction state
     * Shouldn't really be used, but maybe in a pinch
     *
     * @param trestleTransaction - {@link TrestleTransaction} to abort
     */
    void returnAndAbortWithForce(TrestleTransaction trestleTransaction);

    /**
     * Open a transaction and lock it
     *
     * @param write - Open writable transaction?
     */
    void openAndLock(boolean write);

    /**
     * Unlock the transaction and commit it
     *
     * @param write - Is this a write transaction?
     */
    void unlockAndCommit(boolean write);

    /**
     * Get the current number of opened transactions, for the lifetime of the application
     *
     * @return - long of opened transactions
     */
    long getOpenedTransactionCount();

    /**
     * Get the current number of committed transactions, for the lifetime of the application
     *
     * @return - long of committed transactions
     */
    long getCommittedTransactionCount();

    /**
     * Get the current number of aborted transactions, for the lifetime of the application
     *
     * @return - long of aborted transactions
     */
    long getAbortedTransactionCount();

    /**
     * Get the number of currently open read/write transactions
     *
     * @return - {@link AtomicInteger} int of open read/write transactions
     */
    int getCurrentlyOpenTransactions();

    /**
     * Get the number of currently open write transactions
     *
     * @return - {@link AtomicInteger} int of open write transactions
     */
    @Gauge(name = "trestle-open-write-transactions", absolute = true)
    int getOpenWriteTransactions();

    /**
     * Get the number of currently open read transactions
     *
     * @return - {@link AtomicInteger} int of open read transactions
     */
    @Gauge(name = "trestle-open-read-transactions", absolute = true)
    int getOpenReadTransactions();
}
