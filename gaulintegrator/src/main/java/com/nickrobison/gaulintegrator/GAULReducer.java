package com.nickrobison.gaulintegrator;

import com.esri.core.geometry.*;
import com.nickrobison.gaulintegrator.common.ObjectID;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Reducer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 5/5/16.
 */
public class GAULReducer extends Reducer<LongWritable, MapperOutput, LongWritable, Text> {

    private static final Logger logger = LoggerFactory.getLogger(GAULReducer.class);
    private static final String STARTDATE = "temporal.startdate";
    private static final String ENDDATE = "temporal.enddate";
    private static final String CONNECTION = "hadoop.database.uri";

//    Setup the spatial stuff
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.Intersection);
    private static final int INPUTSRS = 32610;
    private static final SpatialReference inputSR = SpatialReference.create(INPUTSRS);

    private Connection dbConnection;
    private LocalDate startDate;
    private LocalDate endDate;
    private Configuration conf;
    private TrestleReasoner reasoner;
    private static final String objectRelationInsertString = "INSERT INTO Object_Relations (From_Object, To_Object, Edge_Weight, Start_Time, End_time) VALUES(?,?,?,?,?)";

    @Override
    protected void setup(Context context) {
        //        Load the properties file
//        TODO(nrobison): Add exception handling
        conf = context.getConfiguration();
//        TODO(nrobison): Move these to byte[]
        startDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(STARTDATE)), 1);
        endDate = LocalDate.ofYearDay(Integer.parseInt(conf.get(ENDDATE)), 1).with(TemporalAdjusters.lastDayOfYear());
        try {
            if (logger.isDebugEnabled()) {
                dbConnection = DriverManager.getConnection("jdbc:postgresql://localhost/gaul", "nrobison", "");
            } else {
                dbConnection = DriverManager.getConnection(conf.get(CONNECTION));
            }
        } catch (SQLException e) {
            logger.error("Cannot connect to postgres database", e);
            throw new RuntimeException("Cannot connect to postgres database", e);
        }

//        Setup the Trestle Reasoner
        reasoner = new TrestleReasoner.TrestleBuilder()
//                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1")
                .withDBConnection(conf.get("reasoner.db.connection"),
                        conf.get("reasoner.db.username"),
                        conf.get("reasoner.db.password"))
                .withInputClasses(GAULObject.class)
                .withName(conf.get("reasoner.ontology.name"))
                .build();

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
//                    values.iterator().next().getPolygonData().polygon);

//            Store into database
//            Store the object
            try {
                reasoner.writeObjectAsConcept(newObject);
            } catch (TrestleClassException e) {
                logger.error("Cannot write object to trestle", e);
            }
            try {
                final PreparedStatement preparedStatement = dbConnection.prepareStatement(newObject.generateSQLInsertStatement());
                preparedStatement.execute();
            } catch (SQLException e) {
                logger.error("Cannot execute SQL insert statement", e);
                throw new RuntimeException("Cannot execute SQL insert statement", e);
            }
//            context.write(key, new Text(newObject.toString()));

