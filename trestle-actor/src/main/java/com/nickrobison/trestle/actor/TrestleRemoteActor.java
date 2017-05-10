package com.nickrobison.trestle.actor;

import akka.actor.AbstractActor;
import akka.actor.Status;
import akka.japi.pf.ReceiveBuilder;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.nickrobison.trestle.reasoner.TrestleReasoner;

import static com.nickrobison.trestle.actor.TrestleActorProtocol.*;


/**
 * Created by nrobison on 10/25/16.
 */
public class TrestleRemoteActor extends AbstractActor {

    public static final String TRESTLENAME = "trestle-actor";

    private static final Logger logger = LoggerFactory.getLogger(TrestleRemoteActor.class);

    private String name;

    private final TrestleReasoner reasoner;

    public TrestleRemoteActor() {
        logger.info("Calling reasoner with no params");
        this.name = "Nick";

        reasoner = new TrestleBuilder()
                .withName("hadoop_gaul_new2")
//                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
                .withDBConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
//                .withInputClasses(SimpleGAULObject.class, AfricaRoads.class)
//                .withSharedCache(cache.getCache())
                .build();

        receive(ReceiveBuilder
                .match(String.class, string -> sender().tell("I got your string: " + string, self()))
                .match(SayHello.class, hello -> sender().tell("Hello, " + hello.getHello(), self()))
                .match(HelloStatus.class, status -> {
                    logger.debug("Helloing: {}", this.name);
                    sender().tell("Helloing: " + this.name, self());
                })
                .match(SpatialIntersectObject.class, intersect -> {
                    final Optional<List<Object>> objects = reasoner.spatialIntersectObject(intersect.getObject(), intersect.getBuffer());
                    sender().tell(objects, self());
                })
//                Need to match from most specific to most generic
                .match(TemporalSpatialIntersect.class, tsIntersect -> {
                    final Optional<? extends List<?>> objects;
                    final Class<?> datasetClass;
                    if (tsIntersect.getClazz() == null) {
                        datasetClass = reasoner.getDatasetClass(tsIntersect.getClassString());
                    } else {
                        datasetClass = tsIntersect.getClazz();
                    }
                    objects = reasoner.spatialIntersect(datasetClass, tsIntersect.getWkt(), tsIntersect.getBuffer(), tsIntersect.getTemporal());
                    sender().tell(objects, self());
                })
                .match(SpatialIntersect.class, intersect -> {
                    final Optional<? extends List<?>> objects = reasoner.spatialIntersect(intersect.getClazz(), intersect.getWkt(), intersect.getBuffer());
                    sender().tell(objects, self());
                })
                .match(GetRelatedObjects.class, related -> {
                    final Optional relatedObjects = reasoner.getRelatedObjects(related.getClazz(), related.getId(), related.getCutoff());
                    sender().tell(relatedObjects, self());
                })
                .match(ExportData.class, export -> {
                    final File file = reasoner.exportDataSetObjects(export.getClazz(), export.getObjectIDs(), export.getDataType());
                    sender().tell(file, self());
                })
                .match(GetDatasets.class, ds -> {
                    final Set<String> availableDatasets = reasoner.getAvailableDatasets();
                    sender().tell(availableDatasets, self());
                })
                .match(ReadObject.class, readObject -> {
                    final Object object = reasoner.readTrestleObject(readObject.getClazz(), readObject.getObjectID());
                    if (object != null) {
                        sender().tell(object, self());
                    } else {
                        sender().tell(new Status.Failure(new Throwable("Cannot read object")), self());
                    }
                })
                .matchAny(this::unhandled)
                .build());
    }

    @Override
    public void postStop() {
        logger.info("Shutting down actor");
//        this.reasoner.shutdown(false);
    }
}
