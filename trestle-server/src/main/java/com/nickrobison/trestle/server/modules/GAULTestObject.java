package com.nickrobison.trestle.server.modules;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;

import java.time.LocalDate;

/**
 * Created by nrobison on 6/30/17.
 */
@DatasetClass(name = "gaul-test")
public class GAULTestObject {


    private static final int ESRID = 4326;

    //    private final ObjectID objectID;
    private final String objectID;
    private final long gaulCode;
    private final String objectName;
//    private final byte[] validRange;
    private final Polygon shapePolygon;
    private final long adm1Code;
    private final String adm1Name;
    private final String status;
    private final boolean dispArea;
    private final long adm0Code;
    private final String adm0Name;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Creates fully initialized GAUL Object Record
     *
     * @param id         String - ID of GAUL Object
     * @param gaulCode   long - GAUL code of underlying object
     * @param objectName String - Object Name
     * @param startDate  LocalDate - Start of object valid interval
     * @param endDate    LocalDate - End of object valid interval
     * @param polygon    Polygon - Object boundary
     */
    public GAULTestObject(String id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, Polygon polygon, long adm1Code, String adm1Name, String status, boolean dispArea, long adm0Code, String adm0Name) {
        this.objectID = id;
        this.objectName = objectName;
//        this.validRange = Utils.WriteDateField(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.shapePolygon = polygon;
        this.gaulCode = gaulCode;
        this.adm1Code = adm1Code;
        this.adm1Name = adm1Name;
        this.adm0Code = adm0Code;
        this.adm0Name = adm0Name;
        this.status = status;
        this.dispArea = dispArea;
    }

    @TrestleCreator
    public GAULTestObject(String id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, String wkt, long adm1Code, String adm1Name, String status, boolean dispArea, long adm0Code, String adm0Name) {
        this.objectID = id;
        this.gaulCode = gaulCode;
        this.objectName = objectName;
//        this.validRange = Utils.WriteDateField(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.shapePolygon = (Polygon) GeometryEngine.geometryFromWkt(wkt, 0, Geometry.Type.Polygon);
        this.adm0Code = adm0Code;
        this.adm0Name = adm0Name;
        this.adm1Code = adm1Code;
        this.adm1Name = adm1Name;
        this.status = status;
        this.dispArea = dispArea;
    }

    @Ignore
    public String getObjectID() {
        return objectID;
    }

    @Fact(name = "id")
    public String getObjectIDAsString() {
        return this.objectID;
    }

    @IndividualIdentifier
    public String getID() {
        return String.format("%s:%s:%s:%s", this.gaulCode, this.objectName.replace(" ", "-"), this.getStartDate().getYear(), this.getEndDate().getYear());
    }

    public String getObjectName() {
        return objectName;
    }

    @Ignore
    public Polygon getShapePolygon() {
        return shapePolygon;
    }

    @Fact(name = "gaulCode")
    public long getGaulCode() {
        return gaulCode;
    }

    /**
     * Get the object polygon in WKT format
     *
     * @return String - WKT formatted polygon
     */
    @Spatial(name = "wkt")
    public String getPolygonAsWKT() {
        return GeometryEngine.geometryToWkt(shapePolygon, 0);
    }

    @StartTemporal
    public LocalDate getStartDate() {
        return this.startDate;
//        return Utils.ReadStartDate(this.validRange);
    }

    @EndTemporal
    public LocalDate getEndDate() {
        return this.endDate;
//        return Utils.ReadExpirationDate(this.validRange);
    }

    @Fact(name = "adm1_code")
    public long getAdm1Code() {
        return adm1Code;
    }

    @Fact(name = "adm1_name")
    public String getAdm1Name() {
        return adm1Name;
    }

    @Fact(name = "status")
    public String getStatus() {
        return status;
    }

    @Fact(name = "disp_area")
    public boolean isDispArea() {
        return dispArea;
    }

    @Fact(name = "adm0_code")
    public long getAdm0Code() {
        return adm0Code;
    }

    @Fact(name = "adm0_name")
    public String getAdm0Name() {
        return adm0Name;
    }

    @Override
    public String toString() {
        return "GAULTestObject{" +
                "objectID=" + objectID +
                ", gaulCode=" + gaulCode +
                ", objectName='" + objectName + '\'' +
//                ", validRange=" + Arrays.toString(validRange) +
                ", startDate=" + getStartDate() +
                ", endDate=" + getEndDate() +
                ", shapePolygon=" + shapePolygon +
                ", adm1Code=" + adm1Code +
                ", adm1Name='" + adm1Name + '\'' +
                ", status='" + status + '\'' +
                ", dispArea=" + dispArea +
                ", adm0Code=" + adm0Code +
                ", adm0Name='" + adm0Name + '\'' +
                '}';
    }
}
