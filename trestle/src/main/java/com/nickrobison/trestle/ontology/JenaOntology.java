package com.nickrobison.trestle.ontology;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.apache.jena.atlas.lib.StrUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 7/22/16.
 */
public abstract class JenaOntology implements ITrestleOntology {

    private static final Logger logger = LoggerFactory.getLogger(JenaOntology.class);

    protected final String ontologyName;
    protected final Model model;
    protected final Graph graph;
    protected final OWLOntology ontology;
    protected final DefaultPrefixManager pm;
    protected final OWLDataFactory df;

    JenaOntology(String ontologyName, Model model, OWLOntology ontology, DefaultPrefixManager pm) {
        this.ontologyName = ontologyName;
        this.model = model;
        this.graph = this.model.getGraph();
        this.ontology = ontology;
        this.pm = pm;
        this.df = OWLManager.getOWLDataFactory();
    }

    abstract public boolean isConsistent();

    @Override
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        this.openTransaction(false);
        final Resource modelResource = model.getResource(getFullIRI(individual).toString());
        final Property modelProperty = model.getProperty(getFullIRI(property).toString());
        final StmtIterator stmtIterator = modelResource.listProperties(modelProperty);

        Set<OWLObjectProperty> properties = new HashSet<>();
        while (stmtIterator.hasNext()) {
            final Statement statement = stmtIterator.nextStatement();
//            We need to check to make sure the returned statement points to a valid URI, otherwise it's just Jena junk.
            if (statement.getObject().isURIResource()) {
                final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getObject().toString()));
                properties.add(owlObjectProperty);
            } else {
                logger.error("Model doesn't contain resource {}", statement.getObject());
            }
        }
        if (properties.isEmpty()) {
            logger.error("Individual {} has no properties {}", individual.getIRI(), property.getIRI());
            return Optional.empty();
        }
        stmtIterator.close();
        this.commitTransaction();

        return Optional.of(properties);
    }

    @Override
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {
        this.openTransaction(true);
        final Resource modelResource = model.createResource(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
        final Resource modelClass = model.createResource(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
        modelResource.addProperty(RDF.type, modelClass);
        this.commitTransaction();
    }

    @Override
    public void createProperty(OWLProperty property) {
        this.openTransaction(true);
        final Resource modelResource = model.createResource(getFullIRIString(property));
        if (property.isOWLDataProperty()) {
            modelResource.addProperty(RDF.type, OWL.DatatypeProperty);
        } else if (property.isOWLObjectProperty()) {
            modelResource.addProperty(RDF.type, OWL.ObjectProperty);
        }
        this.commitTransaction();
    }

    @Override
    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {
        this.openTransaction(true);
        //        Does the individual exist?
        final Resource modelResource = model.getResource(getFullIRIString(dataProperty.getSubject().asOWLNamedIndividual()));
        if (!model.containsResource(modelResource)) {
            throw new MissingOntologyEntity("missing class: ", dataProperty.getSubject());
        }

        final Property modelProperty = model.getProperty(getFullIRIString(dataProperty.getProperty().asOWLDataProperty()));
        if (!model.containsResource(modelProperty)) {
            createProperty(dataProperty.getProperty().asOWLDataProperty());
        }

        final RDFDatatype dataType = TypeMapper.getInstance().getTypeByName(dataProperty.getObject().getDatatype().toStringID());
        modelResource.addProperty(modelProperty,
                dataProperty.getObject().getLiteral(),
                dataType);

        if (!modelResource.hasProperty(modelProperty)) {
            logger.error("Cannot set property {} on Individual {}", dataProperty.getProperty().asOWLDataProperty().getIRI(), dataProperty.getSubject().asOWLNamedIndividual().getIRI());
        }
        this.commitTransaction();
    }

    @Override
    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {
        this.openTransaction(true);
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
        this.commitTransaction();
    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {
        this.openTransaction(false);
        final Resource resource = model.getResource(getFullIRIString(individual));
        this.commitTransaction();
        return model.containsResource(resource);
    }

    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    public void applyChange(OWLAxiomChange... axiom) {
        applyChanges(axiom);
    }

    private void applyChanges(OWLAxiomChange... axioms) {
        ontology.getOWLOntologyManager().applyChanges(Arrays.asList(axioms));
    }

    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    @Override
    public void openTransaction(boolean write) {
        logger.info("Opening model transaction");
        model.begin();
    }

    @Override
    public void commitTransaction() {
        logger.info("Committing model transaction");
        model.commit();
    }

    //    TODO(nrobison): Implement this
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        this.openTransaction(false);
        final Resource modelResource = model.getResource(getFullIRIString(owlClass));
        final ResIterator resIterator = model.listResourcesWithProperty(RDF.type, modelResource);
        Set<OWLNamedIndividual> instances = new HashSet<>();
        while (resIterator.hasNext()) {
            final Resource resource = resIterator.nextResource();
            instances.add(df.getOWLNamedIndividual(IRI.create(resource.getURI())));
        }
        resIterator.close();
        this.commitTransaction();

        return instances;
    }

    //    TODO(nrobison): Does this actually work on a local ontology?
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        this.openTransaction(false);
        final Resource modelResource = model.getResource(getFullIRIString(individual));
        if (modelResource == null) {
            return Optional.empty();
        }
        this.commitTransaction();
        return Optional.of(individual);
//        final Set<OWLNamedIndividual> entities = reasoner.getSameIndividuals(individual).getEntities();
//        if (entities.contains(individual)) {
//            return Optional.of(individual);
//        } else {
//            return Optional.empty();
//        }
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {
        this.openTransaction(false);
        final Resource modelResource = model.getResource(getFullIRIString(individual));
        final Property modelProperty = model.getProperty(getFullIRIString(property));

        if (modelProperty == null) {
            logger.error("Property {} doesn't exist on individual {}", property.getIRI(), individual.getIRI());
            return Optional.empty();
        }

        final StmtIterator stmtIterator = modelResource.listProperties(modelProperty);
        //        Build and return the OWLLiteral
        Set<OWLLiteral> properties = new HashSet<>();

        while (stmtIterator.hasNext()) {
            //        If the URI is null, I think that means that it's just a string
            final Statement statement = stmtIterator.nextStatement();
            final OWLDatatype owlDatatype;
            if (statement.getLiteral().getDatatypeURI() == null) {
                logger.error("Property {} as an emptyURI", property.getIRI());
                owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
            } else {
                owlDatatype = df.getOWLDatatype(IRI.create(statement.getLiteral().getDatatypeURI()));
            }

            if (owlDatatype.getIRI().toString().equals("nothing")) {
                logger.error("Datatype {} doesn't exist", statement.getLiteral().getDatatypeURI());
//                return Optional.empty();
                continue;
            }
            final OWLLiteral parsedLiteral = df.getOWLLiteral(statement.getLiteral().getLexicalForm(), owlDatatype);
            properties.add(parsedLiteral);
        }
        if (properties.isEmpty()) {
            logger.error("Individual {} has no properties {}", individual.getIRI(), property.getIRI());
            return Optional.empty();
        }
        stmtIterator.close();
        this.commitTransaction();

        return Optional.of(properties);
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

    public ResultSet executeSPARQL(String queryString) {
        this.openTransaction(false);
        final Query query = QueryFactory.create(queryString);

//        final QueryExecution qExec = SparqlDLExecutionFactory.create(query, this.model);
        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        final ResultSet resultSet = qExec.execSelect();
        ResultSetFormatter.out(System.out, resultSet, query);
        qExec.close();
        this.commitTransaction();

        return resultSet;
    }

    abstract public void close(boolean drop);

    /**
     * Open a transaction and lock it, for lots of bulk action
     */
    public abstract void lock();

    /**
     * Open a transaction and lock it
     * @param write - Open writable transaction?
     */
    public abstract void openAndLock(boolean write);

    /**
     * Unlock the model to allow for closing the transaction
     */
    public abstract void unlock();

    /**
     * Unlock the transaction and commit it
     */
    public abstract void unlockAndClose();

    protected static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
