package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.nickrobison.trestle.common.EPSGParser;
import oracle.spatial.rdf.client.jena.GraphOracleSem;
import oracle.spatial.rdf.client.jena.ModelOracleSem;
import oracle.spatial.rdf.client.jena.Oracle;
import oracle.spatial.rdf.client.jena.OracleUtils;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by nrobison on 5/23/16.
 */
public class OracleOntology implements ITrestleOntology {

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    private final String ontologyName;
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final Oracle oracle;
//    private final IOntologyDatabase database;
    private final Model model;
    private final GraphOracleSem graph;

//    Directly access the Jena model

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner, String connectionString, String username, String password) {
        this.ontologyName = name;
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
//        try {
//            this.database = new OracleDatabase(connectionString, username, password, ontologyName);
//        } catch (SQLException e) {
//            throw new RuntimeException("Cannot connect to Oracle database", e);
//        }

//        Other ontology stuff
        this.oracle = new Oracle(connectionString, username, password);
        try {
//            Model oracleModel = ModelOracleSem.createOracleSemModel(oracle, ontologyName);
//            this.model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, oracleModel);
            this.model = ModelOracleSem.createOracleSemModel(oracle, ontologyName);
            this.graph = (GraphOracleSem) this.model.getGraph();
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

    /**
     * Returns the set of all instances matching the given class
     *
     * @param owlClass - OWLClass to retrieve
     * @param direct
     * @return - Returns the set of OWLNamedIndividuals that are members of the given class
     */
//    FIXME(nrobison): I think the reasoner is out of sync, so this is completely wrong right now.
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        return reasoner.getInstances(owlClass, direct).getFlattened();
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

    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {
//        I think we need to grab the individual from the database, an put it into the model, before trying to get the properties
        final Optional<OWLNamedIndividual> ontologyIndividual = getIndividual(individual);
        if (ontologyIndividual.isPresent()) {
            final Set<OWLLiteral> dataPropertyValues = reasoner.getDataPropertyValues(ontologyIndividual.get(), property);
            if (dataPropertyValues.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(dataPropertyValues);
            }

        } else {
            return Optional.empty();
        }


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
        ontology.getOWLOntologyManager().saveOntology(ontology, new OWLXMLDocumentFormat(), path);
    }

    private void loadEPSGCodes() {
        final List<AddAxiom> addAxioms = EPSGParser.parseEPSGCodes(this.ontology, this.pm);
        applyChanges(addAxioms.toArray(new AddAxiom[addAxioms.size()]));
    }

    public void initializeOntology() {
        logger.info("Removing all nodes from Oracle ontology {}", ontologyName);
        model.removeAll();
//        try {
//            OracleUtils.dropSemanticModel(oracle, ontologyName);
//        } catch (SQLException e) {
//            logger.error("Cannot drop ontology {}", ontologyName, e);
//        }

//        OracleDatabase oraDB = connectToDatabase();

//        Setup bulk import mode
//        database.enableBulkLoading();

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
        model.read(is, null);

//        Finish the loading
        logger.info("Rebuilding graph and indexes for {}", this.ontologyName);
        try {
            this.graph.analyze();
        } catch (SQLException e) {
            logger.error("Cannot analyze {}", this.ontologyName, e);
        }
        try {
            this.graph.rebuildApplicationTableIndex();
        } catch (SQLException e) {
            logger.error("Cannot rebuild indexes for {}", this.ontologyName, e);
        }
    }

    //    TODO(nrobison): Close connection?
//    private OracleDatabase connectToDatabase() {
//
//        OracleDatabase oraDB;
////        final OWLDataFactory df = OWLManager.getOWLDataFactory();
//        try {
//            oraDB = new OracleDatabase();
//        } catch (SQLException e) {
//            throw new RuntimeException("Problem with Oracle", e);
//        }
//
//        return oraDB;
//    }

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
            logger.debug("Dropping model: {}", this.ontologyName);
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
