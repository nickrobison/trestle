package com.nickrobison.gaulintegrator;

import com.esri.core.geometry.*;
import com.nickrobison.gaulintegrator.common.ObjectID;
import com.nickrobison.gaulintegrator.common.Utils;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Created by nrobison on 5/6/16.
 */
public class GAULObject {

    private static final int DATESIZE = 16;
    private static final int ESRID = 4326;

    private final ObjectID objectID;
    private final long gaulCode;
    private final String objectName;
    private final byte[] validRange;
    private final Polygon shapePolygon;


    private GAULObject() {
        this.objectID = new ObjectID();
        this.objectName = "";
        this.validRange = new byte[DATESIZE];
        this.shapePolygon = new Polygon();
        gaulCode = 0;
    }

    /**
     * Creates fully initialized GAUL Object Record
     * @param id ObjectID - ID of GAUL Object
     * @param gaulCode long - GAUL code of underlying object
     * @param objectName String - Object Name
     * @param startDate LocalDate - Start of object valid interval
     * @param endDate LocalDate - End of object valid interval
     * @param polygon Polygon - Object boundary
     */
    public GAULObject(ObjectID id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, Polygon polygon) {
        this.objectID = id;
        this.objectName = objectName;
        this.validRange = Utils.WriteDateField(startDate, endDate);
        this.shapePolygon = polygon;
        this.gaulCode = gaulCode;
    }

    public ObjectID getObjectID() {
        return objectID;
    }

    public String getObjectName() {
        return objectName;
    }

    public Polygon getShapePolygon() {
        return shapePolygon;
    }

    /**
     * Get the object polygon in WKT format
     * @return String - WKT formatted polygon
     */
    public String getPolygonAsWKT() {
        return GeometryEngine.geometryToWkt(shapePolygon, 0);
    }

    public LocalDate getStartDate() {
        return Utils.ReadStartDate(this.validRange);
    }

    public LocalDate getEndDate() {
        return Utils.ReadExpirationDate(this.validRange);
    }

    /**
     * Generate the required SQL Statement to insert the object into the datastore
     * @return String - SQL Insert statement
     */
    public String generateSQLInsertStatement() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("INSERT INTO Objects (ObjectID, GAULCode, ObjectName, StartDate, EndDate, Geom) ");
        stringBuilder.append("VALUES(uuid('");
        stringBuilder.append(getObjectID().getID());
        stringBuilder.append("'), '");
        stringBuilder.append(getGaulCode());
        stringBuilder.append("', '");
        stringBuilder.append(getObjectName());
        stringBuilder.append("', '");
        stringBuilder.append(Date.valueOf(getStartDate()));
        stringBuilder.append("', '");
        stringBuilder.append(Date.valueOf(getEndDate()));
        stringBuilder.append("', ");
        stringBuilder.append("st_geomfromtext('");
        stringBuilder.append(getPolygonAsWKT());
        stringBuilder.append("',");
        stringBuilder.append(ESRID);
        stringBuilder.append("))");
        return stringBuilder.toString();
    }

//    TODO(nrobison): Clean this up, I don't really like the output style.
    @Override
    public String toString() {
        return "GAULObject{" +
                "objectID=" + objectID +
                ", gaulCode=" + gaulCode +
                ", objectName='" + objectName +
                ", startDate=" + getStartDate() +
                ", endDate=" + getEndDate() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GAULObject that = (GAULObject) o;

        if (!getObjectID().equals(that.getObjectID())) return false;
        return getObjectName().equals(that.getObjectName());

    }

    @Override
    public int hashCode() {
        int result = getObjectID().hashCode();
        result = 31 * result + getObjectName().hashCode();
        return result;
    }

    public long getGaulCode() {
        return gaulCode;
    }
}
