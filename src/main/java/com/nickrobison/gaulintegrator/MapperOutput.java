package com.nickrobison.gaulintegrator;

import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.gaulintegrator.common.Utils;
import org.apache.hadoop.io.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Created by nrobison on 5/5/16.
 */
//FIXME(nrobison): Fix weird duplicate warning
@SuppressWarnings("Duplicates")
public class MapperOutput implements WritableComparable<MapperOutput> {

    private final LongWritable regionID;
    private final Text regionName;
    private final IntWritable datasetYear;
    private final PolygonFeatureWritable polygonData;
    //    StartDate - ExpirationDate
    private final byte[] dateField;

    public MapperOutput() {
        this.regionID = new LongWritable();
        this.regionName = new Text();
        this.datasetYear = new IntWritable();
        this.polygonData = new PolygonFeatureWritable();
        dateField = new byte[Utils.TIMEDATASIZE];
    }

    public MapperOutput(LongWritable regionID,
                        Text polygonName,
                        IntWritable polygonYear,
                        PolygonFeatureWritable polygonData,
                        LocalDate startDate,
                        LocalDate expirationDate) {
        this.regionID = regionID;
        this.regionName = polygonName;
        this.datasetYear = polygonYear;
        this.polygonData = polygonData;
        this.dateField = Utils.WriteDateField(startDate, expirationDate);

    }


    @Override
    public void write(DataOutput out) throws IOException {
        regionID.write(out);
        regionName.write(out);
        datasetYear.write(out);
        polygonData.write(out);
        out.write(dateField);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        regionID.readFields(in);
        regionName.readFields(in);
        datasetYear.readFields(in);
        polygonData.readFields(in);
        in.readFully(this.dateField);
    }


    @Override
    public int compareTo(MapperOutput o) {
        if (regionID.compareTo(o.regionID) == 0) {
//            return regionName.compareTo(o.regionName);
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
//            return regionID.equals(other.regionID) && regionName.equals(((MapperOutput) o).regionName);
        }
        return false;
    }

    @Override
    public String toString() {
        return regionID.toString() + " - " + regionName.toString() + ": " + datasetYear.toString();
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
}
