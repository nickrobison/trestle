package com.nickrobison.trestle.graphdb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.testing.OntologyTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 1/10/17.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@Tag("integration")
@Tag("GraphDB")
public class GraphDBOntologyTest extends OntologyTest {

    @Override
    protected void setupOntology() {
        final Injector injector = Guice.createInjector(new TestModule());
        ontology = getOntology(injector);
        ontology.initializeOntology();
    }

    @SuppressWarnings("unchecked")
    public static <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
        return (TypeLiteral<Set<T>>)TypeLiteral.get(Types.setOf(type));
    }

    private ITrestleOntology getOntology(Injector injector) {
        final Set<ITrestleOntology> ontologies = injector.getInstance(Key.get(setOf(ITrestleOntology.class)));
        return ontologies
                .stream()
                .filter(o -> o instanceof GraphDBOntology)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find underlying ontology"));
    }

    @Override
    protected void shutdownOntology() {
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
        @SuppressWarnings("WrapperTypeMayBePrimitive") Double bigFloat = 4321.43;

        final OWLNamedIndividual long_test = df.getOWLNamedIndividual(IRI.create("trestle:", "long_test"));
        OWLDataProperty aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_small"));
        OWLLiteral owlLiteral = df.getOWLLiteral(Long.toString(smallLong), OWL2Datatype.XSD_LONG);
        final OWLClass owlCl = df.getOWLClass(IRI.create("trestle:", "test"));
        OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        Set<OWLLiteral> individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Big long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_big"));
        owlLiteral = df.getOWLLiteral(Long.toString(bigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Long.toString(bigLong), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_small"));
        owlLiteral = df.getOWLLiteral(Integer.toString(smallInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Integer.toString(smallInt), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be int");

        //        Big Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_big"));
        owlLiteral = df.getOWLLiteral(Integer.toString(bigInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Integer.toString(bigInt), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be int");

        //        Negative Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_int"));
        owlLiteral = df.getOWLLiteral(Integer.toString(negativeInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Integer.toString(negativeInt), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Double
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "float"));
        owlLiteral = df.getOWLLiteral(Double.toString(bigFloat), OWL2Datatype.XSD_DECIMAL);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Double.toString(bigFloat), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_DECIMAL, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Long.toString(negativeLong), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_big_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeBigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = new HashSet<>(ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet());
        assertEquals(Long.toString(negativeBigLong), individualDataProperty.stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");
    }
}
