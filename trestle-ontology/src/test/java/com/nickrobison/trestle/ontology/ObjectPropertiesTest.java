package com.nickrobison.trestle.ontology;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by nickrobison on 4/19/20.
 */
public class ObjectPropertiesTest extends AbstractRDF4JTest {

    @Test
    void testNoObjectProperties() {

        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        // vf.createStatement(vf.createIRI("http://hello"), vf.createIRI("http://hello2"), vf.createLiteral(1))
        final RepositoryResult<Object> result = MockStatementIterator.mockResult();
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).then(answer -> result);

        final TestSubscriber<OWLObjectPropertyAssertionAxiom> subscriber = new TestSubscriber<>();

        final Flowable<OWLObjectPropertyAssertionAxiom> objectProperties = ontology.getIndividualObjectProperty(individual, df.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(":related-to")));
        objectProperties.subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(0);
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testSingleObjectProperty() {

        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");

        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":related-to");
        final IRI objectIRI = vf.createIRI("http://hello");
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, objectIRI);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt);
        Mockito.when(connection.getStatements(Mockito.any(), Mockito.any(), Mockito.isNull())).then(answer -> result);

        final TestSubscriber<OWLObjectPropertyAssertionAxiom> subscriber = new TestSubscriber<>();

        final Flowable<OWLObjectPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getIndividualObjectProperty(individual, df.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(":related-to")));
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValue(df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty("", ":related-to"), individual, df.getOWLNamedIndividual("http://hello")));
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testObjectPropertyException() {

        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":related-to");
        final IRI objectIRI = vf.createIRI("http://hello");
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, objectIRI);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, null);
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).then(answer -> result);

        final TestSubscriber<OWLObjectPropertyAssertionAxiom> subscriber = new TestSubscriber<>();

        final Flowable<OWLObjectPropertyAssertionAxiom> objectProperties = ontology.getIndividualObjectProperty(individual, df.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(":related-to")));
        objectProperties.subscribe(subscriber);

        subscriber.assertError(NullPointerException.class);
        subscriber.assertValueCount(1);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
    }

    @Test
    void testStatementException() {

        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final IRI individualIRI = vf.createIRI(individual.toStringID());
        final IRI propertyIRI = vf.createIRI(":related-to");
        final IRI objectIRI = vf.createIRI("http://hello");
        final Statement stmt = vf.createStatement(individualIRI, propertyIRI, objectIRI);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, null);
        Mockito.when(connection.getStatements(Mockito.any(IRI.class), Mockito.any(), Mockito.any())).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> ontology.getIndividualObjectProperty(individual, df.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(":related-to"))), "Should throw exception");
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
    }


}
