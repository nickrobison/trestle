package com.nickrobison.spatialdifference.storm.bolts;

import com.esri.io.PolygonFeatureWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by nrobison on 5/4/16.
 */
public class NewShapeBolt extends BaseRichBolt {

    private static final Logger logger = Logger.getLogger(NewShapeBolt.class);
    private static final Text GID = new Text("ADM2_CODE");

    private Connection postgresConnection;

    private OutputCollector collector;
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;

//        Setup Postgres connection
        try {
            postgresConnection = DriverManager.getConnection("jdbc:postgresql://localhost/gaul", "nrobison", "");
        } catch (SQLException e) {
            logger.error("Cannot connect to database", e);
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    @Override
    public void execute(Tuple input) {
        PolygonFeatureWritable inputPolygon = (PolygonFeatureWritable) input.getValue(0);
        logger.debug("Received tuple: {}" + inputPolygon);
//        PolygonFeatureWritable inputPolygon = (PolygonFeatureWritable) input.getValue(0);
        final IntWritable adm2_code = (IntWritable) inputPolygon.attributes.get(GID);





        logger.debug("Got Polygon: " + adm2_code.toString());
        collector.emit(new Values(inputPolygon.polygon.calculateArea2D()));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
