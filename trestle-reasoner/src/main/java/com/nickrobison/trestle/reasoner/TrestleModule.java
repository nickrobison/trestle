package com.nickrobison.trestle.reasoner;

import com.google.inject.AbstractModule;
import com.nickrobison.metrician.MetricianModule;

import com.nickrobison.trestle.reasoner.caching.TrestleCacheModule;
import com.nickrobison.trestle.reasoner.events.EventEngine;
import com.nickrobison.trestle.reasoner.events.EventEngineImpl;
import com.nickrobison.trestle.reasoner.events.EventEngineNoOp;
import com.nickrobison.trestle.reasoner.merge.MergeEngineImpl;
import com.nickrobison.trestle.reasoner.merge.MergeEngineNoOp;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeEngine;
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

    TrestleModule(boolean metricsEnabled, boolean cachingEnabled, boolean mergeEnabled, boolean eventEnabled) {
        logger.debug("Building Trestle Module");
        this.metricsEnabled = metricsEnabled;
        this.cachingEnabled = cachingEnabled;
        this.mergeEnabled = mergeEnabled;
        this.eventEnabled = eventEnabled;

    }

    @Override
    protected void configure() {
        install(new MetricianModule(metricsEnabled));
        install(new TrestleCacheModule(cachingEnabled));
//        Merge Engine
        if (mergeEnabled) {
            bind(TrestleMergeEngine.class).to(MergeEngineImpl.class);
        } else {
            bind(TrestleMergeEngine.class).to(MergeEngineNoOp.class);
        }

//        Event Engine
        if (eventEnabled) {
            bind(EventEngine.class).to(EventEngineImpl.class);
        } else {
            bind(EventEngine.class).to(EventEngineNoOp.class);
        }

    }
}
