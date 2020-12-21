package com.nickrobison.trestle.ontology.utils;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 1/11/17.
 */
public class SharedOntologyFunctions {

    /**
     * Parse OWLOntology to InputStream
     * Currently fixed to use the RDF/XML Format
     * @param ontology - OWLOntology
     * @return - InputStream of OWLOntology
     * @throws OWLOntologyStorageException - Throws an error when it fails
     */
    public static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Filter the set of all OWLDataProperties on a given individual, down to the requested properties
     * @param requestedProperties - List of OWLDataProperties to return
     * @param individualProperties - Set of OWLDataPropertyAssertionAxioms
     * @return Set of filtered OWLDataPropertyAssertionAxioms matching given list
     */
//    TODO(nrobison): Eventually, we should change the function so that we filter the lists on the open repository connection, instead of returning everything
    public static Set<OWLDataPropertyAssertionAxiom> filterIndividualDataProperties(List<OWLDataProperty> requestedProperties, Set<OWLDataPropertyAssertionAxiom> individualProperties) {
        Set<OWLDataPropertyAssertionAxiom> propertyAxioms;
        if (individualProperties.size() != requestedProperties.size()) {
            propertyAxioms = individualProperties
                    .stream()
                    .filter(property -> requestedProperties.contains(property.getProperty().asOWLDataProperty()))
                    .collect(Collectors.toSet());
        } else {
            propertyAxioms = individualProperties;
        }
        return propertyAxioms;
    }
}
