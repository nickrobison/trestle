package com.nickrobison.trestle.ontology;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

public class TrestleOntologyModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(TrestleOntologyModule.class);

    private final ITrestleOntology ontology;
    private final String prefix;

    public TrestleOntologyModule(OntologyBuilder builder, String prefix) {
        this.prefix = prefix;
        try {
            this.ontology = builder.build();
        } catch (OWLOntologyCreationException e) {
            logger.error("Cannot initialize ontology", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void configure() {
//        Not used. I think?
    }

    @Provides
    @Singleton
    public ITrestleOntology provideOntology() {
        return this.ontology;
    }

    @Provides
    @Singleton
    public QueryBuilder provideQueryBuilder() {
        return this.ontology.getUnderlyingQueryBuilder();
    }

    @Provides
    @Singleton
    public PrefixManager providePrefixManager() {
        return this.ontology.getUnderlyingPrefixManager();
    }

    @Provides
    @Named("reasonerPrefix")
    public String providePrefix() {
        return this.prefix;
    }
}
