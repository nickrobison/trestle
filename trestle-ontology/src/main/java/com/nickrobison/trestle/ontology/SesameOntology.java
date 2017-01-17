package com.nickrobison.trestle.ontology;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.utils.SesameConnectionManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.OffsetDateTime;
import java.util.*;

import static com.nickrobison.trestle.utils.RDF4JLiteralFactory.createLiteral;
import static com.nickrobison.trestle.utils.RDF4JLiteralFactory.createOWLLiteral;
import static com.nickrobison.trestle.utils.SharedOntologyFunctions.filterIndividualDataProperties;
import static com.nickrobison.trestle.utils.SharedOntologyFunctions.getDataPropertiesFromIndividualFacts;

@NotThreadSafe
public abstract class SesameOntology extends TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(SesameOntology.class);
    protected static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    protected final String ontologyName;
    protected final RepositoryConnection adminConnection;
    protected final Repository repository;
    protected final OWLOntology ontology;
    protected final DefaultPrefixManager pm;
    protected final OWLDataFactory df;
    protected final QueryBuilder qb;
    private final SesameConnectionManager cm;

    protected ThreadLocal<@Nullable RepositoryConnection> tc = ThreadLocal.withInitial(() -> null);


    SesameOntology(String ontologyName, Repository repository, OWLOntology ontology, DefaultPrefixManager pm) {
        this.ontologyName = ontologyName;
        this.repository = repository;
        this.adminConnection = repository.getConnection();
        this.ontology = ontology;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
        this.qb = new QueryBuilder(QueryBuilder.DIALECT.SESAME, this.pm);
        this.cm = new SesameConnectionManager(this.repository, 10, 2);
    }


    public abstract boolean isConsistent();

    @Override
    public Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return this.getIndividualObjectProperty(individual, df.getOWLObjectProperty(propertyIRI));
    }

    @Override
    public Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI) {
        return this.getIndividualObjectProperty(df.getOWLNamedIndividual(individualIRI),
                df.getOWLObjectProperty(objectPropertyIRI));
    }

    @Override
    public Optional<List<OWLObjectPropertyAssertionAxiom>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        List<OWLObjectPropertyAssertionAxiom> properties = new ArrayList<>();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(individualIRI, propertyIRI, null);
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
            this.tc.get().add(individualIRI, RDF.TYPE, classIRI);
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
            this.tc.get().add(subClassIRI, RDFS.SUBCLASSOF, superClassIRI);
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
                this.tc.get().add(propertyIRI, RDF.TYPE, OWL.DATATYPEPROPERTY);
            } else if (property.isOWLObjectProperty()) {
                this.tc.get().add(propertyIRI, RDF.TYPE, OWL.OBJECTPROPERTY);
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
            this.tc.get().add(subjectIRI, propertyIRI, createLiteral(dataProperty.getObject()));
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
            this.tc.get().add(subjectIRI, propertyIRI, objectIRI);
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public void removeIndividual(OWLNamedIndividual individual) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        this.openTransaction(true);
        try {
            this.tc.get().remove(individualIRI, null, null);
        } finally {
            this.commitTransaction(true);
        }
    }

    @Override
    public boolean containsResource(IRI individualIRI) {
        return containsResource(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(individualIRI, null, null);
            if (statements.hasNext()) {
                statements.close();
                return true;
            } else {
                statements.close();
                return false;
            }
        } finally {
            this.commitTransaction(false);
        }

    }

    @Override
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
        logger.error("Write ontology not-implemented for Sesame Ontology");
    }

    protected abstract void closeDatabase(boolean drop);
    @Override
    public void close(boolean drop) {
        this.adminConnection.close();
        repository.shutDown();
        this.closeDatabase(drop);
        logger.debug("Opened {} transactions, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

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
        Set<OWLNamedIndividual> instances = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(getFullIRIString(owlClass));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(null, RDF.TYPE, classIRI);
            try {
                while (statements.hasNext()) {
                    final Statement next = statements.next();
                    instances.add(df.getOWLNamedIndividual(IRI.create(next.getSubject().stringValue())));
                }
            } finally {
                statements.close();
            }
        } finally {
            this.commitTransaction(false);
        }
        return instances;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, List<OWLDataProperty> properties) {
        return this.getDataPropertiesForIndividual(df.getOWLNamedIndividual(individualIRI), properties);
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, List<OWLDataProperty> properties) {

        final Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = getAllDataPropertiesForIndividual(individual);
        logger.debug("Requested {} properties, returned {}", properties.size(), allDataPropertiesForIndividual.size());
        return filterIndividualDataProperties(properties, allDataPropertiesForIndividual);
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
        return this.getAllDataPropertiesForIndividual(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
        Set<OWLDataPropertyAssertionAxiom> properties = new HashSet<>();
        Multimap<String, Literal> statementLiterals = ArrayListMultimap.create();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));

        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(individualIRI, null, null);
            try {

                while (statements.hasNext()) {
                    final Statement statement = statements.next();
                    if (!statement.getPredicate().getNamespace().contains("rdf-syntax")) {
                        final Value object = statement.getObject();
                        if (object instanceof Literal) {
                            statementLiterals.put(statement.getPredicate().toString(), Literal.class.cast(object));
                        }
                    }
                }
            } finally {
                statements.close();
            }
        } finally {
            this.commitTransaction(false);
        }

        statementLiterals
                .asMap()
                .entrySet()
                .forEach(entry -> {
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(IRI.create(entry.getKey()));
                    entry
                            .getValue().forEach(literal -> {
                        final Optional<OWLLiteral> owlLiteral = createOWLLiteral(literal);
                        owlLiteral.ifPresent(owlLiteral1 -> properties.add(df.getOWLDataPropertyAssertionAxiom(
                                owlDataProperty,
                                individual,
                                owlLiteral1)));
                    });
                });

        return properties;
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual) {
        return getAllObjectPropertiesForIndividual(df.getOWLNamedIndividual(individual));
    }

    @Override
    public Set<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual) {
        Set<OWLObjectPropertyAssertionAxiom> properties = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(individualIRI, null, null);
            try {
                while (statements.hasNext()) {
                    final Statement statement = statements.next();

                    if (!statement.getPredicate().getNamespace().contains("rdf-syntax")) {
                        final Value object = statement.getObject();
//                        FIXME(nrobison): Implement this
                        if (object instanceof Literal) {
                            final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getPredicate().toString()));
                            final OWLNamedIndividual objectIndividual = df.getOWLNamedIndividual(IRI.create(statement.getObject().stringValue()));
                            properties.add(df.getOWLObjectPropertyAssertionAxiom(
                                    owlObjectProperty,
                                    individual,
                                    objectIndividual));
                        }
                    }
                }
            } finally {
                statements.close();
            }
        } finally {
            this.commitTransaction(false);
        }
        return properties;
    }

    @Override
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        logger.error("Get Individual unimplemented on SesameOntology");
        return Optional.empty();
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return getIndividualDataProperty(individual, df.getOWLDataProperty(propertyIRI));
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property) {
        return getIndividualDataProperty(df.getOWLNamedIndividual(individualIRI), property);
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property) {
        Set<OWLLiteral> properties = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = this.tc.get().getStatements(individualIRI, propertyIRI, null);
            try {
                while (statements.hasNext()) {
                    final Statement next = statements.next();
                    final Value object = next.getObject();
                    if (object instanceof Literal) {
                        createOWLLiteral(Literal.class.cast(object)).ifPresent(properties::add);
                    } else {
                        logger.warn("{} on {} is not a literal", next.getPredicate(), individual);
                    }
                }
            } finally {
                statements.close();
            }
        } finally {
            this.commitTransaction(false);
        }

        if (properties.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(properties);
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetFactsForIndividual(OWLNamedIndividual individual, @Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal) {
        final String objectQuery = qb.buildObjectPropertyRetrievalQuery(startTemporal, endTemporal, individual);
        final TrestleResultSet resultSet = this.executeSPARQLTRS(objectQuery);
        return getDataPropertiesFromIndividualFacts(this.df, resultSet);
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetTemporalsForIndividual(OWLNamedIndividual individual) {
        final String temporalQuery = this.qb.buildIndividualTemporalQuery(individual);
        final TrestleResultSet resultSet = this.executeSPARQLTRS(temporalQuery);
        return getDataPropertiesFromIndividualFacts(this.df, resultSet);
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

    @Override
    public void setOntologyConnection(RepositoryConnection connection) {
        this.tc.set(this.getThreadTransactionObject().getConnection());
    }

    @Override
    public @Nullable RepositoryConnection getOntologyConnection() {
        return this.cm.getConnection();
    }
}