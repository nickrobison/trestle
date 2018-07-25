package com.nickrobison.trestle.gaulintegrator;

import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialComparisonReport;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.engines.temporal.TemporalComparisonReport;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Reducer;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized", "squid:S2068", "pmd:LawOfDemeter", "pmd:DataflowAnomalyAnalysis "})
public class GAULReducer extends Reducer<GAULMapperKey, MapperOutput, LongWritable, Text> {

    private static final Logger logger = LoggerFactory.getLogger(GAULReducer.class);
    private static final String STARTDATE = "temporal.startdate";
    private static final String ENDDATE = "temporal.enddate";

    //    Setup the spatial stuff
    private static final OperatorExportToWkb operatorWKBExport = OperatorExportToWkb.local();
    private static final int INPUTSRS = 4326;
    private static final SpatialReference inputSR = SpatialReference.create(INPUTSRS);

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR.getID());
    private static final WKBReader wkbReader = new WKBReader(geometryFactory);

    private LocalDate configStartDate;
    private LocalDate configEndDate;
    private TrestleReasoner reasoner;
    private File metricsFile;
//    Controls the granularity of the union matcher
    private double configEqualityCutoff;

    @Override
    protected void setup(Context context) throws IOException {
//        Load the properties file
        final Configuration conf = context.getConfiguration();
        configStartDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(STARTDATE)), 1);
        configEndDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(ENDDATE)), 1).with(TemporalAdjusters.lastDayOfYear());

//        Setup stuff for the shutdown hook
        //        Write out the metrics data
        final String location = conf.get("metrics.path");
        final String taskID = context.getTaskAttemptID().getTaskID().toString();
        metricsFile = new File(location + taskID + ".log");

        configEqualityCutoff = Double.parseDouble(conf.get("equality.cutoff"));


//        Setup the Trestle Reasoner
//            The conf file will strip out parameters with empty values, so we need to check for that and reset them.
        String username = conf.get("reasoner.db.username");
        if (username == null) {
            username = "";
        }

        String password = conf.get("reasoner.db.password");
        if (password == null) {
            password = "";
        }
        logger.info("Initializing reasoner");
        reasoner = new TrestleBuilder()
                .withDBConnection(conf.get("reasoner.db.connection"),
                        username,
                        password)
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(conf.get("reasoner.ontology.location")))
                .withPrefix(conf.get("reasoner.ontology.prefix"))
                .withName(conf.get("reasoner.ontology.name"))
                .withoutMetrics()
                .build();

