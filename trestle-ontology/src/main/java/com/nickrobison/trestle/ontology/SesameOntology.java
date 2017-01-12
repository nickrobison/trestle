package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.utils.RDF4JLiteralFactory.createLiteral;

public abstract class SesameOntology extends TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(SesameOntology.class);
    protected static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    protected final String ontologyName;
    protected final RepositoryConnection connection;
    protected final OWLOntology ontology;
    protected final DefaultPrefixManager pm;
    protected final OWLDataFactory df;


    SesameOntology(String ontologyName, RepositoryConnection connection, OWLOntology ontology, DefaultPrefixManager pm) {
        this.ontologyName = ontologyName;
        this.connection = connection;
        this.ontology = ontology;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
    }


    public abstract boolean isConsistent();

    @Override
    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return this.getIndividualObjectProperty(individual, df.getOWLObjectProperty(propertyIRI));
    }

    @Override
    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI) {
        return this.getIndividualObjectProperty(df.getOWLNamedIndividual(individualIRI),
                df.getOWLObjectProperty(objectPropertyIRI));
    }

    @Override
    public Optional<Set<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        Set<OWLObjectPropertyAssertionAxiom> properties = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = connection.getStatements(individualIRI, propertyIRI, null);
            try {
                while (statements.hasNext()) {
                    final Statement statement = statements.next();
                    final Value object = statement.getObject();
                    final OWLNamedIndividual propertyObject = df.getOWLNamedIndividual(object.stringValue());
                    properties.add(df.getOWLObjectPropertyAssertionAxiom(
                            property,
                            individual,
                            propertyObject));
                }
            } finally {
                statements.close();
            }
        } finally {
            this.commitTransaction(false);
        }

        if (properties.isEmpty()) {
            logger.error("Individual {} has no properties {}", individual.getIRI(), property.getIRI());
            return Optional.empty();
        }

        return Optional.of(properties);
    }

    @Override
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {
        this.openTransaction(true);
        logger.debug("Trying to create individual {}", owlClassAssertionAxiom.getIndividual());
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
        try {
            connection.add(individualIRI, RDF.TYPE, classIRI);
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void createIndividual(OWLNamedIndividual individual, OWLClass owlClass) {
        this.createIndividual(df.getOWLClassAssertionAxiom(owlClass, individual));
    }

    @Override
    public void createIndividual(IRI individualIRI, IRI classIRI) {
        this.createIndividual(
                df.getOWLClassAssertionAxiom(
                        df.getOWLClass(classIRI),
                        df.getOWLNamedIndividual(individualIRI)));
    }

    @Override
    public void associateOWLClass(OWLClass subClass, OWLClass superClass) {
        associateOWLClass(df.getOWLSubClassOfAxiom(subClass, superClass));
    }

    @Override
    public void associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom) {
        final org.eclipse.rdf4j.model.IRI subClassIRI = vf.createIRI(getFullIRIString(subClassOfAxiom.getSubClass().asOWLClass()));
        final org.eclipse.rdf4j.model.IRI superClassIRI = vf.createIRI(getFullIRIString(subClassOfAxiom.getSuperClass().asOWLClass()));
        this.openTransaction(true);
        try {
            connection.add(subClassIRI, RDFS.SUBCLASSOF, superClassIRI);
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void createProperty(OWLProperty property) {
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(true);
        try {
            if (property.isOWLDataProperty()) {
                connection.add(propertyIRI, RDF.TYPE, OWL.DATATYPEPROPERTY);
            } else if (property.isOWLObjectProperty()) {
                connection.add(propertyIRI, RDF.TYPE, OWL.OBJECTPROPERTY);
            }
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI) throws MissingOntologyEntity {
        writeIndividualDataProperty(
                df.getOWLDataPropertyAssertionAxiom(
                        df.getOWLDataProperty(dataPropertyIRI),
                        df.getOWLNamedIndividual(individualIRI),
                        df.getOWLLiteral(
                                owlLiteralString,
                                df.getOWLDatatype(owlLiteralIRI))));
    }

    @Override
    public void writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value) throws MissingOntologyEntity {
        writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(property, individual, value));
    }

    @Override
    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {
        final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(dataProperty.getSubject().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(dataProperty.getProperty().asOWLDataProperty()));

        this.openTransaction(true);
        try {
            connection.add(subjectIRI, propertyIRI, createLiteral(dataProperty.getObject()));
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void writeIndividualObjectProperty(OWLNamedIndividual owlSubject, IRI propertyIRI, OWLNamedIndividual owlObject) throws MissingOntologyEntity {
        writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty(propertyIRI),
                owlSubject,
                owlObject));
    }

    @Override
    public void writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject) throws MissingOntologyEntity {
        writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty(owlProperty),
                df.getOWLNamedIndividual(owlSubject),
                df.getOWLNamedIndividual(owlObject)));
    }

    @Override
    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {
        final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(property.getSubject().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI objectIRI = vf.createIRI(getFullIRIString(property.getObject().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property.getProperty().asOWLObjectProperty()));
        this.openTransaction(true);
        try {
            connection.add(subjectIRI, propertyIRI, objectIRI);
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void removeIndividual(OWLNamedIndividual individual) {

    }

    @Override
    public boolean containsResource(IRI individualIRI) {
        return false;
    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {
        return false;
    }

    @Override
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    public abstract void close(boolean drop);

    @Override
    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    @Override
    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    public abstract void runInference();

    @Override
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, List<OWLDataProperty> properties) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, List<OWLDataProperty> properties) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual) {
        return null;
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return null;
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property) {
        return null;
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetFactsForIndividual(OWLNamedIndividual individual, @Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal) {
        return null;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetTemporalsForIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public IRI getFullIRI(IRI iri) {
        //        Check to see if it's already been expanded
        if (pm.getPrefix(iri.getScheme() + ":") == null) {
            return iri;
        } else {
            return pm.getIRI(iri.toString());
        }
    }

    @Override
    public IRI getFullIRI(String prefix, String suffix) {
        return getFullIRI(IRI.create(prefix, suffix));
    }

    public abstract void initializeOntology();

    @Override
    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    @Override
    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    public abstract TrestleResultSet executeSPARQLTRS(String queryString);

    public abstract void openDatasetTransaction(boolean write);

    public abstract void commitDatasetTransaction(boolean write);
}