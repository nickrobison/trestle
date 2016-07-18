package com.nickrobison.differenceprocessor.records;

import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by nrobison on 4/27/16.
 */
public class GAULMapperResult implements Writable {

    private LongWritable code;
    private PolygonFeatureWritable polygon;
    private Text region_name;
    private Text top_region_name;
    private DoubleWritable polygon_area;
    private DoubleWritable polygon_length;
    private Text tbl_year;

    public GAULMapperResult() {
        this.code = new LongWritable();
        this.polygon = new PolygonFeatureWritable();
        this.region_name = new Text();
        this.top_region_name = new Text();
        this.polygon_area = new DoubleWritable();
        this.polygon_length = new DoubleWritable();
        this.tbl_year = new Text();
    }

    public GAULMapperResult(LongWritable code, Text region_name, Text top_region_name, PolygonFeatureWritable polygon, DoubleWritable polygonArea, DoubleWritable polygonLength, Text tblYear) {
        this.code = code;
        this.region_name = region_name;
        this.top_region_name = top_region_name;
        this.polygon = polygon;
        this.polygon_area = polygonArea;
        this.polygon_length = polygonLength;
        this.tbl_year = tblYear;
    }

    public void readFields(DataInput in) throws IOException {
        code.readFields(in);
        region_name.readFields(in);
        top_region_name.readFields(in);
        polygon.readFields(in);
        polygon_area.readFields(in);
        polygon_length.readFields(in);
        tbl_year.readFields(in);
    }

    public void write(DataOutput out) throws IOException {
        code.write(out);
        region_name.write(out);
        top_region_name.write(out);
        polygon.write(out);
        polygon_area.write(out);
        polygon_length.write(out);
        tbl_year.write(out);
    }

    public Polygon getPolygon() {
        return this.polygon.polygon;
    }

    public String getYear() { return this.tbl_year.toString();}

}
