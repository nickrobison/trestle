package com.nickrobison.trestle.ontology;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by nickrobison on 6/30/20.
 */
public class DataPropertiesTest extends AbstractRDF4JTest {

    @Test
    void testNoDataProperties() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final RepositoryResult<Object> result = MockStatementIterator.mockResult();
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).then(answer -> result);

        final TestSubscriber<OWLLiteral> subscriber = new TestSubscriber<>();

        final Flowable<OWLLiteral> allDataPropertiesForIndividual = ontology.getIndividualDataProperty(individual, df.getOWLDataProperty(org.semanticweb.owlapi.model.IRI.create(":size")));
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(0);
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testSingleDataProperty() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");

        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":size");
        final Literal dataLiteral = vf.createLiteral(10);
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, dataLiteral);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt);
        Mockito.when(connection.getStatements(Mockito.any(), Mockito.any(), Mockito.isNull())).then(answer -> result);

        final TestSubscriber<OWLLiteral> subscriber = new TestSubscriber<>();

        final Flowable<OWLLiteral> allDataPropertiesForIndividual = ontology.getIndividualDataProperty(individual, df.getOWLDataProperty(org.semanticweb.owlapi.model.IRI.create(":size")));
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValue(df.getOWLLiteral("10", OWL2Datatype.XSD_INT));
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testDataPropertyException() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":size");
        final Literal literal = vf.createLiteral(10);
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, literal);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, null);
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).then(answer -> result);

        final TestSubscriber<OWLLiteral> subscriber = new TestSubscriber<>();

        final Flowable<OWLLiteral> allDataPropertiesForIndividual = ontology.getIndividualDataProperty(individual, df.getOWLDataProperty(org.semanticweb.owlapi.model.IRI.create(":size")));
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertError(NullPointerException.class);
        subscriber.assertValueCount(1);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
    }

    @Test
    void testMalformedDataProperty() {

    }

    @Test
    void testStatementException() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":related-to");
        final Literal literal = vf.createLiteral(10);
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, literal);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, null);
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> ontology.getIndividualObjectProperty(individual, df.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(":related-to"))), "Should throw exception");
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
    }

}