//        Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownReducer));
    }

    @Override
    public void reduce(GAULMapperKey key, Iterable<MapperOutput> values, Context context) throws IOException, InterruptedException {

        final Optional<Queue<MapperOutput>> inputRecordsOptional = this.processInputSet(values, context);
//        If we have an object returned from the above function, we need to look for any other overlapping objects
        if (inputRecordsOptional.isPresent()) {
            final Queue<MapperOutput> inputRecords = inputRecordsOptional.get();
            final GAULObject newGAULObject = inputRecords.poll().toObject();
            logger.warn("Processing {}-{}-{} for union and intersections", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
//            Manually run the inferencer, for now
            final Instant infStart = Instant.now();
            reasoner.getUnderlyingOntology().runInference();
            final Instant infStop = Instant.now();
            logger.debug("Updating inference took {} ms", Duration.between(infStart, infStop).toMillis());

//            List of objects to compare to given object
            final List<GAULObject> matchedObjects = new ArrayList<>();

//            See if there's a collection that spatially intersects the object
            try {
                final Optional<Set<String>> collectionIRIs = reasoner.STIntersectCollection(newGAULObject.getPolygonAsWKT(), 0, 0.7, null, null);


//            If true, get all the collection members
                final String collectionIRI = String.format("%s:collection", newGAULObject.getObjectID());
                if (!collectionIRIs.orElse(new HashSet<>()).isEmpty()) {
                    logger.warn("{}-{}-{} has collection members", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
                    collectionIRIs.get().forEach(collection -> processCollectionMembers(newGAULObject, matchedObjects, collection));
                } else {
                    logger.warn("{}-{}-{} getting intersected objects", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
//            If no, find objects to intersect
                    reasoner.spatialIntersectObject(newGAULObject, 0)
                            .ifPresent(matchedObjects::addAll);

//                Go ahead the create the new collection
                    logger.info("{}-{}-{} creating new collection", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
                    reasoner.addObjectToCollection(collectionIRI, newGAULObject, CollectionRelationType.SPATIAL, 1.0);
                }

                // test of approx equal union
                if (matchedObjects.size() > 1) {
                    processEquality(newGAULObject, matchedObjects);
                }

                if (matchedObjects.isEmpty()) {
//                Go ahead the create the new collection
                    logger.info("{}-{}-{} creating new collection", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
                    reasoner.addObjectToCollection(collectionIRI, newGAULObject, CollectionRelationType.SPATIAL, 1.0);//                If we don't have any matches, create a new collection
                }


//            If there are no matching objects in the database, just insert the new record and move on.
                if (!matchedObjects.isEmpty()) {
//                Map of objects and their respective weights
                    Map<GAULObject, Double> relatedObjects = new HashMap<>();

                    for (GAULObject matchedObject : matchedObjects) {
                        double objectWeight = 0.;

                        if (newGAULObject.getObjectName().equals(matchedObject.getObjectName())) {
                            objectWeight = .8;
                        }

                        final double adjustedWeight = writeSTRelations(newGAULObject, matchedObject, objectWeight);

                        relatedObjects.put(matchedObject, adjustedWeight);
                    }
                }

//            Now, we insert the new itself record into the database
                try {
                    logger.warn("{}-{}-{} inserting into repository", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
                    reasoner.writeTrestleObject(newGAULObject);
//                    Write the remaining records
                    this.writeRecordSet(inputRecords);
                } catch (TrestleClassException e) {
                    logger.error("Cannot write {}", newGAULObject.getObjectName(), e);
                } catch (MissingOntologyEntity missingOntologyEntity) {
                    logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
                }
            } catch (RuntimeException e) {
                logger.error("Unable to process object {}-{}-{}", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate(), e);
            }
            logger.warn("{}-{}-{} finished", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
            context.write(key.getRegionID(), new Text(String.format("%s:%s:%s:%s:%s", newGAULObject.getAdm0Code(), newGAULObject.getAdm0Name(), newGAULObject.getObjectID(), newGAULObject.getStartDate(), newGAULObject.getEndDate())));
        }
    }

    private void processEquality(GAULObject newGAULObject, List<GAULObject> matchedObjects) {
        matchedObjects.add(newGAULObject);

        logger.warn("{}-{}-{} calculating equality", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate());
        final Instant start = Instant.now();
        final Optional<UnionEqualityResult<GAULObject>> matchOptional = this.reasoner.getEqualityEngine().calculateSpatialUnion(matchedObjects, INPUTSRS, configEqualityCutoff);
        logger.warn("{}-{}-{} calculating equality took {} ms", newGAULObject.getGaulCode(), newGAULObject.getObjectName(), newGAULObject.getStartDate(), Duration.between(start, Instant.now()).toMillis());
        if (matchOptional.isPresent()) {
            // do something here
            final UnionEqualityResult<GAULObject> match = matchOptional.get();
            logger.warn("found approximate equality between " + match.getUnionObject() + " and " + match.getUnionOf());
            this.reasoner.addTrestleObjectSplitMerge(match.getType(), match.getUnionObject(), new ArrayList<>(match.getUnionOf()), match.getStrength());
        }
    }

    /**
     * Retrieve all the collectionIRI members and determine if the input object should be a member of this collection
     * If so, add all the members to the matchCollection to look for a potential equality
     *
     * @param gaulObject      - input object to parse
     * @param matchCollection - Match collection to add collection members to, if we should
     * @param collectionIRI      - String IRI of collection to retrieve membership of
     */
    private void processCollectionMembers(GAULObject gaulObject, List<GAULObject> matchCollection, String collectionIRI) {
        //                        Here, we want to grab all the collectionIRI members
        final Optional<List<GAULObject>> collectionMembers = reasoner.getCollectionMembers(GAULObject.class, collectionIRI, 0.0, null, gaulObject.getStartDate());

//                        If we have collection members, process them to see if we need to check them for membership
        if (collectionMembers.isPresent()) {
//                      Now add the collection relations
//                      Union the existing members, and see if we have any overlap
//            We need to convert to JTS, in order to properly handle the Union.
//                Get the exterior rings, of the input objects, in order to handle any holes
            final List<com.vividsolutions.jts.geom.Polygon> exteriorRings = getExteriorRings(gaulObject);
            final com.vividsolutions.jts.geom.Geometry inputGeometry = new GeometryCollection(exteriorRings.toArray(new com.vividsolutions.jts.geom.Geometry[0]), geometryFactory).union();

//            Create a new Geometry Collection, and union it
            List<com.vividsolutions.jts.geom.Polygon> exteriorPolygonsToUnion = new ArrayList<>();
            for (GAULObject object : collectionMembers.get()) {
                getExteriorRings(exteriorPolygonsToUnion, object);
            }
            final com.vividsolutions.jts.geom.Geometry collectionUnionGeom = new GeometryCollection(exteriorPolygonsToUnion.toArray(new com.vividsolutions.jts.geom.Geometry[0]), geometryFactory)
                    .union();

            final double unionArea = collectionUnionGeom.getArea();
            final double inputArea = gaulObject.getShapePolygon().calculateArea2D();
            double greaterArea = inputArea >= unionArea ? inputArea : unionArea;

//            final double intersectionArea = operatorIntersection.execute(gaulObject.getShapePolygon(), exteriorGeom, inputSR, null).calculateArea2D() / greaterArea;
            final double intersectionArea = collectionUnionGeom.intersection(inputGeometry).getArea() / greaterArea;
            if (intersectionArea > 0.0) {
                reasoner.addObjectToCollection(collectionIRI, gaulObject, CollectionRelationType.SPATIAL, intersectionArea);
                matchCollection.addAll(collectionMembers.get());
            }
        }
    }

    private List<com.vividsolutions.jts.geom.Polygon> getExteriorRings(GAULObject object) {
        final List<com.vividsolutions.jts.geom.Polygon> polygons = new ArrayList<>();
        getExteriorRings(polygons, object);
        return polygons;
    }

    private void getExteriorRings(List<com.vividsolutions.jts.geom.Polygon> exteriorPolygonsToUnion, GAULObject object) {
        final ByteBuffer polygonBuffer = operatorWKBExport.execute(0, object.getShapePolygon(), null);
        try {
            final com.vividsolutions.jts.geom.Geometry geometry = wkbReader.read(polygonBuffer.array());
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                final com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geometry.getGeometryN(i);
                exteriorPolygonsToUnion.add(geometryFactory
                        .createPolygon(polygon
                                .getExteriorRing()
                                .getCoordinates()));
            }
        } catch (Exception e) {
            logger.error("Exception, just die", e);
            throw new RuntimeException(e);
        }
    }

    private void shutdownReducer() {
        logger.info("Shutting down, trying to export metrics data");
        reasoner.getMetricsEngine().exportData(metricsFile);
        reasoner.shutdown(false);

//        Remove the metrics database, hard coded for now
        final File mvFile = new File("./trestle-metrics.mv.db");
        final File traceFile = new File("./trestle-metrics.trace.db");
        try {
            Files.delete(mvFile.toPath());
            Files.delete(traceFile.toPath());
        } catch (IOException e) {
            logger.error("Cannot remove metrics files", e);
        }
    }

    private double writeSTRelations(GAULObject newGAULObject, GAULObject matchedObject, double objectWeight) {
        logger.warn("{}-{}-{} writing relation for {}-{}-{}",
                newGAULObject.getGaulCode(),
                newGAULObject.getObjectName(),
                newGAULObject.getStartDate(),
                matchedObject.getGaulCode(),
                matchedObject.getObjectName(),
                matchedObject.getStartDate());
        // test of approx equality
        if (this.reasoner.getEqualityEngine().isApproximatelyEqual(newGAULObject, matchedObject, configEqualityCutoff) && !newGAULObject.equals(matchedObject)) {
            // do something here
            logger.info("found approximate equality between GAULObjects {} and {}", newGAULObject.getID(), matchedObject.getID());
//            Write a spatial equals
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.SPATIAL_EQUALS);
        }

//         Spatial interaction
//        Spatial interactions are exhaustive

//                    newGAUL within matchedObject? Covers, or Contains? IF Covers, also contains
        final SpatialComparisonReport spatialComparisonReport = this.reasoner.compareTrestleObjects(newGAULObject, matchedObject, configEqualityCutoff);

//        Write all the relations from the spatial report
//        Overlaps?
        spatialComparisonReport.getSpatialOverlap().ifPresent(s -> {
            if (spatialComparisonReport.getSpatialOverlapPercentage().orElseThrow(() -> new IllegalStateException("Should not have overlaps with percentage")) > 0.001) {
                reasoner.writeSpatialOverlap(newGAULObject, matchedObject, s);
            }
        });

//        Others, if they're not overlaps
        spatialComparisonReport
                .getRelations()
                .stream()
                .filter(relation -> !relation.equals(ObjectRelation.SPATIAL_EQUALS))
                .forEach(relation -> reasoner.writeObjectRelationship(newGAULObject, matchedObject, relation));

//        Try it in the other direction
        final SpatialComparisonReport inverseSpatialReport = this.reasoner.compareTrestleObjects(matchedObject, newGAULObject, configEqualityCutoff);

//        Do all the non-overlaps relations
        inverseSpatialReport
                .getRelations()
                .stream()
                .filter(relation -> !relation.equals(ObjectRelation.SPATIAL_EQUALS))
                .forEach(relation -> reasoner.writeObjectRelationship(matchedObject, newGAULObject, relation));

//        Temporals?
        final TemporalComparisonReport temporalComparisonReport = this.reasoner.getTemporalEngine().compareObjects(newGAULObject, matchedObject);
        temporalComparisonReport
                .getRelations()
                .forEach(relation -> reasoner.writeObjectRelationship(newGAULObject, matchedObject, relation));

//        Try in the other direction

        final TemporalComparisonReport inverseTemporalRelations = this.reasoner.getTemporalEngine().compareObjects(matchedObject, newGAULObject);
        inverseTemporalRelations
                .getRelations()
                .forEach(relation -> reasoner.writeObjectRelationship(matchedObject, newGAULObject, relation));

////        Does one start the other?
//        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getStartDate()) == 0) {
//            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.STARTS);
//        }
//
//        if (TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getEndDate()) == 0) {
//            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.FINISHES);
//        }
//
////            Meets?
//        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getEndDate()) == 0 ||
//                TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getStartDate()) == 0) {
//            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.TEMPORAL_MEETS);
//        }
//
////        Before? (Including meets)
//        if (TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getStartDate()) != 1) {
//            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.BEFORE);
//        }
//
////        After? (Including meets)
//        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getEndDate()) != -1) {
//            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.AFTER);
//        }

//        double intersectedArea = intersectedPolygon.calculateArea2D() / newGAULObject.getShapePolygon().calculateArea2D();
//        objectWeight += (1 - objectWeight) * intersectedArea;
        return objectWeight;
    }

    /**
     * Process input set to determine if we actually have all the possible values for a given input set, if so write them and return a null.
     * If not, return the first copy of the object to process further
     *
     * @param values  - {@link Iterable} of {@link MapperOutput} representing all key values
     * @param context - {@link Context} Hadoop context
     * @return - {@link GAULObject}, null if all the input set is present.
     */
    @SuppressWarnings({"squid:S1172"}) // We can't break the method signature, so we need to suppress this warning
    private Optional<Queue<MapperOutput>> processInputSet(Iterable<MapperOutput> values, Context context) {
        final Configuration configuration = context.getConfiguration();
//        Copy from the iterator into a simple array list, we can't run through the iterator more than once
//        This is a bit of a disaster, but since the set is relatively bounded, it should be ok.
        Queue<MapperOutput> inputRecords = new PriorityQueue<>(Comparator.comparingInt(MapperOutput::getDatasetYear));
        for (MapperOutput record : values) {
            inputRecords.add(WritableUtils.clone(record, configuration));
        }

//        Do my records cover the entirety of the input space?
        final int maxDate = inputRecords
                .stream()
                .map(MapperOutput::getExpirationDate)
                .mapToInt(LocalDate::getYear)
                .max()
                .orElse(0);

        final int minDate = inputRecords
                .stream()
                .map(MapperOutput::getStartDate)
                .mapToInt(LocalDate::getYear)
                .min()
                .orElse(9999);

//        If we have all the available records, than we can assume that the record is contiguous and just smash it into the database
        if (minDate <= configStartDate.getYear() && maxDate >= configEndDate.getYear()) {
            this.writeRecordSet(inputRecords);
            return Optional.empty();
        }
        return Optional.of(inputRecords);
    }


    /**
     * If we have all the records (if an object spans the entire set space), write/merge them into Trestle
     *
     * @param records - {@link List} of {@link MapperOutput}
     */

    private void writeRecordSet(Collection<MapperOutput> records) {
        final ArrayDeque<MapperOutput> sortedRecords = records
                .stream()
                .sorted()
                .collect(Collectors.toCollection(ArrayDeque::new));

        if (!sortedRecords.isEmpty()) {

            MapperOutput latestRecord = sortedRecords.pop();
            try {
                reasoner.writeTrestleObject(latestRecord.toObject());
            } catch (MissingOntologyEntity | TrestleClassException e) {
                logger.error("Unable to write objects", e);
            }

            for (MapperOutput record : sortedRecords) {
                if (record.hashCode() != latestRecord.hashCode()) {
                    latestRecord = record;
                    try {
                        reasoner.writeTrestleObject(record.toObject());
                    } catch (MissingOntologyEntity | TrestleClassException | TrestleInvalidDataException e) {
                        logger.error("Unable to write object {}-{}-{}", record.getRegionID(), record.getRegionName(), record.getStartDate(), e);
                    }
                }
            }
        }
    }
}
