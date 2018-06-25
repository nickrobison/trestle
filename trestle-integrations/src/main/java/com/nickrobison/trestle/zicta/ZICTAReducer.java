package com.nickrobison.trestle.zicta;

import com.esri.core.geometry.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayDeque;

/**
 * Created by nrobison on 3/6/17.
 */
public class ZICTAReducer extends Reducer<Text, ZICTAMapOutput, Text, DoubleWritable> {
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.Intersection);
    private static final int INPUTSRS = 32610;
    private static final SpatialReference inputSR = SpatialReference.create(INPUTSRS);

    @Override
    protected void reduce(Text key, Iterable<ZICTAMapOutput> values, Context context) throws IOException, InterruptedException {
        final Configuration configuration = context.getConfiguration();
        ArrayDeque<ZICTAMapOutput> inputRecords = new ArrayDeque<>();
        for (ZICTAMapOutput record : values) {
            inputRecords.add(WritableUtils.clone(record, configuration));
        }

        final ZICTAMapOutput first = inputRecords.pop();
        double difference = 0.0;
        int count = 1;
        while (!inputRecords.isEmpty()) {
            final Polygon firstPolygon = first.getPolygon();
            final Geometry execute = operatorIntersection.execute(inputRecords.pop().getPolygon(), firstPolygon, inputSR, null);
            final double v = (firstPolygon.calculateArea2D() - execute.calculateArea2D()) / firstPolygon.calculateArea2D();
            difference = (difference + v) / count;
            count++;
        }

        context.write(key, new DoubleWritable(difference));
    }
}
