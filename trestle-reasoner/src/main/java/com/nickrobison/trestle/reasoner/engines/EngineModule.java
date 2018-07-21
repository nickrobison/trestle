package com.nickrobison.trestle.reasoner.engines;

import com.google.inject.PrivateModule;
import com.nickrobison.trestle.reasoner.engines.collection.CollectionEngine;
import com.nickrobison.trestle.reasoner.engines.collection.ITrestleCollectionEngine;
import com.nickrobison.trestle.reasoner.engines.events.EventEngineImpl;
import com.nickrobison.trestle.reasoner.engines.events.EventEngineNoOp;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.exporter.DataExportEngine;
import com.nickrobison.trestle.reasoner.engines.exporter.ITrestleDataExporter;
import com.nickrobison.trestle.reasoner.engines.merge.MergeEngineImpl;
import com.nickrobison.trestle.reasoner.engines.merge.MergeEngineNoOp;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.object.TrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.TrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.relations.RelationTracker;
import com.nickrobison.trestle.reasoner.engines.relations.RelationTrackerImpl;
import com.nickrobison.trestle.reasoner.engines.relations.RelationTrackerNoOp;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngineImpl;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngineImpl;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

public class EngineModule extends PrivateModule {

    private static final Logger logger = LoggerFactory.getLogger(EngineModule.class);

    private final boolean mergedEnabled;
    private final boolean eventEnabled;
    private final boolean trackEnabled;

    public EngineModule(boolean mergeEnabled, boolean eventEnabled, boolean trackEnabled) {
        this.mergedEnabled = mergeEnabled;
        this.eventEnabled = eventEnabled;
        this.trackEnabled = trackEnabled;
    }

    @Override
    protected void configure() {
        logger.debug("Configuring Engine Module");
        bind(ITrestleObjectReader.class)
                .to(TrestleObjectReader.class)
                .asEagerSingleton();
        bind(ITrestleObjectWriter.class)
                .to(TrestleObjectWriter.class)
                .asEagerSingleton();
        bind(ITrestleCollectionEngine.class)
                .to(CollectionEngine.class)
                .in(Singleton.class);
        bind(ITrestleDataExporter.class)
                .to(DataExportEngine.class)
                .in(Singleton.class);
        bind(IndividualEngine.class).asEagerSingleton();
        bind(TemporalEngine.class).asEagerSingleton();
        bind(SpatialEngine.class).in(Singleton.class);
        bind(EqualityEngine.class)
                .to(EqualityEngineImpl.class)
                .in(Singleton.class);
        bind(ContainmentEngine.class)
                .to(ContainmentEngineImpl.class)
                .in(Singleton.class);
        bind(AggregationEngine.class).asEagerSingleton();

        //        Event Engine
        if (eventEnabled) {
            bind(TrestleEventEngine.class)
                    .to(EventEngineImpl.class)
                    .asEagerSingleton();
        } else {
            bind(TrestleEventEngine.class)
                    .to(EventEngineNoOp.class)
                    .asEagerSingleton();
        }

        //        Merge Engine
        if (this.mergedEnabled) {
            bind(TrestleMergeEngine.class)
                    .to(MergeEngineImpl.class)
                    .asEagerSingleton();
        } else {
            bind(TrestleMergeEngine.class)
                    .to(MergeEngineNoOp.class)
                    .asEagerSingleton();
        }

//        Relationship Tracker
        if (this.trackEnabled) {
            bind(RelationTracker.class)
                    .to(RelationTrackerImpl.class)
                    .asEagerSingleton();
        } else {
            bind(RelationTracker.class)
                    .to(RelationTrackerNoOp.class)
                    .asEagerSingleton();
        }


//        Expose some things
        expose(ITrestleObjectWriter.class);
        expose(ITrestleObjectReader.class);
        expose(ITrestleCollectionEngine.class);
        expose(ITrestleDataExporter.class);
        expose(SpatialEngine.class);
        expose(AggregationEngine.class);
        expose(TemporalEngine.class);
        expose(IndividualEngine.class);
        expose(TrestleEventEngine.class);
        expose(TrestleMergeEngine.class);
    }
}
