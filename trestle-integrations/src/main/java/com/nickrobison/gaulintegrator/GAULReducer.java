package com.nickrobison.gaulintegrator;

import com.esri.core.geometry.*;
import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.types.relations.ConceptRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
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
public class GAULReducer extends Reducer<LongWritable, MapperOutput, LongWritable, Text> {

    private static final Logger logger = LoggerFactory.getLogger(GAULReducer.class);
    private static final String STARTDATE = "temporal.startdate";
    private static final String ENDDATE = "temporal.enddate";

    //    Setup the spatial stuff
    private static final OperatorFactoryLocal instance = OperatorFactoryLocal.getInstance();
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) instance.getOperator(Operator.Type.Intersection);
    private static final OperatorWithin operatorWithin = (OperatorWithin) instance.getOperator(Operator.Type.Within);
    private static final OperatorTouches operatorTouches = (OperatorTouches) instance.getOperator(Operator.Type.Touches);
    private static final OperatorExportToWkt operatorWKTExport = (OperatorExportToWkt) instance.getOperator(Operator.Type.ExportToWkt);
    private static final int INPUTSRS = 32610;
    private static final SpatialReference inputSR = SpatialReference.create(INPUTSRS);

    private LocalDate configStartDate;
    private LocalDate configEndDate;
    private Configuration conf;
    private TrestleReasoner reasoner;

    @Override
    protected void setup(Context context) throws IOException {
//        Load the properties file
        conf = context.getConfiguration();
        configStartDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(STARTDATE)), 1);
        configEndDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(ENDDATE)), 1).with(TemporalAdjusters.lastDayOfYear());

