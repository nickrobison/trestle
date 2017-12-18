package com.nickrobison.gaulintegrator;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Partitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nickrobison on 12/18/17.
 */
public class GAULPartitioner extends Partitioner<LongWritable, MapperOutput> {
    private static final Logger logger = LoggerFactory.getLogger(GAULPartitioner.class);

    public GAULPartitioner() {
//        Not used
    }

    @Override
    public int getPartition(LongWritable longWritable, MapperOutput mapperOutput, int numPartitions) {
        try {
            return Math.toIntExact(mapperOutput.getAdm0Code().get()) % numPartitions;
//            If it overflows, just send it to the first reducer
        } catch (ArithmeticException e) {
            logger.error("ADM0 Code {} overflowed, sending to first reducer", mapperOutput.getAdm0Code().get(), e);
            return 0;
        }

    }
}
