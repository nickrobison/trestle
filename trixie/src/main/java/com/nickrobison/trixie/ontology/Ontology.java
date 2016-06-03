package com.nickrobison.trixie.ontology;

import com.nickrobison.trixie.db.oracle.OracleDatabase;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public class Ontology implements IOntology {

    private final static Logger logger = Logger.getLogger(Ontology.class);
    public static final String MAIN_GEO = "main_geo:";
    private final OWLOntology ontology;
    private final FaCTPlusPlusReasoner reasoner;
    private final DefaultPrefixManager pm;

    Ontology(OWLOntology ont, DefaultPrefixManager pm, FaCTPlusPlusReasoner reasoner) {
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
    }

    /**
     * @param iri - IRI of the Ontology to load
     * @return - new Builder to instantiate ontology
     */
    public static Builder from(IRI iri) {
        return new Builder(iri);
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

    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass) {
        return reasoner.getInstances(owlClass, true).getFlattened();
    }

    /**
     * @param path     - IRI of location to write ontology
     * @param validate - boolean validate ontology before writing
     * @throws OWLOntologyStorageException
     */
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
        if (validate) {
            if (!isConsistent()) {
                throw new RuntimeException("Ontology is invalid");
            }
        }
        ontology.getOWLOntologyManager().saveOntology(ontology, new OWLXMLDocumentFormat(), path);
    }

    public void initializeOracleOntology(IRI filename) {
        OracleDatabase oraDB;
        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        try {
            oraDB = new OracleDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Problem with Oracle", e);
        }

//        Setup bulk import mode
        oraDB.enableBulkLoading();

        oraDB.loadBaseOntology(filename.toString());


//        Create the OWl objects
        final OWLClass geographicCRSClass = df.getOWLClass(IRI.create(MAIN_GEO, "GeographicCRS").toString(), pm);
        final OWLObjectProperty has_property = df.getOWLObjectProperty(IRI.create(MAIN_GEO, "has_property").toString(), pm);
        final OWLDataProperty EPSGCodeProperty = df.getOWLDataProperty(IRI.create(MAIN_GEO, "EPSG_Code").toString(), pm);
        final OWLClass WKTStringClass = df.getOWLClass(IRI.create(MAIN_GEO, "WKT_String").toString(), pm);

//        We need to open the EPSG database and write in the projections
        final Set<String> supportedEPSGCodes = CRS.getSupportedCodes("EPSG");
        CoordinateReferenceSystem crs;
        final Iterator<String> setIterator = supportedEPSGCodes.iterator();
        while (setIterator.hasNext()) {
            final String code = setIterator.next();
//        supportedEPSGCodes
//                .stream()
//                .forEach(code -> {
//            final CoordinateReferenceSystem crs;
            try {
                crs = CRS.decode(code);
            } catch (FactoryException e) {
                logger.warn("Can't decode: " + code, e);
//                FIXME(nrobison): Why is this not working? It should skip to the next element in the set
                break;
//                throw new RuntimeException("Can't decode: " + code, e);
            }
//                    Create the CRS individual
            final IRI crsIRI = IRI.create(MAIN_GEO, crs.getName().getCode());
            final OWLNamedIndividual crsIndividual = df.getOWLNamedIndividual(crsIRI.toString(), pm);
            final AddAxiom classAssertionAxiom = new AddAxiom(ontology, df.getOWLClassAssertionAxiom(geographicCRSClass, crsIndividual));
//            applyChange(classAssertionAxiom);

//            Add the projection properties
//            EPSG Code
            final OWLLiteral epsgLiteral = df.getOWLLiteral(code);
            final AddAxiom epsgCodeAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(EPSGCodeProperty, crsIndividual, epsgLiteral));
//            final OWLNamedIndividual epsgCode = df.getOWLNamedIndividual(IRI.create("main_geo:", code).toString(), pm);
//            final AddAxiom epsgCodeAxiom = new AddAxiom(ontology, df.getOWLClassAssertionAxiom(EPSGCodeProperty, epsgCode));
//            df.getOWLDataPropertyAssertionAxiom()
////            applyChange(epsgCodeAxiom);
////            final OWLObjectMinCardinality projectionProperty = df.getOWLObjectMinCardinality(1, has_property, epsgCode.asOWLClass());
////            df.getOWLObjectProperty()
////            final OWLSubClassOfAxiom owlSubClassOfAxiom = df.getOWLSubClassOfAxiom(crsIndividual.asOWLClass(), projectionProperty);
//            final AddAxiom projectionAxiom = new AddAxiom(ontology, df.getOWLSubClassOfAxiom(crsIndividual.asOWLClass(), projectionProperty));
////            applyChange(projectionAxiom);
            applyChanges(classAssertionAxiom, epsgCodeAxiom);


        }

//        Rebuild indexes
//        Write out the ontology (for testing only)
        try {
            writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), false);
        } catch (OWLOntologyStorageException e) {
            logger.error("Can't write ontology", e);
        }
//        Rebuild the oracle indexes
//        oraDB.rebuildIndexes();
    }
}
