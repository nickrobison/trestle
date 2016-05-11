package com.nickrobison.spatialdifference.storm.spouts;

import com.esri.core.geometry.Polygon;
import com.esri.io.PolygonFeatureWritable;
import com.esri.shp.ShpReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.log4j.Logger;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.MessageId;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.util.*;

/**
 * Created by nrobison on 5/2/16.
 */
public class PolygonSpout extends BaseRichSpout {

    private static final Logger logger = Logger.getLogger(PolygonSpout.class);

    //    Spout stuff
    private String spoutID;
    private SpoutOutputCollector collector;

    //    HDFS stuff
    private FileSystem fs;
    private LinkedList<LocatedFileStatus> filteredSourceFiles = null;
    private ShpReader currentReader;
    private FSDataInputStream fileStream;

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {

//      Setup the storm stuff
        spoutID = topologyContext.getThisComponentId();
        collector = spoutOutputCollector;

//        Setup and connect to filesystem
        Configuration hadoopConfig = new Configuration();
        try {
            fs = FileSystem.get(hadoopConfig);
        } catch (IOException e) {
            logger.error("Cannot connect to HDFS", e);
            throw new RuntimeException("Cannot connect to HDFS", e);
        }

//        TODO(nrobison): Make this a variable
        final Path srcPath = new Path("src/test/shapefiles");
        try {
//            TODO(nrobison): Make the recursive function a variable
            final RemoteIterator<LocatedFileStatus> unfilteredSourceFiles = fs.listFiles(srcPath, true);
            filteredSourceFiles = filterSourceFiles(unfilteredSourceFiles, ".shp");
        } catch (IOException e) {
            logger.error("Cannot list files at: " + srcPath.toString(), e);
            throw new RuntimeException("Cannot list files at: " + srcPath.toString(), e);
        }

    }

    @Override
    public void nextTuple() {

//        Is there a file to currently read?
        if (currentReader == null) {
//            Try to open a new file and start reading
            LocatedFileStatus newFile = filteredSourceFiles.removeFirst();
            try {
                currentReader = createShpReader(newFile);
            } catch (IOException e) {
                logger.error("Cannot create ShpReader from: " + newFile.getPath(), e);
                throw new RuntimeException("Cannot create ShpReader from: " + newFile.getPath(), e);
            }
        }
        Polygon nextPolygon;
        try {
            nextPolygon = readPolygon();
        } catch (IOException e) {
            logger.error("Cannot read next polygon", e);
            throw new RuntimeException("Cannot read next polygon", e);
        }
        if (nextPolygon == null) {
            logger.debug("No more polygons to read");
            try {
                fileStream.close();
            } catch (IOException e) {
                logger.error("Cannot close current filestream", e);
                throw new RuntimeException("Cannot close current filestream", e);
            }
            return;
        }
        collector.emit(new Values(nextPolygon));

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("Polygon"));
    }

    private static LinkedList<LocatedFileStatus> filterSourceFiles(RemoteIterator<LocatedFileStatus> sourceFileIterator, String ending) throws IOException {

//        List<LocatedFileStatus> filteredFiles = new ArrayList<>();
        LinkedList<LocatedFileStatus> filteredFiles = new LinkedList<>();

        while (sourceFileIterator.hasNext()) {
            LocatedFileStatus sourceFile = sourceFileIterator.next();
            if (sourceFile
                    .getPath()
                    .getName()
                    .toLowerCase()
                    .endsWith(ending)) {
                filteredFiles.add(sourceFile);
            }
        }

        return filteredFiles;
    }

    private ShpReader createShpReader(LocatedFileStatus filePath) throws IOException {

        logger.debug("Opening file: " + filePath.getPath());
        fileStream = fs.open(filePath.getPath());
        return new ShpReader(fileStream);

    }

    private Polygon readPolygon() throws IOException {
        if (currentReader.hasMore()) {
            return currentReader.readPolygon();
        }
        return null;
    }

//    protected void emitData(List<Object> tuple, MessageId messageId) {
////        logger.trace("Emitting - {}", messageId);
//        collector.emit(tuple, messageId);
//    }
}
