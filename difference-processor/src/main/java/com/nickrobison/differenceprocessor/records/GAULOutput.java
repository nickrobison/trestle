package com.nickrobison.differenceprocessor.records;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by nrobison on 4/28/16.
 */
public class GAULOutput implements Writable, DBWritable {

    private static final int FIELDNUMBER = 4;
    private Text recordName;
    private DoubleWritable spatialVariance;
    private DoubleWritable centerVariance;
    private IntWritable recordCount;

    public static int getFieldNumber() {
        return FIELDNUMBER;
    }

    public GAULOutput() {
        this.recordName = new Text();
        this.spatialVariance = new DoubleWritable();
        this.centerVariance = new DoubleWritable();
        this.recordCount = new IntWritable();
    }

    public GAULOutput(String recordName, double spatialVariance, double centerVariance, int recordCount) {
        this.recordName = new Text(recordName);
        this.spatialVariance = new DoubleWritable(spatialVariance);
        this.centerVariance = new DoubleWritable(centerVariance);
        this.recordCount = new IntWritable(recordCount);
    }

    public GAULOutput(Text recordName, DoubleWritable spatialVariance, DoubleWritable centerVariance, IntWritable recordCount) {
        this.recordName = recordName;
        this.spatialVariance = spatialVariance;
        this.centerVariance = centerVariance;
        this.recordCount = recordCount;
    }

    public String getFormattedResult() {
        return spatialVariance.toString() + "\t"
                + centerVariance.toString() + "\t"
                + recordCount.toString();
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        recordName.readFields(in);
        spatialVariance.readFields(in);
        centerVariance.readFields(in);
        recordCount.readFields(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        recordName.write(out);
        spatialVariance.write(out);
        centerVariance.write(out);
        recordCount.write(out);
    }

    @Override
    public void write(PreparedStatement statement) throws SQLException {
        statement.setString(1, this.recordName.toString());
        statement.setDouble(2, this.spatialVariance.get());
        statement.setDouble(3, this.centerVariance.get());
        statement.setInt(4, this.recordCount.get());
    }

    @Override
    public void readFields(ResultSet resultSet) throws SQLException {
        this.recordName = new Text(resultSet.getString(1));
        this.spatialVariance = new DoubleWritable(resultSet.getDouble(2));
        this.centerVariance = new DoubleWritable(resultSet.getDouble(3));
        this.recordCount = new IntWritable(resultSet.getInt(4));
    }
}
