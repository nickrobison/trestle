package com.nickrobison.spatialdifference;

import com.esri.core.geometry.Envelope;
import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.spatialdifference.records.GAULMapperResult;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Created by nrobison on 4/18/16.
 */
public class SpatialMapper extends Mapper<LongWritable, PolygonFeatureWritable, Text, GAULMapperResult> {

//    DBF field names are fully upercased
    private static final Text NAME = new Text("ADM2_NAME");
    private static final Text TOPNAME = new Text("ADM0_NAME");
    private static final Text CODE = new Text("ADM2_CODE");
    private static final Text YEAR = new Text("TBL_YEAR");
    private final Envelope m_envelope = new Envelope();
    private final Text m_text = new Text();
    private final Text m_name = new Text();
    private final Text m_top_name = new Text();
    private final LongWritable m_code = new LongWritable(0);
    private final DoubleWritable m_area = new DoubleWritable(0);
    private final Text m_year = new Text();

    public void map(final LongWritable key,
                    final PolygonFeatureWritable inputRecord,
                    final Context context) throws IOException, InterruptedException {

        inputRecord.polygon.queryEnvelope(m_envelope);

        m_area.set(m_envelope.calculateArea2D());

        m_name.set(inputRecord.attributes.getText(NAME.toString()));

        m_code.set(inputRecord.attributes.getLong(CODE.toString()));

        m_top_name.set(inputRecord.attributes.getText(TOPNAME.toString()));

        m_year.set(inputRecord.attributes.getText(YEAR.toString()));

        GAULMapperResult outputRecord = new GAULMapperResult(m_code, m_name, m_top_name, inputRecord,
                new DoubleWritable(m_envelope.calculateArea2D()),
                new DoubleWritable(m_envelope.calculateLength2D()), m_year);

//        m_text.set(inputRecord.attributes.getText(NAME.toString())
//                + "-"
//                + inputRecord.attributes.getText(YEAR.toString())
//                + ": "
//                + area
//                + " area");

        context.write(m_name, outputRecord);
    }
}