//        Get the cached files
        if (context.getCacheFiles() != null && context.getCacheFiles().length > 0) {
            File ontologyFile = new File("./ontology");


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

            reasoner = new TrestleBuilder()
                    .withDBConnection(conf.get("reasoner.db.connection"),
                            username,
                            password)
                    .withInputClasses(GAULObject.class)
                    .withOntology(IRI.create(context.getCacheFiles()[0]))
                    .withPrefix(conf.get("reasoner.ontology.prefix"))
                    .withName(conf.get("reasoner.ontology.name"))
                    .withoutCaching()
                    .withoutMetrics()
                    .build();
        }

    }

    @Override
    public void reduce(LongWritable key, Iterable<MapperOutput> values, Context context) throws IOException, InterruptedException {

        final Configuration configuration = context.getConfiguration();
//        Copy from the iterator into a simple array list, we can't run through the iterator more than once
//        This is a bit of a disaster, but since the set is relatively bounded, it should be ok.
        List<MapperOutput> inputRecords = new ArrayList<>();
        for (MapperOutput record : values) {
            inputRecords.add(WritableUtils.clone(record, configuration));
        }

//        Do my records cover the entirety of the input space?
        final int maxDate = inputRecords
                .stream()
                .mapToInt(MapperOutput::getDatasetYear)
                .max()
                .orElse(0);

        final int minDate = inputRecords
                .stream()
                .mapToInt(MapperOutput::getDatasetYear)
                .min()
                .orElse(9999);

//        If we have all the available records, than we can assume that the record is contiguous and just smash it into the database
        if (minDate <= configStartDate.getYear() && maxDate >= configEndDate.getYear()) {
            logger.info("Object ID: {} has all the records", key);
//            ObjectID objectID = new ObjectID();

//            final LocalDate configStartDate = LocalDate.of(minDate, Month.JANUARY, 1).with(TemporalAdjusters.firstDayOfYear());
//            final LocalDate configEndDate = LocalDate.of(maxDate, 1, 1).plusYears(1).with(TemporalAdjusters.firstDayOfYear());
            this.writeFullRecordSet(inputRecords);

        } else {
//            If we have records that don't cover the entirety of the input space, then we need to integrate them
//            Try to see if any potentially matching records exist.

//            Generate a new UUID for this set of objects
//            ObjectID objectID = new ObjectID();
//            Set<Text> polygonNames = inputRecords
//                    .stream()
//                    .map(MapperOutput::getRegionName)
//                    .collect(Collectors.toSet());

//            Create a new GAUL Object
//            String objectName = polygonNames.iterator().next().toString();
//            final LocalDate configStartDate = LocalDate.of(minDate, Month.JANUARY, 1).with(TemporalAdjusters.firstDayOfYear());
//            final LocalDate configEndDate = LocalDate.of(maxDate, 1, 1).plusYears(1).with(TemporalAdjusters.firstDayOfYear());

            GAULObject newGAULObject = inputRecords.get(0).toObject();

//            GAULObject newGAULObject = new GAULObject(
//                    objectID,
//                    key.get(),
//                    objectName,
//                    configStartDate,
//                    configEndDate,
//                    inputRecords.get(0).getPolygonData().polygon
//            );

//            Try from Trestle
//            Manually run the inferencer, for now
            final Instant infStart = Instant.now();
            reasoner.getUnderlyingOntology().runInference();
            final Instant infStop = Instant.now();
            logger.debug("Updating inference took {} ms", Duration.between(infStart, infStop).toMillis());

//            List of objects to compare to given object
            final List<GAULObject> matchedObjects = new ArrayList<>();
            boolean hasConcept = false;


//            See if there's a concept that spatially intersects the object
            final Optional<Set<String>> conceptIRIs = reasoner.STIntersectConcept(newGAULObject.getPolygonAsWKT(), 0, null, null);


//            If true, get all the concept members
            if (!conceptIRIs.orElse(new HashSet<>()).isEmpty()) {
                hasConcept = true;
                conceptIRIs.get().forEach(concept -> {
                    final Optional<List<GAULObject>> conceptMembers = reasoner.getConceptMembers(GAULObject.class, concept, null, newGAULObject.getStartDate());
                    conceptMembers.ifPresent(matchedObjects::addAll);
//                Now add the concept relations
//                    TODO(nrobison): This feels bad.
                    reasoner.addObjectToConcept(concept, newGAULObject, ConceptRelationType.TEMPORAL, 1.0);
                });
            } else {
//            If no, find objects to intersect
                reasoner.spatialIntersectObject(newGAULObject, 0)
                        .ifPresent(matchedObjects::addAll);

//                Go ahead the create the new concept
                reasoner.addObjectToConcept(String.format("%s:concept", newGAULObject.getObjectName()), newGAULObject, ConceptRelationType.TEMPORAL, 1.0);
            }

////            Check to see if anything in the database either has the same name, or intersects the original object, with an added buffer.
//            final Optional<List<@NonNull GAULObject>> gaulObjects = reasoner.spatialIntersectObject(newGAULObject, 500);
//
//            List<GAULObject> matchedObjects = gaulObjects.orElse(new ArrayList<>());

//            Now, do the normal spatial intersection with the new object and its matched objects

            // test of approx equal union
            if (matchedObjects.size() > 1) {
                List<GAULObject> allGAUL = new ArrayList<>(matchedObjects);
                allGAUL.add(newGAULObject);

                final Optional<UnionEqualityResult<GAULObject>> matchOptional = this.reasoner.getEqualityEngine().calculateSpatialUnion(allGAUL, inputSR, 0.9);
                if (matchOptional.isPresent()) {
                    // do something here
                    final UnionEqualityResult<GAULObject> match = matchOptional.get();
                    logger.debug("found approximate equality between " + match.getUnionObject() + " and " + match.getUnionOf());
                    this.reasoner.addTrestleObjectSplitMerge(match.getType(), match.getUnionObject(), new ArrayList<>(match.getUnionOf()), match.getStrength());
                }
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
                reasoner.writeTrestleObject(newGAULObject);
            } catch (TrestleClassException e) {
                logger.error("Cannot write {}", newGAULObject.getObjectName(), e);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            }
        }
        context.write(key, new Text("Records: " + inputRecords.size()));
    }

    @Override
    public void cleanup(Context context) {
        reasoner.shutdown(false);
    }


    private double writeSTRelations(GAULObject newGAULObject, GAULObject matchedObject, double objectWeight) {
        // test of approx equality
        if (this.reasoner.getEqualityEngine().isApproximatelyEqual(newGAULObject, matchedObject, inputSR, 0.9)) {
            // do something here
            logger.info("found approximate equality between GAULObjects {} and {}", newGAULObject.getID(), matchedObject.getID());
//            Write a spatial equals
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.SPATIAL_EQUALS);
        }

