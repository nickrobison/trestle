package com.nickrobison.trestle.ontology;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.utils.JenaLiteralFactory;
import com.nickrobison.trestle.utils.SharedOntologyFunctions;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.Lock;
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Created by nrobison on 7/22/16.
 */
@SuppressWarnings("Duplicates")
@ThreadSafe
public abstract class JenaOntology extends TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(JenaOntology.class);

    protected final String ontologyName;
    protected final Model model;
    protected final Graph graph;
    protected final OWLOntology ontology;
    protected final DefaultPrefixManager pm;
    protected final OWLDataFactory df;
    protected final QueryBuilder qb;
    protected final JenaLiteralFactory jf;

    JenaOntology(String ontologyName, Model model, OWLOntology ontology, DefaultPrefixManager pm) {
        this.ontologyName = ontologyName;
        this.model = model;
        this.graph = this.model.getGraph();
        this.ontology = ontology;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
        this.qb = new QueryBuilder(QueryBuilder.DIALECT.JENA, this.pm);
        this.jf = new JenaLiteralFactory(this.model);
    }

    abstract public boolean isConsistent();

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
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource modelResource = model.getResource(getFullIRI(individual).toString());
            final Property modelProperty = model.getProperty(getFullIRI(property).toString());
            final StmtIterator stmtIterator = modelResource.listProperties(modelProperty);

            try {
                while (stmtIterator.hasNext()) {
                    final Statement statement = stmtIterator.nextStatement();
//            We need to check to make sure the returned statement points to a valid URI, otherwise it's just Jena junk.
                    if (statement.getObject().isURIResource()) {
                        final OWLNamedIndividual propertyObject = df.getOWLNamedIndividual(IRI.create(statement.getObject().asResource().getURI()));
                        final OWLObjectPropertyAssertionAxiom owlObjectProperty = df.getOWLObjectPropertyAssertionAxiom(property, individual, propertyObject);
//                final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getObject().toString()));
                        properties.add(owlObjectProperty);
                    } else {
                        logger.error("Model doesn't contain resource {}", statement.getObject());
                    }
                }
            } finally {
                stmtIterator.close();
            }
        } finally {
            model.leaveCriticalSection();
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

//        TODO(nrobison): This should be more optimistic, don't check, just write.
        this.openAndLock(true);
        logger.debug("Trying to create the individual");
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelResource = model.getResource(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
            final Resource modelClass = model.getResource(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
//            I think we can just ignore this and move on, try to add the assertion, it should simply move on if it already exists
//            if (model.contains(modelResource, RDF.type, modelClass)) {
//                logger.debug("{} already exists in the model", owlClassAssertionAxiom.getIndividual());
//                return;
//            }
            modelResource.addProperty(RDF.type, modelClass);

        } catch (TDBTransactionException e) {
            logger.error("Not in transaction", e);
        } finally {
            this.model.leaveCriticalSection();
            this.unlockAndCommit(true);
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

        final Resource modelSubclass;
        final Resource superClassResource;
        this.openAndLock(true);
        this.model.enterCriticalSection(Lock.WRITE);
        try {

            modelSubclass = model.getResource(getFullIRIString(subClassOfAxiom.getSubClass().asOWLClass()));
            superClassResource = model.getResource(getFullIRIString(subClassOfAxiom.getSuperClass().asOWLClass()));
            if (model.contains(modelSubclass, RDFS.subClassOf, superClassResource)) {
                logger.info("{} is already a subclass of {}", subClassOfAxiom.getSubClass().asOWLClass(), subClassOfAxiom.getSuperClass().asOWLClass());
                return;
            }
            modelSubclass.addProperty(RDFS.subClassOf, superClassResource);
        } finally {
            this.model.leaveCriticalSection();
            this.unlockAndCommit(true);
        }
    }

    @Override
    public void createProperty(OWLProperty property) {

        this.openTransaction(true);
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelResource = model.createResource(getFullIRIString(property));
            if (property.isOWLDataProperty()) {
                modelResource.addProperty(RDF.type, OWL.DatatypeProperty);
            } else if (property.isOWLObjectProperty()) {
                modelResource.addProperty(RDF.type, OWL.ObjectProperty);
            }
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
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

        this.openAndLock(true);
        this.model.enterCriticalSection(Lock.WRITE);
//        TODO(nrobison): Optimize for the happy path, no reason we should check for an existing individual or data property each time. Instead, just catch the exception once.
        try {
            //        Does the individual exist?
            final Resource modelResource = model.getResource(getFullIRIString(dataProperty.getSubject().asOWLNamedIndividual()));
            if (!model.containsResource(modelResource)) {
                throw new MissingOntologyEntity("missing class: ", dataProperty.getSubject());
            }

            final Property modelProperty = model.getProperty(getFullIRIString(dataProperty.getProperty().asOWLDataProperty()));
            if (!model.containsResource(modelProperty)) {
                createProperty(dataProperty.getProperty().asOWLDataProperty());
            }

            try {
                modelResource.addProperty(modelProperty,
                        jf.createLiteral(dataProperty.getObject()));
            } catch (AddDeniedException e) {
                logger.error("Problem adding property {} to individual {}",
                        dataProperty.getProperty().asOWLDataProperty().getIRI(),
                        dataProperty.getSubject().asOWLNamedIndividual().getIRI(),
                        e);
            }

            if (!modelResource.hasProperty(modelProperty)) {
                logger.error("Cannot set property {} on Individual {}",
                        dataProperty.getProperty().asOWLDataProperty().getIRI(),
                        dataProperty.getSubject().asOWLNamedIndividual().getIRI());
            }
        } finally {
            this.model.leaveCriticalSection();
            this.unlockAndCommit(true);
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
        this.openAndLock(true);
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelSubject = model.getResource(getFullIRIString(property.getSubject().asOWLNamedIndividual()));
            final Resource modelObject = model.getResource(getFullIRIString(property.getObject().asOWLNamedIndividual()));
            final Property modelProperty = model.getProperty(getFullIRIString(property.getProperty().asOWLObjectProperty()));

//        Check if the subject exists
            if (!model.containsResource(modelSubject)) {
                throw new MissingOntologyEntity("Missing subject: ", property.getSubject());
            }

//        Check if the object exists, or create
            if (!model.containsResource(modelProperty)) {
                createProperty(property.getProperty().asOWLObjectProperty());
            }

//        Check if the object exists
            if (!model.containsResource(modelObject)) {
                throw new MissingOntologyEntity("Missing object: ", property.getObject());
            }

            modelSubject.addProperty(modelProperty, modelObject);

            if (!modelSubject.hasProperty(modelProperty)) {
                logger.error("Cannot set property {} on Individual {}", property.getProperty().asOWLObjectProperty().getIRI(), property.getSubject().asOWLNamedIndividual().getIRI());
            }
        } finally {
            this.model.leaveCriticalSection();
            this.unlockAndCommit(true);
        }
    }

    @Override
    public void removeIndividual(OWLNamedIndividual individual) {
        this.openTransaction(true);
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelResource = this.model.getResource(getFullIRIString(individual));
            modelResource.removeProperties();
//            final StmtIterator stmtIterator = modelResource.listProperties();
//                try {
//                    logger.debug("Removing statements for {}", individual.getIRI());
//                    final List<Statement> statements = stmtIterator.toList();
//                    model.remove(statements);
//                } finally {
//                    stmtIterator.close();
//                }
//                Now, remove the individual
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(true);
        }
    }

    @Override
    public boolean containsResource(IRI individualIRI) {
        return containsResource(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {
        logger.debug("checking for resource {}", individual.getIRI());
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource resource = model.getResource(getFullIRIString(individual));
            return model.containsResource(resource);
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
    }

    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
        this.openAndLock(true);
        if (validate) {
            logger.info("Validating ontology before writing out");
            if (!this.isConsistent()) {
                logger.error("Ontology is inconsistent");
                throw new RuntimeException("Inconsistent ontology");
            }
        }

        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(new File(path.toURI()));
        } catch (FileNotFoundException e) {
            logger.error("Cannot open to file path", e);
            return;
        }
        logger.info("Writing ontology to {}", path);
        this.model.enterCriticalSection(Lock.READ);
        model.write(fileOutputStream, "RDF/XML");
        this.model.leaveCriticalSection();
        logger.debug("Finished writing ontology to {}", path);
        this.unlockAndCommit(true);
    }

    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

//    @Override
//    public void openTransaction(boolean write) {
//        logger.info("Opening model transaction");
//        model.begin();
//    }
//
//    @Override
//    public void commitTransaction() {
//        logger.info("Committing model transaction");
//        model.commit();
//    }

    //    TODO(nrobison): Implement this
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean inferred) {
        Set<OWLNamedIndividual> instances = new HashSet<>();
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource modelResource = model.getResource(getFullIRIString(owlClass));
            final ResIterator resIterator = model.listResourcesWithProperty(RDF.type, modelResource);
            try {
                while (resIterator.hasNext()) {
                    final Resource resource = resIterator.nextResource();
                    instances.add(df.getOWLNamedIndividual(IRI.create(resource.getURI())));
                }
            } finally {
                resIterator.close();
            }
        } finally {
            this.model.leaveCriticalSection();
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
        return SharedOntologyFunctions.filterIndividualDataProperties(properties, allDataPropertiesForIndividual);
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(IRI individualIRI) {
        return this.getAllDataPropertiesForIndividual(df.getOWLNamedIndividual(individualIRI));
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> getAllDataPropertiesForIndividual(OWLNamedIndividual individual) {
        Set<OWLDataPropertyAssertionAxiom> properties = new HashSet<>();
//        Map<String, Literal> statementLiterals = new HashMap<>();
        Multimap<String, Literal> statementLiterals = ArrayListMultimap.create();
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource modelResource = model.getResource(getFullIRIString(individual));
            final StmtIterator stmtIterator = modelResource.listProperties();
            try {
                while (stmtIterator.hasNext()) {
                    final Statement statement = stmtIterator.nextStatement();
//            Filter out RDF stuff
                    if (!statement.getPredicate().getNameSpace().contains("rdf-syntax")) {
//                Check to see if it's an object or data property
                        if (statement.getObject().isLiteral()) {
//                Check to see if property is object or data project
                            try {
//                                statement.getLiteral();
                                statementLiterals.put(statement.getPredicate().getURI(), statement.getLiteral());
                            } catch (Exception e) {
                                logger.debug("Can't get literal for {}", statement.getSubject(), e);
                            }
                        }
                    }
                }
            } finally {
                stmtIterator.close();
            }
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }

        statementLiterals
                .asMap()
                .entrySet()
                .forEach(entry -> {
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(IRI.create(entry.getKey()));
                    entry
                            .getValue().forEach(literal -> {
                        final Optional<OWLLiteral> owlLiteral = jf.createOWLLiteral(literal);
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
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource modelResource = model.getResource(getFullIRIString(individual));

            final StmtIterator stmtIterator = modelResource.listProperties();

            try {
                while (stmtIterator.hasNext()) {
                    final Statement statement = stmtIterator.nextStatement();

                    if (!statement.getPredicate().getNameSpace().contains("rdf-syntax")) {
//                Ensure that it's an object property
                        if (statement.getObject().isURIResource()) {
                            final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getPredicate().getURI()));
                            final OWLNamedIndividual objectIndividual = df.getOWLNamedIndividual(IRI.create(statement.getObject().asResource().getURI()));
                            final OWLObjectPropertyAssertionAxiom owlClassAssertionAxiom = df.getOWLObjectPropertyAssertionAxiom(owlObjectProperty,
                                    individual,
                                    objectIndividual);
                            properties.add(owlClassAssertionAxiom);
                        }
                    }

                }
            } finally {
                stmtIterator.close();
            }
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        return properties;
    }

    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {

        final Resource modelResource;
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            modelResource = model.getResource(getFullIRIString(individual));
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        if (modelResource == null) {
            return Optional.empty();
        }
        return Optional.of(individual);
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
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            final Resource modelResource = model.getResource(getFullIRIString(individual));
            final Property modelProperty = model.getProperty(getFullIRIString(property));

            if (modelProperty == null) {
                logger.error("Property {} doesn't exist on individual {}", property.getIRI(), individual.getIRI());
                return Optional.empty();
            }

            final StmtIterator stmtIterator = modelResource.listProperties(modelProperty);
//          Build and return the OWLLiteral
            try {
                while (stmtIterator.hasNext()) {
//                  If the URI is null, I think that means that it's just a string
                    final Statement statement = stmtIterator.nextStatement();
                    final Optional<OWLLiteral> parsedLiteral = jf.createOWLLiteral(statement.getLiteral());
                    parsedLiteral.ifPresent(properties::add);
                }
                if (properties.isEmpty()) {
                    logger.error("Individual {} has no properties {}", individual.getIRI(), property.getIRI());
                    this.commitTransaction(false);
                    return Optional.empty();
                }
            } finally {
                stmtIterator.close();
            }
        } finally {
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }

        return Optional.of(properties);
    }


    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetFactsForIndividual(OWLNamedIndividual individual, @Nullable OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal) {
        final String objectQuery = qb.buildObjectPropertyRetrievalQuery(startTemporal, endTemporal, individual);
//        Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new HashSet<>();
        final TrestleResultSet resultSet = this.executeSPARQLTRS(objectQuery);
//        final ResultSet resultSet = this.executeSPARQL(objectQuery);
        Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = SharedOntologyFunctions.getDataPropertiesFromIndividualFacts(df, resultSet);



//        while (resultSet.hasNext()) {
//            final QuerySolution next = resultSet.next();
//            final Optional<OWLLiteral> owlLiteral = this.jf.createOWLLiteral(next.getLiteral("object"));
//            owlLiteral.ifPresent(literal -> retrievedDataProperties.add(df.getOWLDataPropertyAssertionAxiom(
//                    df.getOWLDataProperty(next.getResource("property").getURI()),
//                    df.getOWLNamedIndividual(next.getResource("individual").getURI()),
//                    literal
//            )));
//        }
        return retrievedDataProperties;
    }

    @Override
    public Set<OWLDataPropertyAssertionAxiom> GetTemporalsForIndividual(OWLNamedIndividual individual) {
        final String temporalQuery = qb.buildIndividualTemporalQuery(individual);
//        Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new HashSet<>();
        final TrestleResultSet resultSet = this.executeSPARQLTRS(temporalQuery);
        return SharedOntologyFunctions.getDataPropertiesFromIndividualFacts(df, resultSet);
//        final ResultSet resultSet = this.executeSPARQL(temporalQuery);
//        while (resultSet.hasNext()) {
//            final QuerySolution next = resultSet.next();
//            final Optional<OWLLiteral> owlLiteral = this.jf.createOWLLiteral(next.getLiteral("object"));
//            owlLiteral.ifPresent(literal -> retrievedDataProperties.add(df.getOWLDataPropertyAssertionAxiom(
//                    df.getOWLDataProperty(next.getResource("property").getURI()),
//                    df.getOWLNamedIndividual(next.getResource("individual").getURI()),
//                    literal
//            )));
//        }
    }

    public IRI getFullIRI(IRI iri) {
        //        Check to see if it's already been expanded
        if (pm.getPrefix(iri.getScheme() + ":") == null) {
            return iri;
        } else {
            return pm.getIRI(iri.toString());
        }
    }

    public IRI getFullIRI(String prefix, String suffix) {
        return getFullIRI(IRI.create(prefix, suffix));
    }

    abstract public void initializeOntology();

    @Override
    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    @Override
    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    /**
     * Get the underlying model. Should only be use if really necessary, this skips all the transactions and concurency models that we've built.
     *
     * @return - Base Jena Model.
     */
    public Model getUnderlyingModel() {
        return this.model;
    }

    abstract public void close(boolean drop);

    /**
     * Parse boolean to correct Jena transaction
     *
     * @param writeTransaction - Boolean whether or not to open a writable transaction
     * @return - parsed boolean
     */
    protected static boolean getJenaLock(boolean writeTransaction) {
        if (writeTransaction) {
            return Lock.WRITE;
        }
        return Lock.READ;
    }

    /**
     * Build TrestleResultSet from Jena ResultSet
     * @param resultSet - Jena ResultSet to parse
     * @return - TrestleResultSet
     */
    TrestleResultSet buildResultSet(ResultSet resultSet) {
        final TrestleResultSet trestleResultSet = new TrestleResultSet(resultSet.getRowNumber());
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final TrestleResult results = new TrestleResult();
            final Iterator<String> varNames = next.varNames();
            while (varNames.hasNext()) {
                final String varName = varNames.next();
                final RDFNode rdfNode = next.get(varName);
                if (rdfNode.isResource()) {
                    results.addValue(varName, df.getOWLNamedIndividual(IRI.create(rdfNode.asResource().getURI())));
                } else {
                    final Optional<OWLLiteral> literal = this.jf.createOWLLiteral(rdfNode.asLiteral());
                    literal.ifPresent(literalValue -> results.addValue(varName, literalValue));
                }
            }
            trestleResultSet.addResult(results);
        }
        return trestleResultSet;
    }
}
