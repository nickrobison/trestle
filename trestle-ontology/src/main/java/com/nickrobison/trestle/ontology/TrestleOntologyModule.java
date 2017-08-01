package com.nickrobison.trestle.ontology;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class TrestleOntologyModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(TrestleOntologyModule.class);

    private final OntologyBuilder builder;

    public TrestleOntologyModule(OntologyBuilder builder) {
        this.builder = builder;
    }

    @Override
    protected void configure() {
//        Not used. I think?
    }

    @Provides
    @Singleton
    public ITrestleOntology provideOntology() {
        try {
            return this.builder.build();
        } catch (OWLOntologyCreationException e) {
            logger.error("Cannot initialize ontology", e);
            throw new IllegalStateException(e);
        }
    }
}
