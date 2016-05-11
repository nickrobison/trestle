package com.nickrobison.HadoopTests;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.gaulintegrator.GAULObject;
import com.nickrobison.gaulintegrator.common.ObjectID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by nrobison on 5/9/16.
 */
public class GAULInsert {

    private Connection dbConnection;
    private GAULObject testObject;
    private ObjectID testID;

    @Before
    public void setup() throws SQLException {
        dbConnection = DriverManager.getConnection("jdbc:postgresql://localhost/gaul", "nrobison", "");

        Polygon testPolygon = new Polygon();
        Envelope env = new Envelope(1000, 2000, 1010, 2010);
        testPolygon.addEnvelope(env, false);
        testID = new ObjectID();
        testObject = new GAULObject(testID,
                4321,
                "Test Object",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2000, 1, 1),
                testPolygon);
    }

    @Test
    public void TestInsert() throws SQLException {

        final PreparedStatement preparedStatement = dbConnection.prepareStatement(testObject.generateSQLInsertStatement());

        preparedStatement.execute();
//        Try and get it back
        String retrieveResults = "SELECT ObjectID, GaulCode, objectname, startdate, enddate, st_astext(geom) as geom FROM Objects WHERE ObjectID=?";
        final PreparedStatement pSRetrieve = dbConnection.prepareStatement(retrieveResults);
        pSRetrieve.setObject(1, testID.getID());
        final ResultSet rs = pSRetrieve.executeQuery();
        rs.next();
        GAULObject retrievedObject = new GAULObject(
                new ObjectID((UUID) rs.getObject("ObjectID"), ObjectID.IDVersion.SIMPLE),
                rs.getLong("GaulCode"),
                rs.getString("ObjectName"),
                rs.getDate("StartDate").toLocalDate(),
                rs.getDate("EndDate").toLocalDate(),
                (Polygon) GeometryEngine.geometryFromWkt(rs.getString("Geom"), 0, Geometry.Type.Polygon)
                );

        assertEquals("Objects should be the same", testObject.toString(), retrievedObject.toString());

    }

    @After
    public void cleanup() throws SQLException {
        String deleteTestDataString = "DELETE FROM objects where ObjectID = ?";
        final PreparedStatement deleteStatement = dbConnection.prepareStatement(deleteTestDataString);
        deleteStatement.setObject(1, testID.getID());
        deleteStatement.execute();

        dbConnection.close();
    }
}
