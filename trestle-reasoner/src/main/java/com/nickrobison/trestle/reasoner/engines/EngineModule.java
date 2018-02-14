package com.nickrobison.trestle.reasoner.engines;

import com.google.inject.PrivateModule;
import com.nickrobison.trestle.reasoner.engines.events.EventEngineImpl;
import com.nickrobison.trestle.reasoner.engines.events.EventEngineNoOp;
import com.nickrobison.trestle.reasoner.engines.events.TrestleEventEngine;
import com.nickrobison.trestle.reasoner.engines.merge.MergeEngineImpl;
import com.nickrobison.trestle.reasoner.engines.merge.MergeEngineNoOp;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeEngine;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.object.TrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.TrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.containment.ContainmentEngineImpl;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngineImpl;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineModule extends PrivateModule {

    private static final Logger logger = LoggerFactory.getLogger(EngineModule.class);

    private final boolean mergedEnabled;
    private final boolean eventEnabled;

    public EngineModule(boolean mergeEnabled, boolean eventEnabled) {
        this.mergedEnabled = mergeEnabled;
        this.eventEnabled = eventEnabled;
    }

    @Override
    protected void configure() {
        logger.debug("Configuring Engine Module");
        bind(ITrestleObjectReader.class).to(TrestleObjectReader.class).asEagerSingleton();
        bind(ITrestleObjectWriter.class).to(TrestleObjectWriter.class).asEagerSingleton();
        bind(IndividualEngine.class).asEagerSingleton();
        bind(SpatialEngine.class).asEagerSingleton();
        bind(TemporalEngine.class).asEagerSingleton();
        bind(EqualityEngine.class).to(EqualityEngineImpl.class).asEagerSingleton();
        bind(ContainmentEngine.class).to(ContainmentEngineImpl.class).asEagerSingleton();

        //        Event Engine
        if (eventEnabled) {
            bind(TrestleEventEngine.class).to(EventEngineImpl.class);
        } else {
            bind(TrestleEventEngine.class).to(EventEngineNoOp.class);
        }

        //        Merge Engine
        if (this.mergedEnabled) {
            bind(TrestleMergeEngine.class).to(MergeEngineImpl.class);
        } else {
            bind(TrestleMergeEngine.class).to(MergeEngineNoOp.class);
        }


//        Expose some things
        expose(ITrestleObjectWriter.class);
        expose(ITrestleObjectReader.class);
        expose(SpatialEngine.class);
        expose(TemporalEngine.class);
        expose(IndividualEngine.class);
        expose(TrestleEventEngine.class);
        expose(TrestleMergeEngine.class);
    }
}
