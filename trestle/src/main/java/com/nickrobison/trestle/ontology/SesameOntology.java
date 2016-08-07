package com.nickrobison.trestle.ontology;

import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.apache.marmotta.commons.sesame.transactions.sail.KiWiTransactionalSail;
import org.apache.marmotta.kiwi.config.KiWiConfiguration;
import org.apache.marmotta.kiwi.persistence.KiWiDialect;
import org.apache.marmotta.kiwi.persistence.pgsql.PostgreSQLDialect;
import org.apache.marmotta.kiwi.reasoner.engine.ReasoningConfiguration;
import org.apache.marmotta.kiwi.reasoner.sail.KiWiReasoningSail;
import org.apache.marmotta.kiwi.sail.KiWiStore;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 7/18/16.
 */
// FIXME(nrobison): Doesn't work at all. Completely and utterly broken
public class SesameOntology implements ITrestleOntology {

    private static final Logger logger = LoggerFactory.getLogger(SesameOntology.class);

    private final String ontologyName;
    private final OWLOntology ontology;
    private final DefaultPrefixManager pm;
    private final OWLDataFactory df;
    private final Repository repository;
    //    private final RepositoryConnection connection;
    private final ValueFactory vf;
    private final KiWiTransactionalSail tsail;
    private final KiWiStore store;
    private final KiWiReasoningSail rsail;

    SesameOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        this.ontologyName = name;
        this.ontology = ont;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
        KiWiDialect dialect = new PostgreSQLDialect();
        final KiWiConfiguration kiWiConfiguration = new KiWiConfiguration(name, connectionString, username, password, dialect);
        store = new KiWiStore(kiWiConfiguration);
        tsail = new KiWiTransactionalSail(store);
        rsail = new KiWiReasoningSail(tsail, new ReasoningConfiguration());

        repository = new SailRepository(new ForwardChainingRDFSInferencer(store));
//        I think it throws a NPE, but still seems to work. No idea if the inferencer runs or not
        try {
            repository.initialize();
        } catch (RepositoryException e) {
            logger.error("Problem initializing {}", ontologyName, e);
        } catch (NullPointerException e) {
            logger.error("Problem initializing {}, more NPEs", ontologyName, e);
        }

        vf = repository.getValueFactory();

    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI) {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("argument.type.incompatible")
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {

        RepositoryConnection connection;
        try {
            connection = repository.getConnection();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        final URI modelResource = vf.createURI(getFullIRIString(individual));
        final URI modelProperty = vf.createURI(getFullIRIString(property));

        Set<OWLObjectProperty> properties = new HashSet<>();
        final RepositoryResult<Statement> statements;
        try {
            statements = connection.getStatements(modelResource, modelProperty, null, true);
        } catch (RepositoryException e) {
            logger.error("Cannot get statements for {}", individual, e);
            return Optional.empty();
        }

        try {
            while (statements.hasNext()) {
                final Statement statement = statements.next();
                properties.add(df.getOWLObjectProperty(IRI.create(statement.getObject().toString())));
            }
        } catch (RepositoryException e) {
            logger.error("Cannot close statement", e);
        } finally {
            try {
                statements.close();
                connection.close();
            } catch (RepositoryException e) {
                logger.error("Cannot close statement", e);
            }
        }

        if (properties.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(properties);
    }

    @Override
    //    TODO(nrobison): Finish
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {

    }

    @Override
//    TODO(nrobison): Finish
    public void createIndividual(OWLNamedIndividual individual, OWLClass owlClass) {

    }

    @Override
    //    TODO(nrobison): Finish
    public void createIndividual(IRI individualIRI, IRI classIRI) {

    }

    @Override
//    TODO(nrobison): Finish
    public void associateOWLClass(OWLClass subClass, OWLClass superClass) {

    }

    @Override
    //    TODO(nrobison): Finish
    public void associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom) {

    }

    @Override
    //    TODO(nrobison): Finish
    public void createProperty(OWLProperty property) {

    }

    @Override
    //    TODO(nrobison): Finish
    public void writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI) throws MissingOntologyEntity {

    }

    @Override
    //    TODO(nrobison): Finish
    public void writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value) throws MissingOntologyEntity {

    }

    @Override
    //    TODO(nrobison): Finish
    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {

    }

    @Override
    //    TODO(nrobison): Finish
    public void writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject) {

    }

    @Override
    //    TODO(nrobison): Finish
    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {

    }

    @Override
    //    TODO(nrobison): Finish
    public boolean containsResource(IRI individualIRI) {
        return false;
    }

    @Override
    //    TODO(nrobison): Finish
    public boolean containsResource(OWLNamedObject individual) {
        return false;
    }

    @Override
    //    TODO(nrobison): Finish
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    @Override
    public void close(boolean drop) {
        logger.info("Shutting down repository {}", ontologyName);
        try {
//            connection.close();
            repository.shutDown();
        } catch (RepositoryException e) {
            logger.error("Cannot close repository {}", ontologyName, e);
        }

        if (drop) {
            try {
                store.getPersistence().dropDatabase();
            } catch (SQLException e) {
                logger.error("Cannot drop database {}", ontologyName, e);
            }
        }
    }

    @Override
    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    @Override
    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    @Override
    public void openTransaction(boolean write) {
        logger.info("Opening repository transaction");
        try {
            repository.getConnection().begin();
        } catch (RepositoryException e) {
            logger.error("Could not open transaction", e);
            throw new RuntimeException("Could not open transaction", e);
        }
    }

    @Override
    public void commitTransaction() {
        logger.info("Committing repository transaction");
        try {
            repository.getConnection().commit();
        } catch (RepositoryException e) {
            logger.error("Could not commit transaction", e);
        }
    }

    @Override
    @SuppressWarnings("argument.type.incompatible")
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred) {
        RepositoryConnection connection;
        try {
            connection = repository.getConnection();
        } catch (RepositoryException e) {
            logger.error("Cannot get connection", e);
            throw new RuntimeException("Cannot get connection", e);
        }

        Set<OWLNamedIndividual> instances = new HashSet<>();
        final URI resource = vf.createURI(getFullIRIString(owlClass));
//        pretty sure this doesn't work.
        final RepositoryResult<Statement> statements;
        try {
            statements = connection.getStatements(resource, RDF.TYPE, null, inferred);
        } catch (RepositoryException e) {
            logger.error("Cannot get statements for {}", owlClass, e);
            return new HashSet<>();
        }
        try {
            while (statements.hasNext()) {
                final Statement statement = statements.next();
                instances.add(df.getOWLNamedIndividual(IRI.create(statement.getSubject().toString())));
            }
        } catch (RepositoryException e) {
            logger.error("Problem with statement", e);
        } finally {
            try {
                statements.close();
                connection.close();
            } catch (RepositoryException e) {
                logger.error("Cannot close statement", e);
            }
        }
        return instances;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, List<OWLDataProperty> properties) {
        return new HashSet<>();
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, List<OWLDataProperty> properties) {
        return new HashSet<>();
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
        return new HashSet<>();
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
        return new HashSet<>();
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual) {
        return new HashSet<>();
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual) {
        return new HashSet<>();
    }

    @Override
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        return Optional.empty();
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property) {
        return Optional.empty();
    }

    @Override
