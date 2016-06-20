package com.nickrobison.trixie.common;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by nrobison on 6/17/16.
 */
public class EPSGParser {

    private static final Logger logger = LoggerFactory.getLogger(EPSGParser.class);

    private static final String MAIN_GEO = "main_geo:";

    public EPSGParser() {    }

    public static List<AddAxiom> parseEPSGCodes(final OWLOntology ontology, final DefaultPrefixManager pm) {

//        Create at data factory
        final OWLDataFactory df = OWLManager.getOWLDataFactory();

        final OWLClass CRSClass = df.getOWLClass(IRI.create(MAIN_GEO, "CRS").toString(), pm);
        final OWLObjectProperty has_property = df.getOWLObjectProperty(IRI.create(MAIN_GEO, "has_property").toString(), pm);
        final OWLDataProperty EPSGCodeProperty = df.getOWLDataProperty(IRI.create(MAIN_GEO, "EPSG_Code").toString(), pm);
        final OWLDataProperty is_projected = df.getOWLDataProperty(IRI.create(MAIN_GEO, "is_projected").toString(), pm);
        final OWLDataProperty WKTStringProperty = df.getOWLDataProperty(IRI.create(MAIN_GEO, "WKT_String").toString(), pm);

//        We need to open the EPSG database and write in the projections
        final List<AddAxiom> changes = new ArrayList<>();
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
            final IRI crsIRI = IRI.create(MAIN_GEO, crs.getName().toString());
            final OWLNamedIndividual crsIndividual = df.getOWLNamedIndividual(crsIRI.toString(), pm);
            final AddAxiom classAssertionAxiom = new AddAxiom(ontology, df.getOWLClassAssertionAxiom(CRSClass, crsIndividual));

//            Add the projection properties
//            EPSG Code
//            final OWLLiteral epsgLiteral = df.getOWLLiteral(code, OWL2Datatype.XSD_INTEGER);
//            final AddAxiom epsgCodeAxiom = new AddAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(EPSGCodeProperty, crsIndividual, epsgLiteral));

//            WKT String
            final OWLLiteral wktLiteral = df.getOWLLiteral(crs.toString(), OWL2Datatype.RDFS_LITERAL);
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
            changes.add(classAssertionAxiom);
//            changes.add(epsgCodeAxiom);
            changes.add(wktStringAxiom);
            changes.add(projectedAxiom);

        }

        return changes;
    }
}
