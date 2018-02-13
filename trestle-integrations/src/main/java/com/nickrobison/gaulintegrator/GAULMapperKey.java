package com.nickrobison.gaulintegrator;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by nickrobison on 12/19/17.
 */
public class GAULMapperKey implements WritableComparable<GAULMapperKey> {

    private final LongWritable regionID;
    private final Text regionName;

    public GAULMapperKey() {
        this.regionID = new LongWritable();
        this.regionName = new Text();
    }

    public GAULMapperKey(LongWritable regionID, Text regionName) {
        this.regionID = regionID;
        this.regionName = regionName;
    }

    public LongWritable getRegionID() {
        return regionID;
    }

    public Text getRegionName() {
        return regionName;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        this.regionID.write(dataOutput);
        this.regionName.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.regionID.readFields(dataInput);
        this.regionName.readFields(dataInput);
    }

    @Override
    public int compareTo(@NotNull GAULMapperKey o) {

        int result = this.getRegionID().compareTo(o.getRegionID());
        if (result == 0) {
            result = this.getRegionName().compareTo(o.getRegionName());
        }
        return result;
    }
}
