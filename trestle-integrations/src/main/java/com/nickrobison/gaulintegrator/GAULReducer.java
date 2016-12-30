package com.nickrobison.gaulintegrator;

import com.esri.core.geometry.*;
import com.nickrobison.gaulintegrator.common.ObjectID;
import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
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
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized"})
public class GAULReducer extends Reducer<LongWritable, MapperOutput, LongWritable, Text> {

    private static final Logger logger = LoggerFactory.getLogger(GAULReducer.class);
    private static final String STARTDATE = "temporal.startdate";
    private static final String ENDDATE = "temporal.enddate";

    //    Setup the spatial stuff
    private static final OperatorFactoryLocal instance = OperatorFactoryLocal.getInstance();
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) instance.getOperator(Operator.Type.Intersection);
    private static final OperatorWithin operatorWithin = (OperatorWithin) instance.getOperator(Operator.Type.Within);
    private static final OperatorTouches operatorTouches = (OperatorTouches) instance.getOperator(Operator.Type.Touches);
    private static final OperatorEquals operatorEquals = (OperatorEquals) instance.getOperator(Operator.Type.Equals);
    private static final OperatorExportToWkt operatorWKTExport = (OperatorExportToWkt) instance.getOperator(Operator.Type.ExportToWkt);
    private static final int INPUTSRS = 32610;
    private static final SpatialReference inputSR = SpatialReference.create(INPUTSRS);

    private LocalDate startDate;
    private LocalDate endDate;
    private Configuration conf;
    private TrestleReasoner reasoner;

    @Override
    protected void setup(Context context) throws IOException {
//        Load the properties file
        conf = context.getConfiguration();
        startDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(STARTDATE)), 1);
        endDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(ENDDATE)), 1).with(TemporalAdjusters.lastDayOfYear());

//        Get the cached files
        if (context.getCacheFiles() != null && context.getCacheFiles().length > 0) {
            File ontologyFile = new File("./ontology");

//        Setup the Trestle Reasoner
            reasoner = new TrestleBuilder()
                    .withDBConnection(conf.get("reasoner.db.connection"),
                            conf.get("reasoner.db.username"),
                            conf.get("reasoner.db.password"))
                    .withInputClasses(GAULObject.class)
                    .withOntology(IRI.create(context.getCacheFiles()[0]))
                    .withPrefix(conf.get("reasoner.ontology.prefix"))
                    .withName(conf.get("reasoner.ontology.name"))
                    .withoutCaching()
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
            logger.debug("Added record: {}", record);
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
        if (minDate <= startDate.getYear() && maxDate >= endDate.getYear()) {
            logger.info("Object ID: {} has all the records", key);
            ObjectID objectID = new ObjectID();

//            Are all the names the same?
//            TODO(nrobison): Work on this, right now, I don't think there are any records that change, but if so, we need to handle them.
            Set<Text> polygonNames = new HashSet<>();
            polygonNames = inputRecords
                    .stream()
                    .map(MapperOutput::getRegionName)
                    .collect(Collectors.toSet());

            logger.debug("There are: {} unique names for this object", polygonNames.size());

//            Create the new GAUL Object
            String objectName = polygonNames.iterator().next().toString();
            final LocalDate startDate = LocalDate.of(minDate, Month.JANUARY, 1).with(TemporalAdjusters.firstDayOfYear());
            final LocalDate endDate = LocalDate.of(maxDate, 1, 1).plusYears(1).with(TemporalAdjusters.firstDayOfYear());
            GAULObject newObject = new GAULObject(objectID,
                    key.get(),
                    objectName,
                    startDate,
                    endDate,
                    inputRecords.get(0).getPolygonData().polygon);

//            Store into database
//            Store the object
            try {
//                reasoner.writeObjectAsConcept(newObject);
                reasoner.WriteAsTrestleObject(newObject);
            } catch (TrestleClassException e) {
                logger.error("Cannot write object to trestle", e);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                logger.error("Missing individual {}", missingOntologyEntity.getIndividual(), missingOntologyEntity);
            }

        } else {
//            If we have records that don't cover the entirety of the input space, then we need to integrate them
//            Try to see if any potentially matching records exist.

//            Generate a new UUID for this set of objects
            ObjectID objectID = new ObjectID();
            Set<Text> polygonNames = inputRecords
                    .stream()
                    .map(MapperOutput::getRegionName)
                    .collect(Collectors.toSet());

//            Create a new GAUL Object
            String objectName = polygonNames.iterator().next().toString();
            final LocalDate startDate = LocalDate.of(minDate, Month.JANUARY, 1).with(TemporalAdjusters.firstDayOfYear());
            final LocalDate endDate = LocalDate.of(maxDate, 1, 1).plusYears(1).with(TemporalAdjusters.firstDayOfYear());

            GAULObject newGAULObject = new GAULObject(
                    objectID,
                    key.get(),
                    objectName,
                    startDate,
                    endDate,
                    inputRecords.get(0).getPolygonData().polygon
            );

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
            final Optional<Set<String>> conceptIRIs = reasoner.STIntersectConcept(newGAULObject.getPolygonAsWKT(), 0);


//            If true, get all the concept members
            if (conceptIRIs.orElse(new HashSet<>()).size() > 0) {
                hasConcept = true;
                conceptIRIs.get().forEach(concept -> {
                    final Optional<List<GAULObject>> conceptMembers = reasoner.getConceptMembers(GAULObject.class, concept, null, null);
                    conceptMembers.ifPresent(members -> members.forEach(matchedObjects::add));
//                Now add the concept relations
//                    TODO(nrobison): This feels bad.
                    reasoner.addObjectToConcept(concept, newGAULObject, ConceptRelationType.TEMPORAL, 1.0);
                });
            } else {
//            If no, find objects to intersect
                reasoner.spatialIntersectObject(newGAULObject, 0)
                        .ifPresent(objects -> objects.forEach(matchedObjects::add));

//                Go ahead the create the new concept
                reasoner.addObjectToConcept(String.format("%s:concept", newGAULObject.getObjectName()), newGAULObject, ConceptRelationType.TEMPORAL, 1.0);
            }

////            Check to see if anything in the database either has the same name, or intersects the original object, with an added buffer.
//            final Optional<List<@NonNull GAULObject>> gaulObjects = reasoner.spatialIntersectObject(newGAULObject, 500);
//
//            List<GAULObject> matchedObjects = gaulObjects.orElse(new ArrayList<>());

//            Now, do the normal spatial intersection with the new object and its matched objects


//            If there are no matching objects in the database, just insert the new record and move on.
            if (matchedObjects.size() > 0) {
//                Map of objects and their respective weights
                Map<GAULObject, Double> relatedObjects = new HashMap<>();

                for (GAULObject matchedObject : matchedObjects) {
                    double objectWeight = 0.;

                    if (newGAULObject.getObjectName().equals(matchedObject.getObjectName())) {
                        objectWeight = .8;
                    }

//                    Spatial intersections
//                    We'll go through them one by one and see what works
//                    Equal?
                    if (operatorEquals.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null)) {
//                        If equal, write the relationship and continue to the next shape
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.EQUALS);
                        continue;
                    }

//                    Covers?

//                    Contains? Both new object inside matched object, and matched object inside new object
                    if (operatorWithin.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null)) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.CONTAINS);
                        continue;
                    }

                    if (operatorWithin.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null)) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.INSIDE);
                        continue;
                    }

