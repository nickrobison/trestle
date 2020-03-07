package com.nickrobison.trestle.graphdb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.typesafe.config.Config;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class GraphDBOntologyModule extends AbstractModule {
    public static final String CONFIG_PATH = "trestle.ontology.graphdb";

    @Override
    protected void configure() {
        final Multibinder<ITrestleOntology> ontologyBinder = Multibinder.newSetBinder(binder(), ITrestleOntology.class);
        ontologyBinder.addBinding().to(GraphDBOntology.class);
    }

    @Provides
    GraphDBOntology provideOntology(Config config, @OntologyName String name, OWLOntology ontology, DefaultPrefixManager pm) {
        final Config path = config.getConfig(CONFIG_PATH);
        return new GraphDBOntology(name,
                path.getString("connectionString"), path.getString("username"), path.getString("password"),
                ontology, pm);
    }
}
