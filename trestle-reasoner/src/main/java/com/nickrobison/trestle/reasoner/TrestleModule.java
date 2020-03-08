package com.nickrobison.trestle.reasoner;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.nickrobison.metrician.MetricianModule;
import com.nickrobison.trestle.graphdb.GraphDBOntology;
import com.nickrobison.trestle.graphdb.GraphDBOntologyModule;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.TrestleOntologyModule;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.nickrobison.trestle.reasoner.caching.TrestleCacheModule;
import com.nickrobison.trestle.reasoner.engines.EngineModule;
import com.nickrobison.trestle.reasoner.parser.TrestleParserModule;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nickrobison.trestle.reasoner.utils.ConfigValidator.ValidateConfig;

/**
 * Created by nrobison on 4/12/17.
 */
public class TrestleModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(TrestleModule.class);
    private final boolean metricsEnabled;
    private final boolean cachingEnabled;
    private final boolean mergeEnabled;
    private final boolean eventEnabled;
    private final TrestlePrefixManager prefixManager;

    TrestleModule(TrestlePrefixManager prefixManager, boolean metricsEnabled, boolean cachingEnabled, boolean mergeEnabled, boolean eventEnabled) {
        logger.debug("Building Trestle Module");
        this.prefixManager = prefixManager;
        this.metricsEnabled = metricsEnabled;
        this.cachingEnabled = cachingEnabled;
        this.mergeEnabled = mergeEnabled;
        this.eventEnabled = eventEnabled;
    }

    @Override
    protected void configure() {
//        Bind the executor factory
        install(new FactoryModuleBuilder()
                .implement(TrestleExecutorService.class, TrestleExecutorService.class)
                .build(TrestleExecutorFactory.class));
        install(new TrestleParserModule());
        install(new MetricianModule(metricsEnabled));
        install(new TrestleCacheModule(cachingEnabled));
        install(new EngineModule(mergeEnabled, eventEnabled));

        // install the ontologies
        install(new TrestleOntologyModule());
        install(new GraphDBOntologyModule());
    }

    @OntologyName
    @Provides
    public String ontologyName() {
        return "trestle";
    }

    @Provides
    @ReasonerPrefix
    public String providePrefix() {
        return this.prefixManager.getDefaultPrefix();
    }

    @Provides
    public DefaultPrefixManager providePrefixManager() {
        return this.prefixManager.getDefaultPrefixManager();
    }

    @Provides
    Class<? extends ITrestleOntology> provideOntologyClass() {
        return GraphDBOntology.class;
    }

    @Provides
    @Singleton
    Config provideConfig() {
        final Config trestleConfig = ConfigFactory.load().getConfig("trestle");
        ValidateConfig(trestleConfig);
        return trestleConfig;
    }
}
