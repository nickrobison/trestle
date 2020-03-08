package com.nickrobison.trestle.gaulintegrator.sorting;

import com.nickrobison.trestle.gaulintegrator.GAULMapperKey;
import org.apache.hadoop.io.WritableComparable;

/**
 * Created by nickrobison on 12/19/17.
 */
public class GAULADM2Comparator {

    private GAULADM2Comparator() {
//        Not used
    }

    public static int compare(WritableComparable o1, WritableComparable o2) {
        GAULMapperKey key1 = GAULMapperKey.class.cast(o1);
        GAULMapperKey key2 = GAULMapperKey.class.cast(o2);

        int result = key1.getRegionID().compareTo(key2.getRegionID());
        if (result == 0) {
            result = key1.getRegionName().compareTo(key2.getRegionName());
        }
        return result;
    }
}
