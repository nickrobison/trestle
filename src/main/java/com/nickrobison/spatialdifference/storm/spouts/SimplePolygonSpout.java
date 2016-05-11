package com.nickrobison.spatialdifference.storm.spouts;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.util.Map;

/**
 * Created by nrobison on 5/4/16.
 */
public class SimplePolygonSpout extends BaseRichSpout {

    SpoutOutputCollector collector;
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void nextTuple() {
        final PolygonFeatureWritable newPolygonWritable1 = new PolygonFeatureWritable();
        final Polygon polygon1 = newPolygonWritable1.polygon;
        Envelope env = new Envelope(1000, 2000, 1010, 2010);
        polygon1.addEnvelope(env, false);
//            polygon1.startPath(0, 0);
//            polygon1.lineTo(10, 0);
//            polygon1.lineTo(10, 10);
//            polygon1.lineTo(0, 0);
//            polygon1.closeAllPaths();
        newPolygonWritable1.attributes.put(new Text("ADM2_CODE"), new IntWritable(4326));
        newPolygonWritable1.attributes.put(new Text("ADM2_NAME"), new Text("Test Region"));
        collector.emit(new Values(newPolygonWritable1));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("polygon"));
    }
}
