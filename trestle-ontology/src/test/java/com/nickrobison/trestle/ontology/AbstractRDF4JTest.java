package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.utils.RDF4JLiteralFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Optional;

/**
 * Created by nickrobison on 6/30/20.
 */
public class AbstractRDF4JTest {
    protected static RDF4JOntology ontology;
    protected static Repository repository;
    protected static OWLOntology owlOntology;
    protected static RepositoryConnection connection;
    protected static RDF4JLiteralFactory factory;
    protected static OWLDataFactory df;
    protected static SimpleValueFactory vf;

    @BeforeAll
    static void setup() {
        owlOntology = OntologyBuilder.loadOntology(Optional.empty(), Optional.empty());
        repository = Mockito.mock(Repository.class);
        connection = Mockito.mock(RepositoryConnection.class);
        factory = Mockito.spy(new RDF4JLiteralFactory(OWLManager.getOWLDataFactory(), SimpleValueFactory.getInstance()));
        df = factory.getDataFactory();
        vf = factory.getValueFactory();
        ontology = Mockito.spy(new TestRDF4JOntology("test-ontology", repository, owlOntology, OntologyBuilder.createDefaultPrefixManager(), factory) {
            @Override
            protected RepositoryConnection getThreadConnection() {
                return connection;
            }

            @Override
            public void commitTransaction(boolean write) {
            }
        });
    }

    @AfterEach
    void reset() {
        Mockito.reset(repository, connection, ontology, factory);
    }
}
