package com.nickrobison.trestle.utils;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by nrobison on 1/11/17.
 */
public class SharedOntologyFunctions {

    /**
     * Parse OWLOntology to InputStream
     * Currently fixed to use the RDF/XML Format
     * @param ontology - OWLOntology
     * @return - InputStream of OWLOntology
     * @throws OWLOntologyStorageException
     */
    public static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
