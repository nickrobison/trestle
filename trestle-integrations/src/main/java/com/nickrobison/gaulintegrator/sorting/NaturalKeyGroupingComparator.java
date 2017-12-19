package com.nickrobison.gaulintegrator.sorting;

import com.nickrobison.gaulintegrator.GAULMapperKey;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

/**
 * Created by nickrobison on 12/19/17.
 */
public class NaturalKeyGroupingComparator extends WritableComparator {

    public NaturalKeyGroupingComparator() {
        super(GAULMapperKey.class, true);
    }

    @Override
    public int compare(WritableComparable a, WritableComparable b) {
        return GAULADM2Comparator.compare(a, b);
    }
}
