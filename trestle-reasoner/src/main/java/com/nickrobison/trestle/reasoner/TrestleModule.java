package com.nickrobison.trestle.reasoner;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.nickrobison.metrician.MetricianModule;
import com.nickrobison.trestle.reasoner.caching.TrestleCacheModule;
import com.nickrobison.trestle.reasoner.engines.EngineModule;
import com.nickrobison.trestle.reasoner.parser.TrestleParserModule;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 4/12/17.
 */
public class TrestleModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(TrestleModule.class);
    private final boolean metricsEnabled;
    private final boolean cachingEnabled;
    private final boolean mergeEnabled;
    private final boolean eventEnabled;
    private final boolean trackEnabled;

    TrestleModule(boolean metricsEnabled, boolean cachingEnabled, boolean mergeEnabled, boolean eventEnabled, boolean trackEnabled) {
        logger.debug("Building Trestle Module");
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
    }
}
