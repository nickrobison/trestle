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
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
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
    private final RepositoryConnection connection;
    private final ValueFactory vf;
    private final KiWiTransactionalSail tsail;
    private final KiWiStore store;

    SesameOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        this.ontologyName = name;
        this.ontology = ont;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
        KiWiDialect dialect = new PostgreSQLDialect();
        final KiWiConfiguration kiWiConfiguration = new KiWiConfiguration(name, connectionString, username, password, dialect);
        store = new KiWiStore(kiWiConfiguration);
        tsail = new KiWiTransactionalSail(store);
        KiWiReasoningSail rsail = new KiWiReasoningSail(tsail, new ReasoningConfiguration());

//
//        Sail wsail = new SailWrapper(rsail) {
//
//            @Override
//            public void shutDown() throws SailException {
//                rsail.getEngine().shutdown(true);
//
//                try {
//                    rsail.getPersistence().dropDatabase();
//                    rsail.getPersistence().dropDatabase();
//                } catch (SQLException e) {
//                    logger.error("Cannot drop database", e);
//                }
//
//                super.shutDown();
//            }
//        };
//
////        We should probably figure out a better way to do this. Apparently, this is really slow
////        Also not sure if we can just cast it to and rdf4j sail, I think so, but we'll see.
////        repository = new SailRepository(new ForwardChainingRDFSInferencer(baseSail));
//        repository = new RepositoryWrapper(new SailRepository(wsail)) {
//
//            @Override
//            public RepositoryConnection getConnection() throws RepositoryException {
//                return new RepositoryConnectionWrapper(this, super.getConnection()) {
//
//                    @Override
//                    public void commit() throws RepositoryException {
//                        super.commit();
//
//                        try {
//                            while(rsail.getEngine().isRunning()) {
//                                logger.info("sleeping for 500ms to let engine finish processing ... ");
//                                Thread.sleep(500);
//                            }
//                            logger.info("sleeping for 100ms to let engine finish processing ... ");
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                            throw new RepositoryException("Could not finish reasoning", e);
//                        }
//                    }
//                };
//            }
//        };
        repository = new SailRepository(rsail);
        repository.initialize();
        connection = repository.getConnection();
        vf = repository.getValueFactory();

    }
    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        return Optional.empty();
    }

    @Override
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {

    }

    @Override
    public void createProperty(OWLProperty property) {

    }

    @Override
    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {

    }

    @Override
    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {

    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {
        return false;
    }

    @Override
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    @Override
    public void applyChange(OWLAxiomChange... axiom) {

    }

    @Override
    public void close(boolean drop) {
        logger.info("Shutting down repository {}", ontologyName);
        connection.close();
        repository.shutDown();
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
    public void openTransaction() {
        logger.info("Opening repository transaction");
        this.connection.begin();
    }

    @Override
    public void commitTransaction() {
        logger.info("Committing repository transaction");
        this.connection.commit();
    }

    @Override
    @SuppressWarnings("argument.type.incompatible")
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        Set<OWLNamedIndividual> instances = new HashSet<>();
        final org.openrdf.model.IRI resource = vf.createIRI(getFullIRIString(owlClass));
//        pretty sure this doesn't work.
        final RepositoryResult<Statement> statements = connection.getStatements(resource, RDF.TYPE, null, direct);
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            instances.add(df.getOWLNamedIndividual(IRI.create(statement.getSubject().toString())));
        }
        statements.close();
        return instances;
    }

    @Override
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        return Optional.empty();
    }

    @Override
//    TODO(nrobison): Make this work
    @SuppressWarnings("argument.type.incompatible")
    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {

        final org.openrdf.model.IRI modelResource = vf.createIRI(getFullIRIString(individual));
        final org.openrdf.model.IRI modelProperty = vf.createIRI(getFullIRIString(property));
        Set<OWLLiteral> properties = new HashSet<>();
        final RepositoryResult<Statement> statements = connection.getStatements(modelResource, modelProperty, null, true);
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            final Value object = statement.getObject();
            final Literal literal = vf.createLiteral("test");
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
        logger.info("Dropping Sesame repository {}", ontologyName);
        try {
            store.getPersistence().dropDatabase();
        } catch (SQLException e) {
            logger.error("Cannot drop sesame repository", e);
        }
//        repository.initialize();

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

        this.openTransaction();
        try {
            connection.add(is, pm.getDefaultPrefix(), RDFFormat.RDFXML);
        } catch (IOException e) {
            logger.error("Cannot load ontology {}", ontologyName, e);
            throw new RuntimeException("Cannot load ontology: " + ontologyName, e);
        }
        this.commitTransaction();

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
}
