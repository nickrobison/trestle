package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.nickrobison.trestle.common.EPSGParser;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import oracle.spatial.rdf.client.jena.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by nrobison on 5/23/16.
 */
@SuppressWarnings("initialization")
public class OracleOntology implements ITrestleOntology {

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    private final String ontologyName;
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final Oracle oracle;
    private final OWLDataFactory df;
    private final Model model;
    private final GraphOracleSem graph;

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner, String connectionString, String username, String password) {
        this.ontologyName = name;
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
        this.df = OWLManager.getOWLDataFactory();
//        try {
//            this.database = new OracleDatabase(connectionString, username, password, ontologyName);
//        } catch (SQLException e) {
//            throw new RuntimeException("Cannot connect to Oracle database", e);
//        }

//        Other ontology stuff

        final Attachment owlprime = Attachment.createInstance(
                new String[]{}, "OWLPRIME",
                InferenceMaintenanceMode.NO_UPDATE, QueryOptions.DEFAULT);
        this.oracle = new Oracle(connectionString, username, password);
        try {
//            We need this so that it actually creates the model if it doesn't exist
            ModelOracleSem.createOracleSemModel(oracle, ontologyName);
            this.graph = new GraphOracleSem(oracle, ontologyName, owlprime);
            this.model = new ModelOracleSem(graph);
        } catch (SQLException e) {
            throw new RuntimeException("Can't create oracle model", e);
        }


    }

    /**
     * @return - Returns the raw underlying ontology
     */
    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    /**
     * @return - Returns the raw underlying prefix manager
     */
    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    public void applyChange(OWLAxiomChange... axiom) {
        applyChanges(axiom);
    }

    private void applyChanges(OWLAxiomChange... axioms) {
        ontology.getOWLOntologyManager().applyChanges(Arrays.asList(axioms));
    }

    /**
     * @return - Returns whether or not the reasoner state is consistent
     */
    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    public void openTransaction(boolean write) {
        logger.info("Opening model transaction");
        model.begin();
    }

    public void commitTransaction() {
        logger.info("Committing model transaction");
        model.commit();
    }

    /**
     * Returns the set of all instances matching the given class
     *
     * @param owlClass - OWLClass to retrieve
     * @param direct
     * @return - Returns the set of OWLNamedIndividuals that are members of the given class
     */
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {

        final Resource modelResource = model.getResource(getFullIRIString(owlClass));
        final ResIterator resIterator = model.listResourcesWithProperty(RDF.type, modelResource);
        Set<OWLNamedIndividual> instances = new HashSet<>();
        while (resIterator.hasNext()) {
            final Resource resource = resIterator.nextResource();
            instances.add(df.getOWLNamedIndividual(IRI.create(resource.getURI())));
        }

        return instances;
    }

    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {

//        Try directly from the reasoner
        final Set<OWLNamedIndividual> entities = reasoner.getSameIndividuals(individual).getEntities();
        if (entities.contains(individual)) {
            return Optional.of(individual);
        } else {
            return Optional.empty();
        }

//        final OWLDataFactory df = OWLManager.getOWLDataFactory();
////        final OWLDataFactory df = reasoner.getOWLDataFactory();
//        List<AddAxiom> ontologyAxioms = new ArrayList<>();
//
////        Need to pass the full IRI, Jena doesn't understand prefixes
//        final Resource singleResource = database.getIndividual(getFullIRI(individual.getIRI()));
//        //        Build the individual
//        final StmtIterator stmtIterator = singleResource.listProperties();
//        int statementCount = 0;
//        while (stmtIterator.hasNext()) {
//            final Statement statement = stmtIterator.nextStatement();
//            final AddAxiom addAxiom;
////            FIXME(nrobison): This is pretty gross right now, only supports basic
//            if (statement.getObject().isLiteral()) {
//                final OWLDataProperty owlDataProperty = df.getOWLDataProperty(IRI.create(statement.getPredicate().toString()));
//                final OWLLiteral owlLiteral = df.getOWLLiteral(statement.getLiteral().getLexicalForm(), OWL2Datatype.getDatatype(IRI.create(statement.getLiteral().getDatatypeURI())));
//                addAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(owlDataProperty, individual, owlLiteral));
//                ontologyAxioms.add(addAxiom);
//                applyChange(addAxiom);
////                Commit the change, for now.
//            } else if (statement.getObject().isURIResource()) {
//                final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getPredicate().toString()));
//                final OWLNamedIndividual owlObjectTarget = df.getOWLNamedIndividual(IRI.create(statement.getObject().toString()));
//                addAxiom = new AddAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, individual, owlObjectTarget));
//                ontologyAxioms.add(addAxiom);
//            } else {
//                throw new RuntimeException("Cannot parse this statement: " + statement);
//            }
//            applyChange(addAxiom);
//            statementCount++;
//        }
//        if (statementCount > 0) {
//            return Optional.of(individual);
//        } else {
//            return Optional.empty();
//        }

//        Get it back from the ontology?
//        Commit the changes and return
//        applyChange((OWLAxiomChange[]) ontologyAxioms.toArray());
//        return individual;

//        singleResource.listProperties();
    }

    //    TODO(nrobison): Make this return an iterator load into the HashSet
    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {

        final Statement modelProperty = model.getProperty(model.getResource(getFullIRI(individual.getIRI()).toString()),
                model.getProperty(getFullIRI(property.getIRI()).toString()));
        if (modelProperty == null) {
            logger.error("Property {} doesn't exist on individual {}", property.getIRI(), individual.getIRI());
            return Optional.empty();
        }

//        Build and return the OWLLiteral
        Set<OWLLiteral> properties = new HashSet<>();
//        If the URI is null, I think that means that it's just a string
        final OWLDatatype owlDatatype;
        if (modelProperty.getLiteral().getDatatypeURI() == null) {
            logger.error("Property {} as an emptyURI", property.getIRI());
            owlDatatype = df.getOWLDatatype(OWL2Datatype.XSD_STRING.getIRI());
        } else {
            owlDatatype = df.getOWLDatatype(IRI.create(modelProperty.getLiteral().getDatatypeURI()));
        }

        if (owlDatatype.getIRI().toString().equals("nothing")) {
            logger.error("Datatype {} doesn't exist", modelProperty.getLiteral().getDatatypeURI());
            return Optional.empty();
        }
        final OWLLiteral parsedLiteral = df.getOWLLiteral(modelProperty.getLiteral().getLexicalForm(), owlDatatype);
        properties.add(parsedLiteral);
        return Optional.of(properties);
    }

    //    TODO(nrobison): Close iterator
    @Override
    public Optional<Set<OWLObjectProperty>> getIndividualObjectProperty(OWLNamedIndividual individual, OWLObjectProperty property) {
        final Resource modelResource = model.getResource(getFullIRI(individual).toString());
        final Property modelProperty = model.getProperty(getFullIRI(property).toString());
        final StmtIterator stmtIterator = modelResource.listProperties(modelProperty);

        Set<OWLObjectProperty> properties = new HashSet<>();
        while (stmtIterator.hasNext()) {
            final Statement statement = stmtIterator.nextStatement();
            final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getObject().toString()));
            properties.add(owlObjectProperty);
        }
        if (properties.isEmpty()) {
            logger.error("Individual {} has no properties {}", individual.getIRI(), property.getIRI());
            return Optional.empty();
        }

        return Optional.of(properties);
    }

    //    FIXME(nrobison): This should have the ability to be locked to avoid polluting the ontology
    @Override
    public void createIndividual(OWLClassAssertionAxiom owlClassAssertionAxiom) {

        final Resource modelResource = model.createResource(getFullIRIString(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual()));
        final Resource modelClass = model.createResource(getFullIRIString(owlClassAssertionAxiom.getClassExpression().asOWLClass()));
        modelResource.addProperty(RDF.type, modelClass);
    }

    //    FIXME(nrobison): This should have the ability to be locked to avoid polluting the ontology
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

    /**
     * Write the ontology to disk
     *
     * @param path     - IRI of location to write ontology
     * @param validate - boolean validate ontology before writing
     * @throws OWLOntologyStorageException
     */
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
        if (validate) {
            if (!isConsistent()) {
                throw new RuntimeException("OracleOntology is invalid");
            }
        }

        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(new File("/Users/nrobison/Desktop/test.owl"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot open file", e);
        }
//        ontology.getOWLOntologyManager().saveOntology(ontology, new OWLXMLDocumentFormat(), path);
        this.model.write(fileOutputStream);
    }

    private void loadEPSGCodes() {
        final List<AddAxiom> addAxioms = EPSGParser.parseEPSGCodes(this.ontology, this.pm);
        applyChanges(addAxioms.toArray(new AddAxiom[addAxioms.size()]));
    }

    public void initializeOntology() {
        logger.info("Dropping Oracle ontology {}", ontologyName);
        if (!model.isEmpty()) {
            model.removeAll();
        }

        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        try {
            this.graph.dropApplicationTableIndex();
        } catch (SQLException e) {
            logger.error("Cannot drop application index", e);
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

        model.read(is, null);

//        Setup Inference engine
        try {
            runInference();
        } catch (SQLException e) {
            logger.error("Cannot setup inference engine on {}", ontologyName, e);
        }

        logger.info("Rebuilding indexes for {}", this.ontologyName);

        try {
            this.graph.rebuildApplicationTableIndex();
        } catch (SQLException e) {
            logger.error("Cannot rebuild indexes for {}", this.ontologyName, e);
        }

    }

    /**
     * Run the inference engine and rebuild the indexes.
     * The inference engine is only run manually, via this method.
     *
     * @throws SQLException
     */
    public void runInference() throws SQLException {

//        this.graph = new GraphOracleSem(oracle, ontologyName, owlprime);

        logger.info("Rebuilding graph and performing inference");
        this.graph.analyze();
        this.graph.performInference();
        this.graph.rebuildApplicationTableIndex();
    }

    /**
     * Converts a prefixed IRI to the full one for the reasoner
     *
     * @param iri - Prefixed IRI to convert
     * @return - IRI with full URI attached
     */
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

    public IRI getFullIRI(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject.getIRI());
    }

    public String getFullIRIString(OWLNamedObject owlNamedObject) {
        return getFullIRI(owlNamedObject).toString();
    }

    /**
     * Return the number of asserted triples in the ontology
     *
     * @return long - Number of triples in ontology
     */
    public long getTripleCount() {

        return this.graph.getCount(Triple.ANY);
    }

    /**
     * Execute a raw SPARQL Query against the ontology
     *
     * @param queryString - Query String
     * @return - Jena ResultSet
     */
    @SuppressWarnings("Duplicates")
    public ResultSet executeSPARQL(String queryString) {
//        OracleDatabase oraDB = connectToDatabase();
//        return database.executeRawSPARQL(query);
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = QueryExecutionFactory.create(query, this.model);
        final ResultSet resultSet = qExec.execSelect();
        ResultSetFormatter.out(System.out, resultSet, query);
//        Make sure to not close the executor until after reading out the triples!
        qExec.close();

        return resultSet;

    }

    /**
     * Shutdown the reasoner and disconnect from the database
     */
    public void close(boolean drop) {
        logger.debug("Disconnecting");
        reasoner.dispose();
        model.close();
        if (drop) {
            logger.info("Dropping model: {}", this.ontologyName);
            try {
                OracleUtils.dropSemanticModel(oracle, this.ontologyName);
            } catch (SQLException e) {
                throw new RuntimeException("Cannot drop oracle model", e);
            }
        }
        try {
            oracle.dispose();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot disconnect from oracle database");
        }
//        final OracleDatabase oraDB = connectToDatabase();
//        database.disconnect();
    }
}
