package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.spatial.EntityDefinition;
import org.apache.jena.query.spatial.SpatialDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/15/16.
 */
@SuppressWarnings({"initialization", "Duplicates"})
public class LocalOntology implements ITrestleOntology {

    private final String ontologyName;
    private final static Logger logger = LoggerFactory.getLogger(LocalOntology.class);
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final InfModel model;
    private final Graph graph;
    private final OWLDataFactory df;


    LocalOntology(String ontologyName, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner) {
        this.ontologyName = ontologyName;
        ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
        this.df = OWLManager.getOWLDataFactory();
//        Instead of a database object, we use a Jena model to support the RDF querying
        Dataset ds = initialiseTDB();


//        spatial stuff
        Directory indexDirectory;
        Dataset spatialDataset = null;
        logger.debug("Building TDB and Lucene database");
        try {
            indexDirectory = FSDirectory.open(new File("./target/data/lucene/"));
//            Not sure if these entity and geo fields are correct, but oh well.
            EntityDefinition ed = new EntityDefinition("uri", "geo");
            ed.setSpatialContextFactory("com.spatial4j.core.context.jts.JtsSpatialContextFactory");
//            Create a spatial dataset that combines the TDB dataset + the spatial index
             spatialDataset = SpatialDatasetFactory.createLucene(ds, indexDirectory, ed);
            logger.debug("Lucene index is up and running");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create spatial dataset", e);
        }
        final Reasoner owlReasoner = ReasonerRegistry.getOWLReasoner();
//        I don't think I need the TDBmodelgetter, since I just bind it to the default model
        this.model = ModelFactory.createInfModel(owlReasoner, spatialDataset.getDefaultModel());
//        TDB.sync(this.model);
        this.graph = this.model.getGraph();

    }

    private Dataset initialiseTDB() {
        final String tdbPath = "target/data/tdb";
        new File(tdbPath).mkdirs();

        return TDBFactory.createDataset(tdbPath);
    }

    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    @Override
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
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

        return Optional.of(properties);
    }

    @Override
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {

        final Resource modelResource = model.createResource(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
        final Resource modelClass = model.createResource(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
        modelResource.addProperty(RDF.type, modelClass);
    }

    @Override
    public void createProperty(OWLProperty property) {

        final Resource modelResource = model.createResource(getFullIRIString(property));
        if (property.isOWLDataProperty()) {
            modelResource.addProperty(RDF.type, OWL.DatatypeProperty);
        } else if (property.isOWLObjectProperty()) {
            modelResource.addProperty(RDF.type, OWL.ObjectProperty);
        }
    }

    @Override
    public void writeIndividualDataProperty(OWLDataPropertyAssertionAxiom dataProperty) throws MissingOntologyEntity {

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
    }

    @Override
    public void writeIndividualObjectProperty(OWLObjectPropertyAssertionAxiom property) throws MissingOntologyEntity {

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
    }

    @Override
    public boolean containsResource(OWLNamedObject individual) {

        final Resource resource = model.getResource(getFullIRIString(individual));
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
    public void openTransaction() {
        logger.info("Opening model transaction");
        model.begin();
    }

    @Override
    public void commitTransaction() {
        logger.info("Committing model transaction");
        model.commit();
    }

    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        return reasoner.getInstances(owlClass, direct).getFlattened();
    }

//    TODO(nrobison): Does this actually work on a local ontology?
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        final Set<OWLNamedIndividual> entities = reasoner.getSameIndividuals(individual).getEntities();
        if (entities.contains(individual)) {
            return Optional.of(individual);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {

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

    public void initializeOntology() {
//        this.openTransaction();
        if (!model.isEmpty()) {
            logger.info("Dropping local ontology {}", ontologyName);
            model.removeAll();
        }

        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
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

        model.read(is, null);
//        this.commitTransaction();
        TDB.sync(this.model);
    }

    @Override
    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    @Override
    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    public ResultSet executeSPARQL(String queryString) {
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = SparqlDLExecutionFactory.create(query, this.model);
        final ResultSet resultSet = qExec.execSelect();
        ResultSetFormatter.out(System.out, resultSet, query);
        qExec.close();

        return resultSet;
    }

    public void close(boolean drop) {

        logger.debug("Shutting down reasoner and model");
        reasoner.dispose();
        model.close();
        TDB.closedown();
        if (drop) {
//            Just delete the directory
            try {
                logger.debug("Deleting directory {}", "./target/data");
                FileUtils.deleteDirectory(new File("./target/data"));
            } catch (IOException e) {
                logger.error("Couldn't delete data directory");
            }
        }
    }

    private ByteArrayInputStream ontologyToIS() {
        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
//            Jena doesn't support OWL/XML, so we need base RDF.
            ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Cannot save ontology to bytearray", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

//    This comes from an online gist, not sure if it's really necessary or not, but it seems to work
//    https://gist.github.com/ijdickinson/3830267
    static class LocalTDBModelGetter implements ModelGetter {

        private final Dataset ds;

        LocalTDBModelGetter(Dataset dataset) {
            this.ds = dataset;
        }

        @Override
        public Model getModel(String URL) {
            throw new NotImplemented("getModel( String ) is no implemented");
        }

        @Override
        public Model getModel(String URL, ModelReader loadIfAbsent) {
            Model m = ds.getNamedModel(URL);

            if (m == null) {
                m = ModelFactory.createDefaultModel();
                loadIfAbsent.readModel(m, URL);
                ds.addNamedModel(URL, m);
            }

            return m;
        }
    }
}
