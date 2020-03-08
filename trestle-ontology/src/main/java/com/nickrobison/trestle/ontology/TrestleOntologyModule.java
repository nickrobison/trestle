package com.nickrobison.trestle.ontology;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class TrestleOntologyModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(TrestleOntologyModule.class);

//    private final ITrestleOntology ontology;

    public TrestleOntologyModule() {
    }

    @Override
    protected void configure() {
        bind(OWLOntology.class).toProvider(OWLOntologyProvider.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public ITrestleOntology provideOntology(Injector injector, Class<? extends ITrestleOntology> desiredClass) {
        logger.debug("Attempting to create {} ontology", desiredClass.getName());
        return (ITrestleOntology) injector.getAllBindings().keySet()
                .stream()
                .filter(key -> desiredClass.isAssignableFrom(key.getTypeLiteral().getRawType()))
                .map(injector::getInstance)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find %s ontology", desiredClass.getName())));

    }

    @Provides
    @Singleton
    public QueryBuilder provideQueryBuilder(ITrestleOntology ontology) {
        return ontology.getUnderlyingQueryBuilder();
    }
}
