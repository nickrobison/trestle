package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.nickrobison.trestle.common.EPSGParser;
import com.nickrobison.trestle.db.IOntologyDatabase;
import com.nickrobison.trestle.db.oracle.OracleDatabase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by nrobison on 5/23/16.
 */
// TODO(nrobison): This should support initializing the oracle database on construction
//    FIXME(nrobison): This database handling is a total disaster, fix it!!!
public class OracleOntology implements ITrestleOntology {

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    public static final String MAIN_GEO = "main_geo:";
    private final String ontologyName;
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final IOntologyDatabase database;

    OracleOntology(String name, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner, String connectionString, String username, String password) {
        this.ontologyName = name;
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
        try {
            this.database = new OracleDatabase(connectionString, username, password, ontologyName);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to Oracle database", e);
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
        final OWLDataFactory df = OWLManager.getOWLDataFactory();
//        final OWLDataFactory df = reasoner.getOWLDataFactory();
        List<AddAxiom> ontologyAxioms = new ArrayList<>();

//        Need to pass the full IRI, Jena doesn't understand prefixes
        final Resource singleResource = database.getIndividual(getFullIRI(individual.getIRI()));
        //        Build the individual
        final StmtIterator stmtIterator = singleResource.listProperties();
        int statementCount = 0;
        while (stmtIterator.hasNext()) {
            final Statement statement = stmtIterator.nextStatement();
            final AddAxiom addAxiom;
//            FIXME(nrobison): This is pretty gross right now, only supports basic
            if (statement.getObject().isLiteral()) {
                final OWLDataProperty owlDataProperty = df.getOWLDataProperty(IRI.create(statement.getPredicate().toString()));
                final OWLLiteral owlLiteral = df.getOWLLiteral(statement.getLiteral().getLexicalForm(), OWL2Datatype.getDatatype(IRI.create(statement.getLiteral().getDatatypeURI())));
                addAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(owlDataProperty, individual, owlLiteral));
                ontologyAxioms.add(addAxiom);
                applyChange(addAxiom);
//                Commit the change, for now.
            } else if (statement.getObject().isURIResource()) {
                final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(IRI.create(statement.getPredicate().toString()));
                final OWLNamedIndividual owlObjectTarget = df.getOWLNamedIndividual(IRI.create(statement.getObject().toString()));
                addAxiom = new AddAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, individual, owlObjectTarget));
                ontologyAxioms.add(addAxiom);
            } else {
                throw new RuntimeException("Cannot parse this statement: " + statement);
            }
            applyChange(addAxiom);
            statementCount++;
        }
        if (statementCount > 0) {
            return Optional.of(individual);
        } else {
            return Optional.empty();
        }

//        Get it back from the ontology?
//        Commit the changes and return
//        applyChange((OWLAxiomChange[]) ontologyAxioms.toArray());
//        return individual;

//        singleResource.listProperties();
    }

    @Override
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
//        OracleDatabase oraDB = connectToDatabase();

//        Setup bulk import mode
        database.enableBulkLoading();

        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
//            Jena doesn't support OWL/XML, so we need base RDF.
            ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Cannot save ontology to bytearray", e);
        }

        final ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());

//        OracleDatabase oraDB = connectToDatabase();
//        oraDB.enableBulkLoading();
        database.loadBaseOntology(is);
        database.rebuildIndexes();
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
     * @param query - Query String
     * @return - Jena ResultSet
     */
    public ResultSet executeSPARQL(String query) {
//        OracleDatabase oraDB = connectToDatabase();
        return database.executeRawSPARQL(query);

    }

    /**
     * Shutdown the reasoner and disconnect from the database
     */
    public void close() {
        logger.debug("Disconnecting");
        reasoner.dispose();
//        final OracleDatabase oraDB = connectToDatabase();
        database.disconnect();
    }
}
