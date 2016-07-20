package com.nickrobison.StormTests;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import com.nickrobison.differenceprocessor.storm.utils.KryoFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by nrobison on 5/5/16.
 */
public class TestKryoSerialization {

    private final Text CODE = new Text("ADM2_CODE");
    private final Text NAME = new Text("ADM2_NAME");
    private final IntWritable CODEVALUE = new IntWritable(4326);
    private final Text NAMEValue = new Text("Test Region");
    private PolygonFeatureWritable inputPolygon;
    private Kryo kryo;

    @Before
    public void createTestData() throws ClassNotFoundException {

//        this.kryo = new Kryo();
        KryoFactory kFactory = new KryoFactory();
        kryo = kFactory.CreateKryoSerializer();
        inputPolygon = new PolygonFeatureWritable();
        final Polygon polygon = inputPolygon.polygon;
        polygon.startPath(0, 0);
        polygon.lineTo(10, 0);
        polygon.lineTo(10, 10);
        polygon.lineTo(0, 0);
        polygon.closeAllPaths();
        inputPolygon.attributes.put(CODE, CODEVALUE);
        inputPolygon.attributes.put(NAME, NAMEValue);
    }

    @Test
    public void testPolygonWritable() {
//        Serialize
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output newOutput = new Output(outputStream);
        kryo.writeObject(newOutput, inputPolygon);
        newOutput.close();

//        Deserialize
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        final Input newInput = new Input(inputStream);
        final PolygonFeatureWritable deserializedPolygon = kryo.readObject(newInput, PolygonFeatureWritable.class);
//        Check that the attributes are fine
        Assert.assertEquals(CODEVALUE, deserializedPolygon.attributes.get(CODE));
        Assert.assertEquals(NAMEValue, deserializedPolygon.attributes.get(NAME));
        Assert.assertEquals(inputPolygon.polygon.calculateArea2D(), deserializedPolygon.polygon.calculateArea2D(), 0.0);

    }

    @Test public void testWithEnvelope() {

        PolygonFeatureWritable newWritableEnvelope = new PolygonFeatureWritable();
        Polygon poly = newWritableEnvelope.polygon;
        Envelope env = new Envelope(1000, 2000, 1010, 2010);
        poly.addEnvelope(env, false);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output newOutput = new Output(outputStream);
        kryo.writeObject(newOutput, newWritableEnvelope);
        newOutput.close();

//        Deserialize
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        final Input newInput = new Input(inputStream);
        final PolygonFeatureWritable deserializedPolygon = kryo.readObject(newInput, PolygonFeatureWritable.class);

        Assert.assertEquals(newWritableEnvelope.polygon.calculateArea2D(), deserializedPolygon.polygon.calculateArea2D(), 0.0);

    }
}
