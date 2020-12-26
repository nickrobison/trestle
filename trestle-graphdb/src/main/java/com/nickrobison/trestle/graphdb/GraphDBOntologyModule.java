package com.nickrobison.trestle.graphdb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.nickrobison.trestle.ontology.ConnectionProperties;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.nickrobison.trestle.ontology.utils.RDF4JLiteralFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class GraphDBOntologyModule extends AbstractModule {

    @Override
    protected void configure() {
        final Multibinder<ITrestleOntology> ontologyBinder = Multibinder.newSetBinder(binder(), ITrestleOntology.class);
        ontologyBinder.addBinding().to(GraphDBOntology.class);
    }

    @Provides
    GraphDBOntology provideOntology(@OntologyName String name, OWLOntology ontology, DefaultPrefixManager pm, ConnectionProperties conn, RDF4JLiteralFactory factory) {
        return new GraphDBOntology(name,
                conn.getConnectionString(), conn.getUsername(), conn.getPassword(),
                ontology, pm, factory);
    }
}
