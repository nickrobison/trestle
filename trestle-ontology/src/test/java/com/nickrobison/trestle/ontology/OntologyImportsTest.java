package com.nickrobison.trestle.ontology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnloadableImportException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by nrobison on 1/27/17.
 */
@Tag("integration")
@Tag("local")
public class OntologyImportsTest {
    @Test
    public void testLocalImports()
    {
        /*
         check that an ontology import that is neither found locally
         nor resolvable on the web results in an UnloadableImportException
          */
        String ontString = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@base <http://test.org/import_test.owl> .\n" +
                "<http://test.org/import_test.owl> rdf:type owl:Ontology ;\n" +
                "owl:imports <http://test.org/unimportable_test.owl> .";
        InputStream is = new ByteArrayInputStream( ontString.getBytes() );
        assertThrows(UnloadableImportException.class, () -> {
            ITrestleOntology testOnt = new OntologyBuilder()
                    .withDBConnection("tdb:local", "", "")
                    .fromInputStream(is)
                    .name("test")
                    .build();
        });

        /*
        check that an ontology that is loadable only from a local file does
        not result in an UnloadableImportException
         */
        String ontString2 = "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@base <http://test.org/import_test.owl> .\n" +
                "<http://test.org/import_test.owl> rdf:type owl:Ontology ;\n" +
                "owl:imports <http://test.org/locally_importable_test.owl> .";
        InputStream is2 = new ByteArrayInputStream( ontString2.getBytes() );
        try {
            ITrestleOntology testOnt2 = new OntologyBuilder()
                    .fromInputStream(is2)
                    .name("test2")
                    .build();
        } catch (OWLOntologyCreationException e) {
            fail("Local ontology import not loadable from file");
        }
    }
}
