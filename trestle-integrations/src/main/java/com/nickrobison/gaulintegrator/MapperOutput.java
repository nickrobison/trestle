package com.nickrobison.gaulintegrator;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.common.ObjectID;
import com.nickrobison.gaulintegrator.common.Utils;
import org.apache.hadoop.io.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Created by nrobison on 5/5/16.
 */
public class MapperOutput implements WritableComparable<MapperOutput> {

    private final LongWritable regionID;
    private final Text regionName;
    private final IntWritable datasetYear;
    private final PolygonFeatureWritable polygonData;
    //    StartDate - ExpirationDate
    private final byte[] dateField;
    private final LongWritable adm0Code;
    private final Text adm0Name;
    private final LongWritable adm1Code;
    private final Text adm1Name;
    private final BooleanWritable dispArea;
    private final Text status;

    public MapperOutput() {
        this.regionID = new LongWritable();
        this.regionName = new Text();
        this.datasetYear = new IntWritable();
        this.polygonData = new PolygonFeatureWritable();
        dateField = new byte[Utils.TIMEDATASIZE];
        this.adm0Code = new LongWritable();
        this.adm0Name = new Text();
        this.adm1Code = new LongWritable();
        this.adm1Name = new Text();
        this.dispArea = new BooleanWritable();
        this.status = new Text();
    }

    public MapperOutput(LongWritable regionID,
                        Text polygonName,
                        IntWritable polygonYear,
                        PolygonFeatureWritable polygonData,
                        LocalDate startDate,
                        LocalDate expirationDate,
                        LongWritable adm0Code,
                        Text adm0Name,
                        LongWritable adm1Code,
                        Text adm1Name,
                        BooleanWritable dispArea,
                        Text status) {
        this.regionID = regionID;
        this.regionName = polygonName;
        this.datasetYear = polygonYear;
        this.polygonData = polygonData;
        this.dateField = Utils.WriteDateField(startDate, expirationDate);
        this.adm0Code = adm0Code;
        this.adm0Name = adm0Name;
        this.adm1Code = adm1Code;
        this.adm1Name = adm1Name;
        this.dispArea = dispArea;
        this.status = status;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        regionID.write(out);
        regionName.write(out);
        datasetYear.write(out);
        polygonData.write(out);
        out.write(dateField);
        adm1Code.write(out);
        adm1Name.write(out);
        adm0Code.write(out);
        adm0Name.write(out);
        dispArea.write(out);
        status.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        regionID.readFields(in);
        regionName.readFields(in);
        datasetYear.readFields(in);
        polygonData.readFields(in);
        in.readFully(this.dateField);
        adm0Code.readFields(in);
        adm0Name.readFields(in);
        adm1Code.readFields(in);
        adm1Name.readFields(in);
        dispArea.readFields(in);
        status.readFields(in);
    }


    @Override
    public int compareTo(MapperOutput o) {
        if (regionID.compareTo(o.regionID) == 0) {
            return datasetYear.compareTo(o.datasetYear);
        } else {
//            TODO(nrobison): There should be another step, to check validity dates and finally area.
            return regionID.compareTo(o.regionID);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MapperOutput) {
            MapperOutput other = (MapperOutput) o;
//            FIXME(nrobison): This isn't right, an object can have the same ID, and a different name.
            return regionID.equals(other.regionID) && datasetYear.equals(other.datasetYear);
        }
        return false;
    }

    @Override
    public String toString() {
        return "MapperOutput{" +
                "regionID=" + regionID +
                ", regionName=" + regionName +
                ", datasetYear=" + datasetYear +
                ", polygonData=" + polygonData +
                ", dateField=" + Arrays.toString(dateField) +
                ", adm0Code=" + adm0Code +
                ", adm0Name=" + adm0Name +
                ", adm1Code=" + adm1Code +
                ", adm1Name=" + adm1Name +
                ", dispArea=" + dispArea +
                ", status=" + status +
                '}';
    }

    public LongWritable getRegionID() {
        return regionID;
    }

    public Text getRegionName() {
        return regionName;
    }

    public int getDatasetYear() {
        return datasetYear.get();
    }

    public PolygonFeatureWritable getPolygonData() {
        return polygonData;
    }

    public LocalDate getStartDate() {
        return Utils.ReadStartDate(this.dateField);
    }

    public LocalDate getExpirationDate() {
        return Utils.ReadExpirationDate(this.dateField);
    }

    public byte[] getDateField() {
        return dateField;
    }

    /**
     * Checks whether the object is valid at a given time instant. Inclusive of start date and exclusive of expiration date
     *
     * @param validAtDate Time instant to check object validity.
     * @return Object is valid
     */
//    TODO(nrobison): Refactor into standalone helper class
    public boolean isValidNow(LocalDate validAtDate) {
        if (validAtDate.isEqual(getStartDate())) {
            return true;
        } else if (validAtDate.isEqual(getExpirationDate())) {
            return false;
        } else if (validAtDate.isAfter(getStartDate()) && validAtDate.isBefore(getExpirationDate())) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether a given time instant occurs before, during, or after the Object's valid range
     *
     * @param validAtDate Time instant to check relation to object validity
     * @return occurs before, during, or after object's valid time range.
     */
//    TODO(nrobison): Refactor into standalone helper class
    public int compareDate(LocalDate validAtDate) {

        final LocalDate startDate = getStartDate();
        final LocalDate expirationDate = getExpirationDate();
        if (validAtDate.isEqual(startDate)) {
            return 0;
        } else {
            if (validAtDate.isEqual(expirationDate)) {
                return 1;
            } else if (validAtDate.isAfter(startDate) && validAtDate.isBefore(expirationDate)) {
                return 0;
            } else if (validAtDate.isAfter(expirationDate)) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    @Override
    public int hashCode() {
        int result = regionID.hashCode();
        result = 31 * result + (regionName != null ? regionName.hashCode() : 0);
        result = 31 * result + (datasetYear != null ? datasetYear.hashCode() : 0);
        return result;
    }

    public GAULObject toObject(LocalDate minDate, LocalDate maxDate) {
        return new GAULObject(
                regionID.get() + "-" + regionName.toString(),
                regionID.get(),
                regionName.toString(),
                minDate,
                maxDate,
                polygonData.polygon,
                adm1Code.get(),
                adm1Name.toString(),
                status.toString(),
                dispArea.get(),
                adm0Code.get(),
                adm0Name.toString());
    }

    public GAULObject toObject() {
        return toObject(getStartDate(), getExpirationDate());
    }
}
