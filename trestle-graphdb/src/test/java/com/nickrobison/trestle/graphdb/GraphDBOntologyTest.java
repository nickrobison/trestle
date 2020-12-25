package com.nickrobison.trestle.graphdb;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.testing.OntologyTest;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 1/10/17.
 */
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
        return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
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
    public void testByteParsing() {
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
        final TrestleTransaction t1 = this.ontology.createandOpenNewTransaction(true);
        OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t1)).blockingAwait();
        List<OWLLiteral> individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");

//        Big long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_big"));
        owlLiteral = df.getOWLLiteral(Long.toString(bigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t2 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t2)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Long.toString(bigLong), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");

//        Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_small"));
        owlLiteral = df.getOWLLiteral(Integer.toString(smallInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t3 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t3)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Integer.toString(smallInt), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be int");

        //        Big Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_big"));
        owlLiteral = df.getOWLLiteral(Integer.toString(bigInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t4 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t4)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Integer.toString(bigInt), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be int");

        //        Negative Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_int"));
        owlLiteral = df.getOWLLiteral(Integer.toString(negativeInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t5 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t5)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Integer.toString(negativeInt), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");

        //        Double
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "float"));
        owlLiteral = df.getOWLLiteral(Double.toString(bigFloat), OWL2Datatype.XSD_DECIMAL);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t6 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t6)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Double.toString(bigFloat), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_DECIMAL, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t7 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t7)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Long.toString(negativeLong), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");

        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_big_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeBigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        final TrestleTransaction t8 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom).andThen(ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral))).doOnComplete(() -> this.ontology.returnAndCommitTransaction(t8)).blockingAwait();
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong).toList().blockingGet();
        assertEquals(Long.toString(negativeBigLong), individualDataProperty.get(0).getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get(0).getDatatype().getBuiltInDatatype(), "Should be long");
    }
}
