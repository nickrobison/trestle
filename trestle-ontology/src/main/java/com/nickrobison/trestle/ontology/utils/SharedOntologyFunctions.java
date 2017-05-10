package com.nickrobison.trestle.ontology.utils;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;

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

    /**
     * Given a TrestleResultSet representing all Facts from a given individual.
     * Return as a Set of OWLDataPropertyAssertionAxioms
     * @param df - OWLDataFactory to use
     * @param resultSet - TrestleResultSet of Individual Facts
     * @return - Set of OWLDataPropertyAssertionAxioms
     */
    public static Set<OWLDataPropertyAssertionAxiom> getDataPropertiesFromIndividualFacts(OWLDataFactory df, TrestleResultSet resultSet) {
        return resultSet.getResults().stream().map(result -> df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create(result.getIndividual("property").orElseThrow(() -> new RuntimeException("Unable to get property")).toStringID())),
                df.getOWLNamedIndividual(IRI.create(result.getIndividual("individual").orElseThrow(() -> new RuntimeException("Unable to get individual")).toStringID())),
                result.getLiteral("object").orElseThrow(() -> new RuntimeException("Unable to get object"))))
                .collect(Collectors.toSet());
    }
}
