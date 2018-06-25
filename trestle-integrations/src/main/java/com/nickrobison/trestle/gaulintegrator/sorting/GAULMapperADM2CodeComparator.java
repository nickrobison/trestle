package com.nickrobison.trestle.gaulintegrator.sorting;

import com.nickrobison.trestle.gaulintegrator.GAULMapperKey;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

/**
 * Created by nickrobison on 12/19/17.
 */
public class GAULMapperADM2CodeComparator extends WritableComparator {

    public GAULMapperADM2CodeComparator() {
        super(GAULMapperKey.class, true);
    }

    @Override
    public int compare(WritableComparable a, WritableComparable b) {
        return GAULADM2Comparator.compare(a, b);
    }
}
