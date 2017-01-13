package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 1/10/17.
 */
@Tag("integration")
@Tag("GraphDB")
public class GraphDBOntologyTest extends OntologyTest {
    @Override
    void setupOntology() throws OWLOntologyCreationException {
        ontology = new OntologyBuilder()
                .fromInputStream(inputStream)
//                .withDBConnection("graphdb", "", "")
                .name("trestle")
                .build();
        ontology.initializeOntology();
    }

    @Override
    void shutdownOntology() {
        ontology.close(true);
    }

    @Override
    @Test
    public void testByteParsing() throws MissingOntologyEntity {
        int smallInt = 4321;
        int bigInt = Integer.MAX_VALUE;
        int negativeInt = Integer.MIN_VALUE;
        long smallLong = 4321;
        long negativeLong = -4321;
        long negativeBigLong = Long.MIN_VALUE;
        long bigLong = Long.MAX_VALUE;
        Double bigFloat = 4321.43;

        final OWLNamedIndividual long_test = df.getOWLNamedIndividual(IRI.create("trestle:", "long_test"));
        OWLDataProperty aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_small"));
        OWLLiteral owlLiteral = df.getOWLLiteral(Long.toString(smallLong), OWL2Datatype.XSD_LONG);
        final OWLClass owlCl = df.getOWLClass(IRI.create("trestle:", "test"));
        OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        Optional<Set<OWLLiteral>> individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Big long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_big"));
        owlLiteral = df.getOWLLiteral(Long.toString(bigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(bigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_small"));
        owlLiteral = df.getOWLLiteral(Integer.toString(smallInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(smallInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be int");

        //        Big Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_big"));
        owlLiteral = df.getOWLLiteral(Integer.toString(bigInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(bigInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be int");

        //        Negative Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_int"));
        owlLiteral = df.getOWLLiteral(Integer.toString(negativeInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(negativeInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Double
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "float"));
        owlLiteral = df.getOWLLiteral(Double.toString(bigFloat), OWL2Datatype.XSD_DECIMAL);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Double.toString(bigFloat), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_DECIMAL, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_big_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeBigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeBigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");
    }
}
