package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

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
        // TODO: Add this
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

    // Temporals and Facts

    @Test
    void testTemporals() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final OWLDataPropertyAssertionAxiom axiom = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(":exists-from"), individual, df.getOWLLiteral(LocalDate.of(1989, 3, 26).atStartOfDay(ZoneOffset.UTC).toString()));
        df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(":exists-to"), individual, df.getOWLLiteral(LocalDate.of(1989, 3, 26).atStartOfDay(ZoneOffset.UTC).toString()));

        final TrestleResultSet results = new TrestleResultSet(2,
                List.of("individual", "property", "object"),
                List.of(
                        new TrestleResult(Map.of("individual", individual, "property", df.getOWLNamedIndividual(":exists-from"), "object", axiom.getObject())),
                        new TrestleResult(Map.of("individual", individual, "property", df.getOWLNamedIndividual(":exists-to"), "object", df.getOWLLiteral(LocalDate.of(2020, 3, 26).atStartOfDay(ZoneOffset.UTC).toString())))
                ));

        Mockito.when(ontology.executeSPARQLResults(Mockito.anyString())).thenReturn(results);
        final TestSubscriber<OWLDataPropertyAssertionAxiom> subscriber = new TestSubscriber<>();
        final Flowable<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getTemporalsForIndividual(individual);
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertComplete();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(2);
        subscriber.assertValueAt(0, axiom);
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testFacts() {
        // Try a real fact and a fake fact
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final OWLDataPropertyAssertionAxiom axiom = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(":public-id"), individual, df.getOWLLiteral(42));
        df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(":exists-to"), individual, df.getOWLLiteral(LocalDate.of(1989, 3, 26).atStartOfDay(ZoneOffset.UTC).toString()));

        final TrestleResultSet results = new TrestleResultSet(2,
                List.of("individual", "property", "object"),
                List.of(
                        new TrestleResult(Map.of("individual", individual, "property", df.getOWLNamedIndividual(":public-id"), "object", axiom.getObject())),
                        new TrestleResult(Map.of("individual", individual, "property", df.getOWLDataProperty(":exists-to"), "object", df.getOWLLiteral(LocalDate.of(2020, 3, 26).atStartOfDay(ZoneOffset.UTC).toString())))
                ));

        Mockito.when(ontology.executeSPARQLResults(Mockito.anyString())).thenReturn(results);
        final TestSubscriber<OWLDataPropertyAssertionAxiom> subscriber = new TestSubscriber<>();
        final Flowable<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getFactsForIndividual(individual, LocalDate.of(1990, 5, 14).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), LocalDate.of(1990, 5, 14).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), false);
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertError(ClassCastException.class);
        subscriber.assertValueCount(1);
        subscriber.assertValueAt(0, axiom);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));

    }

    @Test
    void testNoFacts() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final TrestleResultSet results = new TrestleResultSet(0, List.of("nothing"));

        Mockito.when(ontology.executeSPARQLResults(Mockito.anyString())).thenReturn(results);
        final TestSubscriber<OWLDataPropertyAssertionAxiom> subscriber = new TestSubscriber<>();
        final Flowable<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual =ontology.getFactsForIndividual(individual, LocalDate.of(1990, 5, 14).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), LocalDate.of(1990, 5, 14).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(), false);
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertValueCount(0);
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void testConversionException() {

        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final TrestleResultSet results = new TrestleResultSet(1, List.of("nothing"), List.of(new TrestleResult(Map.of("nothing", df.getOWLLiteral("here")))));

        Mockito.when(ontology.executeSPARQLResults(Mockito.anyString())).thenReturn(results);
        final TestSubscriber<OWLDataPropertyAssertionAxiom> subscriber = new TestSubscriber<>();
        final Flowable<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getTemporalsForIndividual(individual);
        allDataPropertiesForIndividual.subscribe(subscriber);

        subscriber.assertError(RuntimeException.class);
        subscriber.assertValueCount(0);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
    }

}