//                    Meets
                    if (operatorTouches.execute(newGAULObject.getShapePolygon(), matchedObject.getShapePolygon(), inputSR, null)) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.MEETS);
                        continue;
                    }

//                    Overlaps
//                    Compute the total area intersection
                    Polygon intersectedPolygon = new Polygon();
                    final Geometry computedGeometry = operatorIntersection.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null);
                    if (computedGeometry.getType() == Geometry.Type.Polygon) {
                        intersectedPolygon = (Polygon) computedGeometry;
                    } else {
                        logger.error("Incorrectly computed geometry, assuming 0 intersection");
//                        throw new RuntimeException("Incorrectly computed geometry");
                    }
                    if (computedGeometry.calculateArea2D() > 0.0) {
                        final String wktBoundary = operatorWKTExport.execute(0, intersectedPolygon, null);
                        reasoner.writeSpatialOverlap(newGAULObject, matchedObject, wktBoundary);
                    }

//                Temporals?
//                        For the begins, we add 1 day to the new object and see if they're equal
                    if (newGAULObject.getEndDate().plusDays(1).isEqual(matchedObject.getStartDate())) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.BEGINS);
//                    New object before matched object?
                    } else if (newGAULObject.getEndDate().isBefore(matchedObject.getStartDate())) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.BEFORE);
                    } else if (newGAULObject.getStartDate().minusDays(1).isEqual(matchedObject.getEndDate())) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.ENDS);
                    } else if (newGAULObject.getStartDate().isAfter(matchedObject.getEndDate())) {
                        reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.AFTER);
//                        During, or overlap from start dates?
                    } else if (newGAULObject.getStartDate().isEqual(matchedObject.getStartDate())) {
//                        objects have matching starts, do they have matching ends as well?
                        if (newGAULObject.getEndDate().isEqual(matchedObject.getEndDate())) {
                            reasoner.writeObjectRelationship(newGAULObject, matchedObject, ObjectRelation.DURING);
                        } else {
//                            No, but they do overlap
                            String temporalOverlap;
                            if (newGAULObject.getEndDate().isBefore(matchedObject.getEndDate())) {
                                temporalOverlap = String.format("%s:%s", newGAULObject.getStartDate().toString(), newGAULObject.getEndDate().toString());
                            } else {
                                temporalOverlap = String.format("%s:%s", matchedObject.getStartDate().toString(), matchedObject.getEndDate().toString());
                            }
                            reasoner.writeTemporalOverlap(newGAULObject, matchedObject, temporalOverlap);
                        }
                    } else if (newGAULObject.getEndDate().isEqual(matchedObject.getEndDate())) {
//                        Overlap from end dates?
                        String temporalOverlap;
                        if (newGAULObject.getStartDate().isBefore(matchedObject.getStartDate())) {
                            temporalOverlap = String.format("%s:%s", matchedObject.getStartDate().toString(), matchedObject.getEndDate().toString());
                        } else {
                            temporalOverlap = String.format("%s:%s", newGAULObject.getStartDate().toString(), newGAULObject.getEndDate().toString());
                        }
                        reasoner.writeTemporalOverlap(newGAULObject, matchedObject, temporalOverlap);
                    }

                    double intersectedArea = intersectedPolygon.calculateArea2D() / newGAULObject.getShapePolygon().calculateArea2D();
                    objectWeight += (1 - objectWeight) * intersectedArea;

                    relatedObjects.put(matchedObject, objectWeight);
                }


//                Now, insert the matchedObjects into the reasoner
//                Only store relations with a weight of .2 or higher
//                relatedObjects.entrySet()
//                        .stream()
//                        .filter(relatedEntrySet -> relatedEntrySet.getValue() >= .2)
//                        .forEach(relatedEntrySet -> reasoner.writeFactWithRelation(newGAULObject, relatedEntrySet.getValue(), relatedEntrySet.getKey()));

            }

//            Now, we insert the new itself record into the database
            try {
                reasoner.WriteAsTrestleObject(newGAULObject);
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
}
