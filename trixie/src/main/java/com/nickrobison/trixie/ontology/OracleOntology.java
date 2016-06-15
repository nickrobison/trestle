package com.nickrobison.trixie.ontology;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.nickrobison.trixie.db.IOntologyDatabase;
import com.nickrobison.trixie.db.oracle.OracleDatabase;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by nrobison on 5/23/16.
 */
// TODO(nrobison): This should support initializing the oracle database on construction
//    FIXME(nrobison): This database handling is a total disaster, fix it!!!
public class OracleOntology implements ITrixieOntology {

    private final static Logger logger = LoggerFactory.getLogger(OracleOntology.class);
    public static final String MAIN_GEO = "main_geo:";
    private final OWLOntology ontology;
    private final FaCTPlusPlusReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final IOntologyDatabase database;

    OracleOntology(OWLOntology ont, DefaultPrefixManager pm, FaCTPlusPlusReasoner reasoner, String connectionString, String username, String password) {
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
        try {
            this.database = new OracleDatabase(connectionString, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to Oracle database", e);
        }
    }

    /**
     * @param iri - IRI of the OracleOntology to load
     * @return - new Builder to instantiate ontology
     */
    public static Builder from(IRI iri) {
        return new Builder(iri);
    }

    public static Builder withDBConnection(IRI iri, String connectionString, String username, String password) {
        return new Builder(iri, connectionString, username, password);
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
     * @return - Returns the set of OWLNamedIndividuals that are members of the given class
     */
//    FIXME(nrobison): I think the reasoner is out of sync, so this is completely wrong right now.
    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass) {
        return reasoner.getInstances(owlClass, false).getFlattened();
    }

    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        final OWLDataFactory df = reasoner.getOWLDataFactory();
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

    public void initializeOntology(boolean oracle) {

        loadEPSGCodes();
        if (isConsistent()) {
            logger.error("OracleOntology is inconsistent");
        }

        if (oracle) {
            initializeOracleOntology();
        }
    }

    private void loadEPSGCodes() {

//        Create data factory
//        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        final OWLDataFactory df = reasoner.getOWLDataFactory();

        final OWLClass CRSClass = df.getOWLClass(IRI.create(MAIN_GEO, "CRS").toString(), pm);
        final OWLObjectProperty has_property = df.getOWLObjectProperty(IRI.create(MAIN_GEO, "has_property").toString(), pm);
        final OWLDataProperty EPSGCodeProperty = df.getOWLDataProperty(IRI.create(MAIN_GEO, "EPSG_Code").toString(), pm);
        final OWLDataProperty is_projected = df.getOWLDataProperty(IRI.create(MAIN_GEO, "is_projected").toString(), pm);
        final OWLDataProperty WKTStringProperty = df.getOWLDataProperty(IRI.create(MAIN_GEO, "WKT_String").toString(), pm);

//        We need to open the EPSG database and write in the projections
        final Set<String> supportedEPSGCodes = CRS.getSupportedCodes("EPSG");
        for (String code : supportedEPSGCodes) {
            CoordinateReferenceSystem crs;
//            TODO(nrobison): Figure out how to parse the rest of the EPSG codes
//            TODO(nrobison): Change the EPSG codes to strings
            try {
//                Some of the EPSG codes come back as Strings, so we need to handle them appropriately
                try {
                    final int epsgCode = Integer.parseInt(code);
                    crs = CRS.decode("EPSG:" + epsgCode);
                } catch (NumberFormatException e) {
                    crs = CRS.decode(code);
                }

            } catch (FactoryException e) {
                logger.warn("Can't decode: {}", code, e);
                continue;
            }
//                    Create the CRS individual
            final IRI crsIRI = IRI.create(MAIN_GEO, code);
            final OWLNamedIndividual crsIndividual = df.getOWLNamedIndividual(crsIRI.toString(), pm);
            final AddAxiom classAssertionAxiom = new AddAxiom(ontology, df.getOWLClassAssertionAxiom(CRSClass, crsIndividual));

//            Add the projection properties
//            EPSG Code
            final OWLLiteral epsgLiteral = df.getOWLLiteral(code, OWL2Datatype.XSD_INTEGER);
            final AddAxiom epsgCodeAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(EPSGCodeProperty, crsIndividual, epsgLiteral));

//            WKT String
            final OWLLiteral wktLiteral = df.getOWLLiteral(crs.toString(), OWL2Datatype.XSD_STRING);
            final AddAxiom wktStringAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(WKTStringProperty, crsIndividual, wktLiteral));

//            is projected?
//            TODO(nrobison): Figure out projection property
            final OWLLiteral projectedLiteral = df.getOWLLiteral(false);
            final AddAxiom projectedAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(is_projected, crsIndividual, projectedLiteral));
//            final OWLNamedIndividual epsgCode = df.getOWLNamedIndividual(IRI.create("main_geo:", code).toString(), pm);
//            final AddAxiom epsgCodeAxiom = new AddAxiom(ontology, df.getOWLClassAssertionAxiom(EPSGCodeProperty, epsgCode));
//            df.getOWLDataPropertyAssertionAxiom()
////            applyChange(epsgCodeAxiom);
////            final OWLObjectMinCardinality projectionProperty = df.getOWLObjectMinCardinality(1, has_property, epsgCode.asOWLClass());
////            df.getOWLObjectProperty()
////            final OWLSubClassOfAxiom owlSubClassOfAxiom = df.getOWLSubClassOfAxiom(crsIndividual.asOWLClass(), projectionProperty);
//            final AddAxiom projectionAxiom = new AddAxiom(ontology, df.getOWLSubClassOfAxiom(crsIndividual.asOWLClass(), projectionProperty));
////            applyChange(projectionAxiom);
            applyChanges(classAssertionAxiom, epsgCodeAxiom, wktStringAxiom, projectedAxiom);

        }
    }

    public void initializeOracleOntology(IRI filename) {
//        OracleDatabase oraDB = connectToDatabase();

//        Setup bulk import mode
        database.enableBulkLoading();

        database.loadBaseOntology(filename.toString());

//        Rebuild indexes
        database.rebuildIndexes();
    }

    public void initializeOracleOntology() {
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
     * @param iri - Prefixed IRI to convert
     * @return - IRI with full URI attached
     */
    public IRI getFullIRI(IRI iri) {
        return pm.getIRI(iri.toString());
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