//    TODO(nrobison): Make this work
    @SuppressWarnings("argument.type.incompatible")
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property) {

        RepositoryConnection connection;
        try {
            connection = repository.getConnection();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        final URI modelResource = vf.createURI(getFullIRIString(individual));
        final URI modelProperty = vf.createURI(getFullIRIString(property));
        Set<OWLLiteral> properties = new HashSet<>();
        final RepositoryResult<Statement> statements;
        try {
            statements = connection.getStatements(modelResource, modelProperty, null, true);
        } catch (RepositoryException e) {
            logger.error("Cannot get statements for {}", individual, e);
            return Optional.empty();
        }
        try {
            while (statements.hasNext()) {
                final Statement statement = statements.next();
                final Literal object = (Literal) statement.getObject();
                final OWLDatatype owlDatatype = df.getOWLDatatype(IRI.create(object.getDatatype().toString()));
                properties.add(df.getOWLLiteral(object.stringValue(), owlDatatype));
            }
        } catch (RepositoryException e) {
            logger.error("Problem with statement", e);
        } finally {
            try {
                statements.close();
                connection.close();
            } catch (RepositoryException e) {
                logger.error("Cannot close statement", e);
            }
        }

        if (properties.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(properties);
    }

    @Override
    public IRI getFullIRI(IRI iri) {

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

    @Override
    public void initializeOntology() {

        RepositoryConnection connection;
        try {
            connection = repository.getConnection();
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot open connection", e);
        }
        logger.info("Dropping Sesame repository {}", ontologyName);
        try {
            store.getPersistence().dropDatabase();
        } catch (SQLException e) {
            logger.error("Cannot drop sesame repository", e);
        }

        logger.debug("Writing out the ontology to byte array");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
//            Jena doesn't support OWL/XML, so we need base RDF.
            ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Cannot save ontology to bytearray", e);
        }

        final ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
        logger.debug("Reading model from byte stream");
        logger.info("Creating new model {}", ontologyName);

        try {
            connection.begin();
        } catch (RepositoryException e) {
            logger.error("Cannot begin transaction", e);
        }
        try {
            connection.add(is, pm.getDefaultPrefix(), RDFFormat.RDFXML);
        } catch (Exception e) {
            logger.error("Cannot load ontology {}", ontologyName, e);
            throw new RuntimeException("Cannot load ontology: " + ontologyName, e);
        }

        try {
            connection.commit();
            connection.close();
        } catch (RepositoryException e) {
            logger.error("Cannot commit transaction", e);
        }
    }

    @Override
    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    @Override
    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    @Override
//    FIXME(nrobison): This needs to be abstracted better. Sesame results are different
    @SuppressWarnings("return.type.incompatible")
    public ResultSet executeSPARQL(String query) {
        return null;
    }

    @Override
    public void lock() {

    }

    @Override
    public void openAndLock(boolean write) {

    }

    @Override
    public void unlock() {

    }

    @Override
    public void unlockAndCommit() {

    }
}
