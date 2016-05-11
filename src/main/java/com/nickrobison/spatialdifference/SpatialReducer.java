package com.nickrobison.spatialdifference;

import com.esri.core.geometry.*;
import com.nickrobison.spatialdifference.records.GAULMapperResult;
import com.nickrobison.spatialdifference.records.GAULOutput;
import org.apache.commons.math3.util.FastMath;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nrobison on 4/20/16.
 */
public class SpatialReducer extends Reducer<Text, GAULMapperResult, Text, GAULOutput> {

    private static final Logger logger = Logger.getLogger(SpatialReducer.class);

    private Double intersection_area = 0.0;
    private Envelope envelope = new Envelope();
    private Double m_double = 0.0;
    private Double centroidVariance = 0.0;
    private Configuration conf;

    //    ESRI stuff
    private static final OperatorFactoryLocal projEnv = OperatorFactoryLocal.getInstance(); // = OperatorFactoryLocal.getInstance();
    private static int codeOut = 32610;// WGS_1984_UTM_Zone_10N; : GCS 4326
    private SpatialReference inputSR;
    private SpatialReference outputSR;
    //    TODO(nrobison): Static variables?
    private static final OperatorIntersection operatorIntersection = (OperatorIntersection) OperatorFactoryLocal.getInstance().getOperator(Operator.Type.Intersection);
    private static final OperatorBoundary operatorBoundary = (OperatorBoundary) projEnv.getOperator(Operator.Type.Boundary);
    private static final OperatorDistance operatorDistance = (OperatorDistance) projEnv.getOperator(Operator.Type.Distance);

    private static MultipleOutputs mos;

//    FIXME(nrobison): Make these checked assignments
    @SuppressWarnings("unchecked")
    protected void setup(Context context) {
//        Setup the projection and coordinate systems
//        projEnv = OperatorFactoryLocal.getInstance();
        inputSR = SpatialReference.create(codeOut);
        outputSR = SpatialReference.create(codeOut);
//        TODO(nrobison): Move these to be static, final variables? Do init in the class body?
//        operatorIntersection = (OperatorIntersection) projEnv.getOperator(Operator.Type.Intersection);
//        operatorBoundary =   (OperatorBoundary) projEnv.getOperator(Operator.Type.Boundary);
//        operatorDistance = (OperatorDistance) projEnv.getOperator(Operator.Type.Distance);

        mos = new MultipleOutputs(context);
        conf = context.getConfiguration();
    }

    public void reduce(Text key, Iterable<GAULMapperResult> values, Context context) throws IOException, InterruptedException {

//        We can only iterate through the list once, so we need to clone the record into a second array.
        List<Geometry> inputGeoms = new ArrayList<>();
        for (GAULMapperResult record : values) {
            final GAULMapperResult clonedRecord = WritableUtils.clone(record, conf);
            inputGeoms.add(clonedRecord.getPolygon());
        }

//        TODO(nrobison): This is really, really gross
        if (inputGeoms.size() == 0) {
            logger.debug("Record of length 0");
        } else if (inputGeoms.size() == 1) {
            logger.debug("Record of length 1");
//            intersection_area = inputGeoms.get(0).calculateArea2D();
            m_double = 0.;
        } else {
            logger.debug("Running full intersection and computation");
            intersection_area = intersectGeometries(inputGeoms);
            //        For each element in the area, compute its difference from the intersection area
            m_double = computeSpatialVariance(inputGeoms);

//        Now, let's get the centroids of the geom envelopes
            List<Geometry> geomCentroids = new ArrayList<>();
//        GeometryCursor geomCentroids = new SimpleGeometryCursor();
            for (Geometry geom : inputGeoms) {
                geom.queryEnvelope(envelope);
                geomCentroids.add(envelope.getCenter());
            }

//            FIXME(nrobison): Not sure this actually works. Need more varying data
//        From the centroids, build a new boundary
            Point centroidCentroid;
            Polygon boundaryPolygon = new Polygon();
            boundaryPolygon.startPath((Point) geomCentroids.get(0));
            for (Geometry point : geomCentroids.subList(1, geomCentroids.size())) {
                boundaryPolygon.lineTo((Point) point);
            }
            boundaryPolygon.closePathWithLine();
                Envelope boundaryEnvelope = new Envelope();
                boundaryPolygon.queryEnvelope(boundaryEnvelope);
                centroidCentroid = boundaryEnvelope.getCenter();

//        Now, compute the centroid variance
            Double centroidDifference = 0.;
            for (Geometry centroid : geomCentroids) {
                Point centroidPoint = (Point) centroid;
                Double distance = operatorDistance.execute(centroidCentroid, centroidPoint, null);
                centroidDifference += FastMath.pow(distance, 2.);
            }

            centroidVariance = centroidDifference / (double) geomCentroids.size();
        }

        GAULOutput outputRecord = new GAULOutput(key.toString(), m_double, centroidVariance, inputGeoms.size());

        mos.write("HDFS", key, outputRecord);
        mos.write("Postgres", outputRecord, NullWritable.get());
    }

    public void cleanup(Context context) throws IOException, InterruptedException {
        mos.close();
    }

    //    TODO(nrobison): Static?
    private double intersectGeometries(List<Geometry> inputGeoms) {
//        Extract the first element to use as the base intersection
        List<Geometry> firstRecord = new ArrayList<>();
        firstRecord.add(inputGeoms.get(0));

        GeometryCursor firstInput = new SimpleGeometryCursor(firstRecord);
        GeometryCursor restInput = new SimpleGeometryCursor(inputGeoms);

//        The first param is the set of inputs to intersect, the second input is the intersector
        GeometryCursor outputGeoms = operatorIntersection.execute(restInput, firstInput, inputSR, null);

        Geometry geomr = outputGeoms.next();
        Polygon geomPoly = (Polygon) geomr;
        return geomPoly.calculateArea2D();
    }

    //    TODO(nrobison): Static? Probably need to move all variables internal
    private Double computeSpatialVariance(List<Geometry> inputGeoms) {
        Double varianceDifference = 0.;
        for (Geometry geom : inputGeoms) {
            varianceDifference += FastMath.pow((geom.calculateArea2D() - intersection_area), 2.);
        }


        return varianceDifference / (double) inputGeoms.size();
    }
}
