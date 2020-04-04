package com.nickrobison.trestle.reasoner;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.nickrobison.metrician.MetricianModule;
import com.nickrobison.trestle.graphdb.GraphDBOntology;
import com.nickrobison.trestle.graphdb.GraphDBOntologyModule;
import com.nickrobison.trestle.ontology.ConnectionProperties;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.TrestleOntologyModule;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.nickrobison.trestle.reasoner.caching.TrestleCacheModule;
import com.nickrobison.trestle.reasoner.engines.EngineModule;
import com.nickrobison.trestle.reasoner.exceptions.InvalidOntologyName;
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
    public static final String DEFAULT_NAME = "trestle";
    private final boolean metricsEnabled;
    private final boolean cachingEnabled;
    private final boolean mergeEnabled;
    private final boolean eventEnabled;
    private final boolean trackEnabled;
    private final TrestleBuilder builder;

    TrestleModule(TrestleBuilder builder, boolean metricsEnabled, boolean cachingEnabled, boolean mergeEnabled, boolean eventEnabled, boolean trackEnabled) {
        logger.debug("Building Trestle Module");
        this.builder = builder;
        this.metricsEnabled = metricsEnabled;
        this.cachingEnabled = cachingEnabled;
        this.mergeEnabled = mergeEnabled;
        this.eventEnabled = eventEnabled;
        this.trackEnabled = trackEnabled;
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
        install(new EngineModule(mergeEnabled, eventEnabled, trackEnabled));
        // install the ontologies
        install(new TrestleOntologyModule());
        install(new GraphDBOntologyModule());

    }

    @OntologyName
    @Provides
    public String ontologyName() {
        final String ontologyName = builder.ontologyName.orElse(DEFAULT_NAME);
        try {
            validateOntologyName(ontologyName);
            return ontologyName;
        } catch (InvalidOntologyName e) {
            logger.error("{} is an invalid ontology name", ontologyName, e);
            throw new IllegalArgumentException("invalid ontology name", e);
        }
    }

    @Provides
    @ReasonerPrefix
    public String providePrefix() {
        //        If not specified, use the default Trestle prefix
        return builder.pm.getDefaultPrefix();
    }

    @Provides
    public DefaultPrefixManager providePrefixManager() {
        return this.builder.pm.getDefaultPrefixManager();
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

    @Provides
    ConnectionProperties provideConnectionProperties(Config config) {
        return new ConnectionProperties(this.builder.connectionString.orElseGet(() -> config.getString("ontology.connectionString")),
                this.builder.username.orElseGet(() -> config.getString("ontology.username")),
                this.builder.password.orElseGet(() -> config.getString("ontology.password")));
    }

    /**
     * Validates the ontology name to make sure it doesn't include unsupported characters
     *
     * @param ontologyName - String to validate
     * @throws InvalidOntologyName - Exception thrown if the name is invalid
     */
    private static void validateOntologyName(String ontologyName) throws InvalidOntologyName {
        if (ontologyName.contains("-")) {
            throw new InvalidOntologyName("-");
        } else if (ontologyName.length() > 90) {
            throw new InvalidOntologyName(ontologyName.length());
        }
    }
}
