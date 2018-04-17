package com.nickrobison.trestle.zicta;

import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by nrobison on 3/6/17.
 */
public class ZICTAMapOutput implements WritableComparable<ZICTAMapOutput> {
    private final Text zcta;
//    private final Text affgeoid;
//    private final Text geoid;
//    private final LongWritable aland;
//    private final LongWritable awater;
    private final PolygonFeatureWritable polygon;

    public ZICTAMapOutput(Text zcta, PolygonFeatureWritable polygon) {
        this.zcta = zcta;
//        this.affgeoid = affgeoid;
//        this.geoid = geoid;
//        this.aland = aland;
//        this.awater = awater;
        this.polygon = polygon;
    }

    public Polygon getPolygon() {
        return this.polygon.polygon;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        this.zcta.write(dataOutput);
//        this.affgeoid.write(dataOutput);
//        this.geoid.write(dataOutput);
//        this.aland.write(dataOutput);
//        this.awater.write(dataOutput);
        this.polygon.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.zcta.readFields(dataInput);
//        this.affgeoid.readFields(dataInput);
//        this.geoid.readFields(dataInput);
//        this.aland.readFields(dataInput);
//        this.awater.readFields(dataInput);
        this.polygon.readFields(dataInput);
    }

    @Override
    public int compareTo(@NotNull ZICTAMapOutput o) {
        return this.zcta.compareTo(o.zcta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZICTAMapOutput that = (ZICTAMapOutput) o;

        if (!zcta.equals(that.zcta)) return false;
        return getPolygon().equals(that.getPolygon());
    }

    @Override
    public int hashCode() {
        int result = zcta.hashCode();
        result = 31 * result + getPolygon().hashCode();
        return result;
    }
}