//         Spatial interaction
//        Spatial interactions are exhaustive

//                    newGAUL within matchedObject? Covers, or Contains? IF Covers, also contains
        if (operatorTouches.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null) && operatorWithin.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null)) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.COVERS);
        } else if (operatorWithin.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null)) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.CONTAINS);
        }

//        What about in the other direction?
        if (operatorTouches.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null) && operatorWithin.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null)) {
            reasoner.writeObjectRelationship(matchedObject, newGAULObject, ObjectRelation.COVERS);
        } else if (operatorWithin.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null)) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.INSIDE);
        }

//                    Meets
        if (operatorTouches.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null)) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.SPATIAL_MEETS);
        }

//                    Overlaps
//                    Compute the total area intersection
        Polygon intersectedPolygon = new Polygon();
        final Geometry computedGeometry = operatorIntersection.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null);
        if (computedGeometry.getType() == Geometry.Type.Polygon) {
            intersectedPolygon = (Polygon) computedGeometry;
        } else {
            logger.error("Incorrectly computed geometry, assuming 0 intersection");
        }
        if (computedGeometry.calculateArea2D() > 0.0) {
            final String wktBoundary = operatorWKTExport.execute(0, intersectedPolygon, null);
            reasoner.writeSpatialOverlap(newGAULObject, matchedObject, wktBoundary);
        }

//        Temporals?

//        Does one start the other?
        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getStartDate()) == 0) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.STARTS);
        }

        if (TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getEndDate()) == 0) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.FINISHES);
        }

//            Meets?
        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getEndDate()) == 0 ||
                TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getStartDate()) == 0) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.TEMPORAL_MEETS);
        }

//        Before? (Including meets)
        if (TemporalUtils.compareTemporals(newGAULObject.getEndDate(), matchedObject.getStartDate()) != 1) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.BEFORE);
        }

//        After? (Including meets)
        if (TemporalUtils.compareTemporals(newGAULObject.getStartDate(), matchedObject.getEndDate()) != -1) {
            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.AFTER);
        }

    double intersectedArea = intersectedPolygon.calculateArea2D() / newGAULObject.getShapePolygon().calculateArea2D();
    objectWeight +=(1-objectWeight)*intersectedArea;
        return objectWeight;
}



    /**
     * If we have all the records (if an object spans the entire set space), write/merge them into Trestle
     *
     * @param records - {@link List} of {@link MapperOutput}
     */

    private void writeFullRecordSet(List<MapperOutput> records) {
        final ArrayDeque<MapperOutput> sortedRecords = records
                .stream()
                .sorted()
                .collect(Collectors.toCollection(ArrayDeque::new));

        MapperOutput latestRecord = sortedRecords.pop();
        try {
            final LocalDate recordStart = LocalDate.of(latestRecord.getDatasetYear(), 1, 1);
            reasoner.writeTrestleObject(latestRecord.toObject(), recordStart, latestRecord.getExpirationDate());
        } catch (MissingOntologyEntity | UnregisteredClassException e) {
            logger.error("Unable to write objects", e);
        }

        for (MapperOutput record : sortedRecords) {
            if (record.hashCode() != latestRecord.hashCode()) {
                latestRecord = record;
                final LocalDate recordStart = LocalDate.of(record.getDatasetYear(), 1, 1);
                try {
                    reasoner.writeTrestleObject(record.toObject(), recordStart, record.getExpirationDate());
                } catch (MissingOntologyEntity | UnregisteredClassException e) {
                    logger.error("Unable to write objects", e);
                }
            }
        }
    }
}
