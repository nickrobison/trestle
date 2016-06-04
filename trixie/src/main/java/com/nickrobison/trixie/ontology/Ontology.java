package com.nickrobison.trixie.ontology;

import com.nickrobison.trixie.db.oracle.OracleDatabase;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public class Ontology implements IOntology {

    private final static Logger logger = LoggerFactory.getLogger(Ontology.class);
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

    public void initializeOntology(boolean oracle) {

        loadEPSGCodes();
        if (isConsistent()) {
            logger.error("Ontology is inconsistent");
        }
    }

    private void loadEPSGCodes() {

//        Create data factory
        final OWLDataFactory df = OWLManager.getOWLDataFactory();

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
                } catch(NumberFormatException e) {
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

//        Rebuild indexes
//        Rebuild the oracle indexes
//        oraDB.rebuildIndexes();
    }
}