//            Store the object relation
//            final String relationString = "INSERT INTO Object_Relations (From_Object, To_Object, Edge_Weight, Start_Time, End_time) VALUES(?,?,?,?,?)";
            try {
                PreparedStatement relationInsert = dbConnection.prepareStatement(objectRelationInsertString);
                relationInsert.setObject(1, objectID.getID());
                relationInsert.setObject(2, objectID.getID());
                relationInsert.setDouble(3, 1.0);
                relationInsert.setDate(4, Date.valueOf(startDate));
                relationInsert.setDate(5, Date.valueOf(endDate));
                relationInsert.execute();

            } catch (SQLException e) {
                logger.error("Cannot insert into Object_Relation table", e);
                throw new RuntimeException("Cannot insert into Object_Relation table", e);
            }

        } else {
//            If we have records that don't cover the entirety of the input space, then we need to integrate them
//            Try to see if any potentially matching records exist.

//            Generate a new UUID for this set of objects
            ObjectID objectID = new ObjectID();
            Set<Text> polygonNames = new HashSet<>();
            polygonNames = inputRecords
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

//            Check to see if anything in the database either has the same name, or intersects the original object, with an added buffer.
            String nameRetrieval = "SELECT objectid, gaulcode, objectname, startdate, enddate, st_astext(geom) AS geom FROM Objects AS o1 WHERE ObjectName = ? OR st_intersects(st_buffer(st_geomfromtext(?, 4326), 500), o1.geom)";
            ResultSet rsName;
            try {
                final PreparedStatement nameRetrievalStatement = dbConnection.prepareStatement(nameRetrieval);
                nameRetrievalStatement.setString(1, objectName);
                nameRetrievalStatement.setString(2, newGAULObject.getPolygonAsWKT());
                rsName = nameRetrievalStatement.executeQuery();

            } catch (SQLException e) {
                logger.error("Cannot retrieve records from database", e);
                throw new RuntimeException("Cannot retrieve records from database", e);
            }

            List<GAULObject> matchedObjects_postgres = new ArrayList<>();
            try {
                while (rsName.next()) {
//                    Generate new objects
                    matchedObjects_postgres.add(new GAULObject(
                            new ObjectID((UUID) rsName.getObject("objectid"), ObjectID.IDVersion.SIMPLE),
                            rsName.getLong("GaulCode"),
                            rsName.getString("objectname"),
                            rsName.getDate("startdate").toLocalDate(),
                            rsName.getDate("enddate").toLocalDate(),
                            (Polygon) GeometryEngine.geometryFromWkt(rsName.getString(6), 0, Geometry.Type.Polygon)
                    ));
                }
            } catch (SQLException e) {
                logger.error("Cannot iterate through Name resultSet", e);
                throw new RuntimeException("Cannot iterate through Name resultSet", e);
            }

//            Try from Trestle
            final Optional<List<@NonNull GAULObject>> gaulObjects = reasoner.spatialIntersectObject(newGAULObject, 500);

            List<GAULObject> matchedObjects = gaulObjects.orElse(new ArrayList<>());
            if (matchedObjects_postgres.size() != matchedObjects.size()) {
                logger.warn("Postgres found {} objects, but Trestle only got {}", matchedObjects_postgres.size(), matchedObjects.size());
            }


//            If there are no matching objects in the database, just insert the new record and move on.
            if (matchedObjects.size() > 0) {
//                Map of objects and their respective weights
                Map<GAULObject, Double> relatedObjects = new HashMap<>();

                for (GAULObject matchedObject : matchedObjects) {
                    double objectWeight = 0.;

                    if (newGAULObject.getObjectName().equals(matchedObject.getObjectName())) {
                        objectWeight = .8;
                    }

//                    Compute the total area intersection
                    Polygon intersectedPolygon = new Polygon();
                    final Geometry computedGeometry = operatorIntersection.execute(matchedObject.getShapePolygon(), newGAULObject.getShapePolygon(), inputSR, null);
                    if (computedGeometry.getType() == Geometry.Type.Polygon) {
                        intersectedPolygon = (Polygon) computedGeometry;
                    } else {
                        logger.error("Incorrectly computed geometry, assuming 0 intersection");
//                        throw new RuntimeException("Incorrectly computed geometry");
                    }

                    double intersectedArea = intersectedPolygon.calculateArea2D() / newGAULObject.getShapePolygon().calculateArea2D();
                    objectWeight += (1 - objectWeight) * intersectedArea;

                    relatedObjects.put(matchedObject, objectWeight);
                }

//                Now, insert the matchedObjects into the object relations table
//                This is a <-> relationship, so for each object-pair we insert 2 lookups.
                for (Map.Entry<GAULObject, Double> relatedEntrySet : relatedObjects.entrySet()) {
//                    Only store relations with a weight of .2 or higher
                    if (relatedEntrySet.getValue() >= .2) {
                        PreparedStatement objectRelationInsert;
//                    Forward
                        try {
                            objectRelationInsert = dbConnection.prepareStatement(objectRelationInsertString);
                            objectRelationInsert.setObject(1, newGAULObject.getObjectID().getID());
                            objectRelationInsert.setObject(2, relatedEntrySet.getKey().getObjectID().getID());
                            objectRelationInsert.setDouble(3, relatedEntrySet.getValue());
                            objectRelationInsert.setDate(4, Date.valueOf(startDate));
                            objectRelationInsert.setDate(5, Date.valueOf(endDate));

                            objectRelationInsert.execute();

                        } catch (SQLException e) {
                            logger.error("Cannot insert new object relation into table", e);
                            throw new RuntimeException("Cannot insert new object relation into table", e);
                        }

//                    Backwards
                        try {
                            objectRelationInsert = dbConnection.prepareStatement(objectRelationInsertString);
                            objectRelationInsert.setObject(1, relatedEntrySet.getKey().getObjectID().getID());
                            objectRelationInsert.setObject(2, newGAULObject.getObjectID().getID());
                            objectRelationInsert.setDouble(3, relatedEntrySet.getValue());
                            objectRelationInsert.setDate(4, Date.valueOf(startDate));
                            objectRelationInsert.setDate(5, Date.valueOf(endDate));

                            objectRelationInsert.execute();

                        } catch (SQLException e) {
                            logger.error("Cannot insert new object (inverse) relation into table", e);
                            throw new RuntimeException("Cannot insert new object (inverse) relation into table", e);
                        }

//                        Now, Trestle, but we only have to write in a single direction.
                        reasoner.writeFactWithRelation(newGAULObject, relatedEntrySet.getValue(), relatedEntrySet.getKey());

                    }
                }

            }

//            Now, we insert the new itself record into the database
            try {
                reasoner.writeObjectAsFact(newGAULObject);
            } catch (TrestleClassException e) {
                logger.error("Cannot write {}", newGAULObject.getObjectName(), e);
            }
            final PreparedStatement singleObjectInsert;
            try {
                singleObjectInsert = dbConnection.prepareStatement(newGAULObject.generateSQLInsertStatement());
                singleObjectInsert.execute();
            } catch (SQLException e) {
                logger.error("Cannot insert object into Object table", e);
                throw new RuntimeException("Cannot insert object into Object table", e);
            }

//                Now, add it to the Object_Relations Table
            final PreparedStatement objectRelationInsert;
            try {
                objectRelationInsert = dbConnection.prepareStatement(objectRelationInsertString);
                objectRelationInsert.setObject(1, objectID.getID());
                objectRelationInsert.setObject(2, objectID.getID());
                objectRelationInsert.setDouble(3, 1.0);
                objectRelationInsert.setDate(4, Date.valueOf(startDate));
                objectRelationInsert.setDate(5, Date.valueOf(endDate));

                objectRelationInsert.execute();
            } catch (SQLException e) {
                logger.error("Cannot insert new object's self record in the Object_Relations table", e);
                throw new RuntimeException("Cannot insert new object's self record in the Object_Relations table", e);
            }

        }

        context.write(key, new Text("Records: " + inputRecords.size()));

    }

    @Override
    public void cleanup(Context context) {
        reasoner.shutdown(false);
        try {
            dbConnection.close();
        } catch (SQLException e) {
            logger.error("Cannot close database connection", e);
            throw new RuntimeException("Cannot close database connection", e);
        }
    }
}
