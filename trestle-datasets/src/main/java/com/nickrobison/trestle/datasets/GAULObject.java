package com.nickrobison.trestle.datasets;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.common.DateFieldUtils;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Created by nrobison on 5/6/16.
 */
@SuppressWarnings({"pmd:LawOfDemeter", "pmd:BooleanGetMethodName"})
@DatasetClass(name = "gaul-test")
public class GAULObject {

    private static final int ESRID = 4326;

//    private final ObjectID objectID;
    private final String objectID;
    private final long gaulCode;
    private final String objectName;
    private final byte[] validRange;
    private final Polygon shapePolygon;
    private final long adm1Code;
    private final String adm1Name;
    private final String status;
    private final boolean dispArea;
    private final long adm0Code;
    private final String adm0Name;

    /**
     * Creates fully initialized GAUL Object Record
     * @param id String - ID of GAUL Object
     * @param gaulCode long - GAUL code of underlying object
     * @param objectName String - Object Name
     * @param startDate LocalDate - Start of object valid interval
     * @param endDate LocalDate - End of object valid interval
     * @param polygon Polygon - Object boundary
     */
    public GAULObject(String id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, Polygon polygon, long adm1Code, String adm1Name, String status, boolean dispArea, long adm0Code, String adm0Name) {
        this.objectID = id;
        this.objectName = objectName;
        this.validRange = DateFieldUtils.writeDateField(startDate, endDate);
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
    public GAULObject(String id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, String wkt, long adm1Code, String adm1Name, String status, boolean dispArea, long adm0Code, String adm0Name) {
        this.objectID = id;
        this.gaulCode = gaulCode;
        this.objectName = objectName;
        this.validRange = DateFieldUtils.writeDateField(startDate, endDate);
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
    @Ignore
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
     * @return String - WKT formatted polygon
     */
    @Spatial(name = "wkt")
    public String getPolygonAsWKT() {
        return GeometryEngine.geometryToWkt(shapePolygon, 0);
    }

    @StartTemporal
    public LocalDate getStartDate() {
        return DateFieldUtils.readStartDate(this.validRange);
    }

    @EndTemporal
    public LocalDate getEndDate() {
        return DateFieldUtils.readExpirationDate(this.validRange);
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
    public boolean getDispArea() {
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
        return "GAULObject{" +
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
                ", getDispArea=" + dispArea +
                ", adm0Code=" + adm0Code +
                ", adm0Name='" + adm0Name + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GAULObject that = (GAULObject) o;

        if (getGaulCode() != that.getGaulCode()) return false;
        if (getAdm1Code() != that.getAdm1Code()) return false;
        if (getDispArea() != that.getDispArea()) return false;
        if (getAdm0Code() != that.getAdm0Code()) return false;
        if (!getObjectID().equals(that.getObjectID())) return false;
        if (!getObjectName().equals(that.getObjectName())) return false;
        if (!Arrays.equals(validRange, that.validRange)) return false;
        if (!getShapePolygon().equals(that.getShapePolygon())) return false;
        if (!getAdm1Name().equals(that.getAdm1Name())) return false;
        if (!getStatus().equals(that.getStatus())) return false;
        return getAdm0Name().equals(that.getAdm0Name());
    }

    @Override
    public int hashCode() {
        int result = getObjectID().hashCode();
        result = 31 * result + (int) (getGaulCode() ^ (getGaulCode() >>> 32));
        result = 31 * result + getObjectName().hashCode();
        result = 31 * result + Arrays.hashCode(validRange);
        result = 31 * result + getShapePolygon().hashCode();
        result = 31 * result + (int) (getAdm1Code() ^ (getAdm1Code() >>> 32));
        result = 31 * result + getAdm1Name().hashCode();
        result = 31 * result + getStatus().hashCode();
        result = 31 * result + (getDispArea() ? 1 : 0);
        result = 31 * result + (int) (getAdm0Code() ^ (getAdm0Code() >>> 32));
        result = 31 * result + getAdm0Name().hashCode();
        return result;
    }
}
