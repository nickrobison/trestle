//package com.nickrobison.trestle.ontology;
//
//import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
//import org.apache.jena.query.ResultSet;
//import org.openrdf.model.impl.TreeModel;
//import org.openrdf.repository.config.RepositoryConfigSchema;
//import org.openrdf.repository.manager.LocalRepositoryManager;
//import org.semanticweb.owlapi.model.*;
//import org.semanticweb.owlapi.util.DefaultPrefixManager;
//
//import java.io.File;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
///**
// * Created by nrobison on 7/18/16.
// */
//// FIXME(nrobison): Doesn't work at all. Completely and utterly broken
//public class SesameOntology extends TransactingOntology {
//
//
//    SesameOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
//        final LocalRepositoryManager localRepositoryManager = new LocalRepositoryManager(new File("./target/data"));
//        localRepositoryManager.initialize();
//
//        final TreeModel graph = new TreeModel();
//        RepositoryConfigSchema.NAMESPACE
//
//    }
//
//    @Override
//    public boolean isConsistent() {
//        return false;
//    }
//
//    @Override
//    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI) {
//        return null;
//    }
//
//    @Override
//    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI) {
//        return null;
//    }
//
//    @Override
//    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
//        return null;
//    }
//
//    @Override
//    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {
//
//    }
//
//    @Override
//    public void createIndividual(OWLNamedIndividual individual, OWLClass owlClass) {
//
//    }
//
//    @Override
//    public void createIndividual(IRI individualIRI, IRI classIRI) {
//
//    }
//
//    @Override
//    public void associateOWLClass(OWLClass subClass, OWLClass superClass) {
//
//    }
//
//    @Override
//    public void associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom) {
//
//    }
//
//    @Override
//    public void createProperty(OWLProperty property) {
//
//    }
//
//    @Override
//    public void writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public void writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public void writeIndividualObjectProperty(OWLNamedIndividual owlSubject, IRI propertyIRI, OWLNamedIndividual owlObject) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public void writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {
//
//    }
//
//    @Override
//    public boolean containsResource(IRI individualIRI) {
//        return false;
//    }
//
//    @Override
//    public boolean containsResource(OWLNamedObject individual) {
//        return false;
//    }
//
//    @Override
//    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
//
//    }
//
//    @Override
//    public void close(boolean drop) {
//
//    }
//
//    @Override
//    public OWLOntology getUnderlyingOntology() {
//        return null;
//    }
//
//    @Override
//    public DefaultPrefixManager getUnderlyingPrefixManager() {
//        return null;
//    }
//
//    @Override
//    public void runInference() {
//
//    }
//
//    @Override
//    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, List<OWLDataProperty> properties) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, List<OWLDataProperty> properties) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual) {
//        return null;
//    }
//
//    @Override
//    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual) {
//        return null;
//    }
//
//    @Override
//    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
//        return null;
//    }
//
//    @Override
//    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI) {
//        return null;
//    }
//
//    @Override
//    public Optional<Set<OWLLiteral>> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property) {
//        return null;
//    }
//
//    @Override
//    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property) {
//        return null;
//    }
//
//    @Override
//    public IRI getFullIRI(IRI iri) {
//        return null;
//    }
//
//    @Override
//    public IRI getFullIRI(String prefix, String suffix) {
//        return null;
//    }
//
//    @Override
//    public void initializeOntology() {
//
//    }
//
//    @Override
//    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
//        return null;
//    }
//
//    @Override
//    public String getFullIRIString(OWLNamedObject owlNamedObject) {
//        return null;
//    }
//
//    @Override
//    public ResultSet executeSPARQL(String query) {
//        return null;
//    }
//
//    @Override
//    public void openDatasetTransaction(boolean write) {
//
//    }
//
//    @Override
//    public void commitDatasetTransaction(boolean write) {
//
//    }
//}