package com.nickrobison.trestle.ontology;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.*;

/**
 * Created by nickrobison on 12/22/20.
 */
public class IndividualTests extends AbstractRDF4JTest {

    @Test
    void testIndividualCreation() {
        final IRI individual = IRI.create(":test-individual");
        final IRI clazz = IRI.create(":test-class");
        ontology.createIndividual(df.getOWLNamedIndividual(individual), df.getOWLClass(clazz))
                .test()
                .assertComplete()
                .assertNoErrors();
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(true));
    }

    @Test
    void testIndividualException() {
        final IRI individual = IRI.create(":test-individual");
        final IRI clazz = IRI.create(":test-class");
        final OWLClassAssertionAxiom axiom = df.getOWLClassAssertionAxiom(df.getOWLClass(clazz), df.getOWLNamedIndividual(individual));

        final org.eclipse.rdf4j.model.IRI cIRI = vf.createIRI(ontology.getFullIRIString(axiom.getClassExpression().asOWLClass()));
        final org.eclipse.rdf4j.model.IRI iIRI = vf.createIRI(ontology.getFullIRIString(axiom.getIndividual().asOWLNamedIndividual()));

        Mockito.doThrow(RepositoryException.class).when(connection).add(Mockito.eq(iIRI), Mockito.eq(RDF.TYPE), Mockito.eq(cIRI));
        ontology.createIndividual(individual, clazz)
                .test()
                .assertError(RepositoryException.class);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(true));
    }

    @Test
    void testIndividualRemoval() {
        final IRI iri = IRI.create(":test-individual");
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(iri);
        final org.eclipse.rdf4j.model.IRI iIRi = vf.createIRI(ontology.getFullIRIString(individual));
        ontology.removeIndividual(individual)
                .test()
                .assertComplete()
                .assertNoErrors();
        Mockito.verify(connection, Mockito.times(1)).remove(Mockito.eq(iIRi), Mockito.eq(null), Mockito.eq(null));
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(true));
    }

    @Test
    void testIndividualRemovalException() {
        final IRI iri = IRI.create(":test-individual");
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(iri);
        final org.eclipse.rdf4j.model.IRI iIRi = vf.createIRI(ontology.getFullIRIString(individual));
        Mockito.doThrow(RepositoryException.class).when(connection).remove(Mockito.eq(iIRi), Mockito.eq(null), Mockito.eq(null));
        ontology.removeIndividual(individual)
                .test()
                .assertError(RepositoryException.class);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(true));
    }

    @Test
    void testClassAssociation() {
        final IRI superClass = IRI.create(":test-super");
        final IRI subClass = IRI.create(":test-sub");
        final OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass(subClass), df.getOWLClass(superClass));
        ontology.associateOWLClass(axiom)
                .test()
                .assertComplete()
                .assertNoErrors();


        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(true));
    }

    @Test
    void testClassAssociationException() {
        final IRI superClass = IRI.create(":test-super");
        final IRI subClass = IRI.create(":test-sub");
        final OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass(subClass), df.getOWLClass(superClass));

        final org.eclipse.rdf4j.model.IRI subIRI = vf.createIRI(ontology.getFullIRIString(axiom.getSubClass().asOWLClass()));
        final org.eclipse.rdf4j.model.IRI superIRI = vf.createIRI(ontology.getFullIRIString(axiom.getSuperClass().asOWLClass()));

        Mockito.doThrow(RepositoryException.class).when(connection).add(Mockito.eq(subIRI), Mockito.eq(RDFS.SUBCLASSOF), Mockito.eq(superIRI));
        ontology.associateOWLClass(df.getOWLClass(subClass), df.getOWLClass(superClass))
                .test()
                .assertError(RepositoryException.class);
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(true));
    }

    @Test
    void testCreateObjectProperty() {
        final IRI iri = IRI.create(":has-object-property");
        final OWLObjectProperty property = df.getOWLObjectProperty(iri);
        final org.eclipse.rdf4j.model.IRI pIri = vf.createIRI(ontology.getFullIRIString(property));
        ontology.createProperty(property)
                .test()
                .assertComplete()
                .assertNoErrors();

        Mockito.verify(connection, Mockito.times(1)).add(Mockito.eq(pIri), Mockito.eq(RDF.TYPE), Mockito.eq(OWL.OBJECTPROPERTY));
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(true));
    }

    @Test
    void testCreateDataProperty() {
        final IRI iri = IRI.create(":has-object-property");
        final OWLDataProperty property = df.getOWLDataProperty(iri);
        final org.eclipse.rdf4j.model.IRI pIri = vf.createIRI(ontology.getFullIRIString(property));
        ontology.createProperty(property)
                .test()
                .assertComplete()
                .assertNoErrors();

        Mockito.verify(connection, Mockito.times(1)).add(Mockito.eq(pIri), Mockito.eq(RDF.TYPE), Mockito.eq(OWL.DATATYPEPROPERTY));
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(true));
    }

    @Test
    void testCreatePropertyException() {
        final IRI iri = IRI.create(":has-object-property");
        final OWLDataProperty property = df.getOWLDataProperty(iri);
        final org.eclipse.rdf4j.model.IRI pIri = vf.createIRI(ontology.getFullIRIString(property));
        Mockito.doThrow(RepositoryException.class).when(connection).add(Mockito.eq(pIri), Mockito.eq(RDF.TYPE), Mockito.eq(OWL.DATATYPEPROPERTY));
        ontology.createProperty(property)
                .test()
                .assertError(RepositoryException.class);

        Mockito.verify(connection, Mockito.times(1)).add(Mockito.eq(pIri), Mockito.eq(RDF.TYPE), Mockito.eq(OWL.DATATYPEPROPERTY));
        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(true));
    }

    @Test
    void testContainsResource() {
        final OWLNamedIndividual individual = df.getOWLNamedIndividual(":test-individual");
        final org.eclipse.rdf4j.model.IRI iIRI = vf.createIRI(ontology.getFullIRIString(individual));

        Mockito.when(connection.hasStatement(Mockito.eq(iIRI), Mockito.eq(null), Mockito.eq(null), Mockito.eq(false))).thenReturn(true);

        ontology.containsResource(individual.getIRI())
                .test()
                .assertValue(true)
                .assertComplete()
                .assertNoErrors();
        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
    }

    @Test
    void getInstances() {
        final OWLNamedIndividual individual1 = df.getOWLNamedIndividual(":test-individual");
        final OWLNamedIndividual individual2 = df.getOWLNamedIndividual(":test-individual-2");

        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(individual1.toStringID());
        final org.eclipse.rdf4j.model.IRI iIRI2 = vf.createIRI(individual2.toStringID());
        final Statement stmt = vf.createStatement(individualIRI, iIRI2, iIRI2);
        final Statement stmt2 = vf.createStatement(iIRI2, iIRI2, iIRI2);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, stmt2);
        final OWLClass clazz = df.getOWLClass(":test-class");
        final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(ontology.getFullIRIString(clazz));

        Mockito.when(connection.getStatements(Mockito.isNull(), Mockito.eq(RDF.TYPE), Mockito.eq(classIRI))).then(answer -> result);
        ontology.getInstances(clazz, true)
                .test()
                .assertValueCount(2)
                .assertComplete()
                .assertNoErrors();

        Mockito.verify(ontology, Mockito.times(1)).commitTransaction(Mockito.eq(false));
        Mockito.verify(result, Mockito.times(1)).close();
    }

    @Test
    void getInstancesException() {
        final OWLNamedIndividual individual1 = df.getOWLNamedIndividual(":test-individual");

        final org.eclipse.rdf4j.model.IRI individualIRI = vf.createIRI(individual1.toStringID());
        final Statement stmt = vf.createStatement(individualIRI, individualIRI, individualIRI);
        final RepositoryResult<Object> result = MockStatementIterator.mockResult(stmt, null);
        final OWLClass clazz = df.getOWLClass(":test-class");
        final org.eclipse.rdf4j.model.IRI classIRI = vf.createIRI(ontology.getFullIRIString(clazz));

        Mockito.when(connection.getStatements(Mockito.isNull(), Mockito.eq(RDF.TYPE), Mockito.eq(classIRI))).then(answer -> result);
        ontology.getInstances(clazz, false)
                .test()
                .assertValueCount(1)
                .assertError(NullPointerException.class);

        Mockito.verify(ontology, Mockito.times(1)).unlockAndAbort(Mockito.eq(false));
        Mockito.verify(ontology, Mockito.times(0)).commitTransaction(Mockito.eq(false));
        Mockito.verify(result, Mockito.times(1)).close();
    }
}
