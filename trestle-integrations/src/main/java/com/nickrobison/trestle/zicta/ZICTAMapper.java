package com.nickrobison.trestle.zicta;

import com.esri.io.PolygonFeatureWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Created by nrobison on 3/6/17.
 */
public class ZICTAMapper extends Mapper<LongWritable, PolygonFeatureWritable, Text, ZICTAMapOutput> {
    private static final Text ZCTA = new Text("ZCTA5CE10");
//    private static final Text AFFGEOID = new Text("AFFGEOID10");
//    private static final Text GEOID = new Text("GEOID10");
//    private static final Text ALAND = new Text("ALAND10");
//    private static final Text AWATER = new Text("AWATER10");

    @Override
    protected void map(LongWritable key, PolygonFeatureWritable value, Context context) throws IOException, InterruptedException {

        if (value != null) {
            final Text zcta = (Text) value.attributes.get(ZCTA);
//            final Text affgeoid = (Text) value.attributes.get(AFFGEOID);
//            final Text geoid = (Text) value.attributes.get(GEOID);
//            final LongWritable aland = (LongWritable) value.attributes.get(ALAND);
//            final LongWritable awter = (LongWritable) value.attributes.get(AWATER);
            if (value.polygon != null) {
                final ZICTAMapOutput output = new ZICTAMapOutput(zcta, value);
                context.write(zcta, output);
            } else {
                int i = 0;
            }
        }
    }
}
