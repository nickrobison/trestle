package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.utils.RDF4JLiteralFactory;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.nickrobison.trestle.ontology.utils.ToDataPropertyAssertionAxiom.toDataPropertyAssertionAxiom;

@NotThreadSafe
// We have to suppress these warnings because Checker is garbage and won't allow us to mark a method has ensuring non-null. Because why would it?
// FIXME(nrobison): This has to go away!
@SuppressWarnings({"argument.type.incompatible"})
public abstract class RDF4JOntology extends TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(RDF4JOntology.class);
    protected final SimpleValueFactory vf;
    protected final String ontologyName;
    protected final RepositoryConnection adminConnection;
    protected final Repository repository;
    protected final OWLOntology ontology;
    protected final DefaultPrefixManager pm;
    protected final OWLDataFactory df;
    protected final QueryBuilder qb;
    protected final RDF4JLiteralFactory lf;

    protected ThreadLocal<@Nullable RepositoryConnection> tc = ThreadLocal.withInitial(() -> null);


    protected RDF4JOntology(String ontologyName, Repository repository, OWLOntology ontology, DefaultPrefixManager pm, RDF4JLiteralFactory factory) {
        super();
        this.ontologyName = ontologyName;
        this.repository = repository;
        this.adminConnection = repository.getConnection();
        this.ontology = ontology;
        this.pm = pm;
        this.df = factory.getDataFactory();
        this.vf = factory.getValueFactory();
        this.qb = new QueryBuilder(QueryBuilder.Dialect.SESAME, this.pm);
        this.lf = factory;
    }

    @Override
    public QueryBuilder getUnderlyingQueryBuilder() {
        return this.qb;
    }

    @Override
    public Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return this.getIndividualObjectProperty(individual, df.getOWLObjectProperty(propertyIRI));
    }

    @Override
    public Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(IRI individualIRI, IRI objectPropertyIRI) {
        return this.getIndividualObjectProperty(df.getOWLNamedIndividual(individualIRI),
                df.getOWLObjectProperty(objectPropertyIRI));
    }

    @Override
    public Flowable<OWLObjectPropertyAssertionAxiom> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = getThreadConnection().getStatements(individualIRI, propertyIRI, null);
            return Flowable.fromIterable(statements)
                    .map(statement -> {
                        final Value object = statement.getObject();
                        final OWLNamedIndividual propertyObject = df.getOWLNamedIndividual(object.stringValue());
                        return df.getOWLObjectPropertyAssertionAxiom(
                                property,
                                individual,
                                propertyObject);
                    })
                    .doOnComplete(() -> this.commitTransaction(false))
                    .doOnError(error -> this.unlockAndAbort(false))
                    .doFinally(statements::close);
        } catch (Exception e) {
            this.unlockAndAbort(false);
            throw e;
        }
    }

    @Override
    public Completable createIndividual(OWLNamedIndividual individual, OWLClass owlClass) {
        return this.createIndividual(df.getOWLClassAssertionAxiom(owlClass, individual));
    }

    @Override
    public Completable createIndividual(IRI individualIRI, IRI classIRI) {
        return this.createIndividual(
                df.getOWLClassAssertionAxiom(
                        df.getOWLClass(classIRI),
                        df.getOWLNamedIndividual(individualIRI)));
    }

    @Override
    public Completable createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            logger.debug("Trying to create individual {}", owlClassAssertionAxiom.getIndividual());
            final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
            final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
            getThreadConnection().add(individualIRI, RDF.TYPE, classIRI);
        })
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable associateOWLClass(OWLClass subClass, OWLClass superClass) {
        return associateOWLClass(df.getOWLSubClassOfAxiom(subClass, superClass));
    }

    @Override
    public Completable associateOWLClass(OWLSubClassOfAxiom subClassOfAxiom) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            final org.eclipse.rdf4j.model.IRI subClassIRI = vf.createIRI(getFullIRIString(subClassOfAxiom.getSubClass().asOWLClass()));
            final org.eclipse.rdf4j.model.IRI superClassIRI = vf.createIRI(getFullIRIString(subClassOfAxiom.getSuperClass().asOWLClass()));
            getThreadConnection().add(subClassIRI, RDFS.SUBCLASSOF, superClassIRI);
        })
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable createProperty(OWLProperty property) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
            if (property.isOWLDataProperty()) {
                getThreadConnection().add(propertyIRI, RDF.TYPE, OWL.DATATYPEPROPERTY);
            } else if (property.isOWLObjectProperty()) {
                getThreadConnection().add(propertyIRI, RDF.TYPE, OWL.OBJECTPROPERTY);
            }
        })
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable writeIndividualDataProperty(IRI individualIRI, IRI dataPropertyIRI, String owlLiteralString, IRI owlLiteralIRI) {
        return writeIndividualDataProperty(
                df.getOWLDataPropertyAssertionAxiom(
                        df.getOWLDataProperty(dataPropertyIRI),
                        df.getOWLNamedIndividual(individualIRI),
                        df.getOWLLiteral(
                                owlLiteralString,
                                df.getOWLDatatype(owlLiteralIRI))));
    }

    @Override
    public Completable writeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, OWLLiteral value) {
        return writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(property, individual, value));
    }

    @Override
    public Completable writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(dataProperty.getSubject().asOWLNamedIndividual()));
            final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(dataProperty.getProperty().asOWLDataProperty()));
            getThreadConnection().add(subjectIRI, propertyIRI, this.lf.createLiteral(dataProperty.getObject()));
        })
                .doOnComplete(() -> this.commitTransaction(true))
                .doOnError(error -> this.unlockAndAbort(true));
    }

    @Override
    public Completable writeIndividualObjectProperty(OWLNamedIndividual owlSubject, IRI propertyIRI, OWLNamedIndividual owlObject) {
        return writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty(propertyIRI),
                owlSubject,
                owlObject));
    }

    @Override
    public Completable writeIndividualObjectProperty(IRI owlSubject, IRI owlProperty, IRI owlObject) {
        return writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                df.getOWLObjectProperty(owlProperty),
                df.getOWLNamedIndividual(owlSubject),
                df.getOWLNamedIndividual(owlObject)));
    }

    @Override
    public Completable writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) {
        final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(property.getSubject().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI objectIRI = vf.createIRI(getFullIRIString(property.getObject().asOWLNamedIndividual()));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property.getProperty().asOWLObjectProperty()));
        this.openTransaction(true);
        return Completable.fromRunnable(() -> getThreadConnection().add(subjectIRI, propertyIRI, objectIRI))
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable removeIndividual(OWLNamedIndividual individual) {
        this.openTransaction(true);
        return Completable.fromRunnable(() -> {
            final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
            getThreadConnection().remove(individualIRI, null, null);
        })
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable removeIndividualObjectProperty(OWLNamedIndividual subject, OWLObjectProperty property, @Nullable OWLNamedIndividual object) {
        final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(subject));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property.getNamedProperty()));

        final org.eclipse.rdf4j.model.@Nullable IRI objectIRI;
        if (object == null) {
            objectIRI = null;
        } else {
            objectIRI = vf.createIRI(getFullIRIString(object));
        }
        this.openTransaction(true);
        return Completable.fromRunnable(() -> getThreadConnection().remove(subjectIRI, propertyIRI, objectIRI))
                .doOnError(error -> this.unlockAndAbort(true))
                .doOnComplete(() -> this.commitTransaction(true));
    }

    @Override
    public Completable removeIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty property, @Nullable OWLLiteral literal) {
        final org.eclipse.rdf4j.model.IRI subjectIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));

        final @Nullable Value literalValue;
        if (literal == null) {
            literalValue = null;
        } else {
            literalValue = this.lf.createLiteral(literal);
        }

        this.openTransaction(true);
        return Completable.fromRunnable(() -> getThreadConnection().remove(subjectIRI, propertyIRI, literalValue))
                .doOnComplete(() -> this.commitTransaction(true))
                .doOnError(error -> this.unlockAndAbort(true));
    }

    @Override
    public Single<Boolean> containsResource(IRI individualIRI) {
        return containsResource(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public Single<Boolean> containsResource(OWLNamedObject individual) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        this.openTransaction(false);
        return Single.fromCallable(() -> getThreadConnection().hasStatement(individualIRI, null, null, false))
                .doOnError(error -> this.unlockAndAbort(false))
                .doOnSuccess(contains -> this.commitTransaction(false));
    }

    @Override
    public void writeOntology(IRI path, boolean validate) {
        final OutputStream fso;
        try {
            fso = Files.newOutputStream(new File(path.toURI()).toPath());
        } catch (IOException e) {
            logger.error("Cannot open file path: {}", path, e);
            return;
        }
        final RDFWriter writer = Rio.createWriter(RDFFormat.RDFXML, fso);
        this.openTransaction(false);
        try {
            getThreadConnection().export(writer);
        } finally {
            this.commitTransaction(false);
        }
    }

    protected abstract void closeDatabase(boolean drop);

    @Override
    public void close(boolean drop) {
        this.adminConnection.close();
        repository.shutDown();
        this.closeDatabase(drop);
        logger.debug("Opened {} transactions, committed {}, aborted {}", this.getOpenedTransactionCount(), this.getCommittedTransactionCount(), this.getAbortedTransactionCount());
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
    public Flowable<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred) {
        Set<OWLNamedIndividual> instances = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(getFullIRIString(owlClass));
        this.openTransaction(false);
        final RepositoryResult<Statement> statements = getThreadConnection().getStatements(null, RDF.TYPE, classIRI);
        return Flowable.fromIterable(statements)
                .map(statement -> df.getOWLNamedIndividual(IRI.create(statement.getSubject().stringValue())))
                .doOnError(error -> this.unlockAndAbort(false))
                .doOnComplete(() -> this.commitTransaction(false))
                .doFinally(statements::close);
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(IRI individualIRI, Collection<OWLDataProperty> properties) {
        return this.getDataPropertiesForIndividual(df.getOWLNamedIndividual(individualIRI), properties);
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getDataPropertiesForIndividual(OWLNamedIndividual individual, Collection<OWLDataProperty> properties) {
        Set<OWLDataProperty> requestedProperties = new HashSet<>(properties);
        return getAllDataPropertiesForIndividual(individual)
                .filter(property -> requestedProperties.contains(property.getProperty().asOWLDataProperty()));
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
        return this.getAllDataPropertiesForIndividual(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));

        this.openTransaction(false);
        final RepositoryResult<Statement> result = getThreadConnection().getStatements(individualIRI, null, null);
        return Flowable.fromIterable(result)
                .filter(statement -> !statement.getPredicate().getNamespace().contains("rdf-syntax"))
                .filter(statement -> {
                    final Value object = statement.getObject();
                    return object instanceof Literal;
                })
                .toMultimap(statement -> statement.getPredicate().toString(), statement -> {
                    final Value object = statement.getObject();
                    return (Literal) object;
                })
                .flatMapPublisher(map -> Flowable.fromStream(map.entrySet().stream()))
                .flatMap(entry -> {
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(IRI.create(entry.getKey()));
                    return Flowable.fromIterable(entry.getValue())
                            .flatMap(literal -> {
                                final OWLLiteral owlLiteral = this.lf.createOWLLiteral(literal);
                                return Flowable.just(df.getOWLDataPropertyAssertionAxiom(
                                        owlDataProperty,
                                        individual,
                                        owlLiteral));
                            });
                })
                .doFinally(() -> {
                    result.close();
                    this.commitTransaction(false);
                });
    }

    @Override
    public Flowable<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(IRI individual) {
        return getAllObjectPropertiesForIndividual(df.getOWLNamedIndividual(individual));
    }

    @Override
    public Flowable<OWLObjectPropertyAssertionAxiom> getAllObjectPropertiesForIndividual(OWLNamedIndividual individual) {
        Set<OWLObjectPropertyAssertionAxiom> properties = new HashSet<>();
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        this.openTransaction(false);


        try {
            final RepositoryResult<Statement> statements = getThreadConnection().getStatements(individualIRI, null, null);
            return Flowable.fromIterable(statements)
                    .map(statement -> {
                        final Value object = statement.getObject();
                        final OWLObjectProperty property = df.getOWLObjectProperty(statement.getPredicate().toString());
                        final OWLNamedIndividual propertyObject = df.getOWLNamedIndividual(object.stringValue());
                        return df.getOWLObjectPropertyAssertionAxiom(
                                property,
                                individual,
                                propertyObject);
                    })
                    .doOnComplete(() -> this.commitTransaction(false))
                    .doOnError(error -> this.unlockAndAbort(false))
                    .doFinally(statements::close);
        } catch (Exception e) {
            this.unlockAndAbort(false);
            throw e;
        }
    }

    @Override
    public Flowable<OWLLiteral> getIndividualDataProperty(OWLNamedIndividual individual, IRI propertyIRI) {
        return getIndividualDataProperty(individual, df.getOWLDataProperty(propertyIRI));
    }

    @Override
    public Flowable<OWLLiteral> getIndividualDataProperty(IRI individualIRI, OWLDataProperty property) {
        return getIndividualDataProperty(df.getOWLNamedIndividual(individualIRI), property);
    }

    @Override
    public Flowable<OWLLiteral> getIndividualDataProperty(OWLNamedIndividual individual, OWLDataProperty
            property) {
        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(getFullIRIString(individual));
        final org.eclipse.rdf4j.model.IRI propertyIRI = vf.createIRI(getFullIRIString(property));
        this.openTransaction(false);
        try {
            final RepositoryResult<Statement> statements = getThreadConnection().getStatements(individualIRI, propertyIRI, null);
            return Flowable.fromIterable(statements)
                    .map(statement -> {
                        final Value object = statement.getObject();
                        return this.lf.createOWLLiteral((Literal) object);
                    })
                    .doOnComplete(() -> this.commitTransaction(false))
                    .doOnError(error -> this.unlockAndAbort(false))
                    .doFinally(statements::close);
        } catch (Exception e) {
            this.unlockAndAbort(false);
            throw e;
        }
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getFactsForIndividual(OWLNamedIndividual individual, OffsetDateTime
            validTemporal, OffsetDateTime databaseTemporal, boolean filterTemporals) {
        final String objectQuery = qb.buildObjectFactRetrievalQuery(validTemporal, databaseTemporal, true, null, individual);
        return dataPropertiesFromIndividual(individual, objectQuery);
    }

    @Override
    public Flowable<OWLDataPropertyAssertionAxiom> getTemporalsForIndividual(OWLNamedIndividual individual) {
        final String temporalQuery = this.qb.buildIndividualTemporalQuery(individual);
        return dataPropertiesFromIndividual(individual, temporalQuery);
    }

    private Flowable<OWLDataPropertyAssertionAxiom> dataPropertiesFromIndividual(OWLNamedIndividual individual, String query) {
        this.openTransaction(false);
        return this.executeSPARQLResults(query)
                .lift(toDataPropertyAssertionAxiom(this.df))
                .doOnError(error -> this.unlockAndAbort(false))
                .doOnComplete(() -> this.commitTransaction(false));
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

    @Override
    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    @Override
    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    /**
     * Convert an RDF4J {@link BindingSet} to our custom {@link TrestleResult}
     * @param bindingSet - {@link BindingSet} to convert
     * @return - {@link TrestleResult}
     */
    protected TrestleResult buildResult(BindingSet bindingSet) {
            final TrestleResult results = new TrestleResult();
            final Set<String> varNames = bindingSet.getBindingNames();
            varNames.forEach(varName -> {
                final Binding binding = bindingSet.getBinding(varName);
//                If the binding is null, value is unbound, so skip over it
                if (binding != null) {
                    final Value value = binding.getValue();
//                FIXME(nrobison): This is broken, figure out how to get the correct subtypes
                    if (value instanceof Literal) {
                        final OWLLiteral owlLiteral = this.lf.createOWLLiteral((Literal) value);
                        results.addValue(varName, owlLiteral);
                    } else {
                        results.addValue(varName, df.getOWLNamedIndividual(IRI.create(value.stringValue())));
                    }
                } else {
                    results.addValue(varName, null);
                }
            });
            return results;
    }

    /**
     * This is mostly here so that Checker will be quiet.
     * If we call {@link RDF4JOntology#setOntologyConnection()}, then the thread connection can never be null.
     * However, Checker can't seem to see through the call stack to notice that we've called that function, so we use this instead.
     *
     * @return - {@link RepositoryConnection} associated with the given transaction
     */
    protected RepositoryConnection getThreadConnection() {
        @Nullable final RepositoryConnection repositoryConnection = this.tc.get();
        if (repositoryConnection == null) {
            throw new IllegalStateException("Thread has null repository connection, did a transaction not get opened?");
        }
        return repositoryConnection;
    }

    @Override
    public abstract void openDatasetTransaction(boolean write);

    @Override
    public abstract void commitDatasetTransaction(boolean write);

    @Override
    public void setOntologyConnection() {
        logger.trace("Attempting to set thread connection");
        final TrestleTransaction threadTransactionObject = this.getThreadTransactionObject();
        if (threadTransactionObject == null) {
            logger.debug("Thread has no transaction object, getting connection from the pool");
            this.tc.set(this.repository.getConnection());
        } else {
            @Nullable final RepositoryConnection connection = threadTransactionObject.getConnection();
            logger.trace("Setting thread connection from transaction object {}", connection);
            this.tc.set(connection);
        }
    }

    /**
     * Reset thread connection to null
     */
    protected void resetThreadConnection() {
        logger.trace("Resetting thread connection");
        @Nullable final RepositoryConnection connection = getThreadConnection();
        if (connection != null) {
            connection.close();
        }
        this.tc.remove();
    }

    @Override
    public @NonNull RepositoryConnection getOntologyConnection() {
        final RepositoryConnection connection = this.repository.getConnection();
        logger.trace("Got ontology connection {}", connection);
        return connection;
    }
}