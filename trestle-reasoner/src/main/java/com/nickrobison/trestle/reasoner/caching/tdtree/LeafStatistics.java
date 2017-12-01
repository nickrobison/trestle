package com.nickrobison.trestle.reasoner.caching.tdtree;

import java.io.Serializable;

public class LeafStatistics implements Serializable {
    public static final long serialVersionUID = 42L;

    private final int leafID;
    private final String binaryID;
    private final double[] coordinates;
    private final int records;

    public LeafStatistics(int leafID, String binaryID, double[] coordinates, int records) {
        this.leafID = leafID;
        this.binaryID = binaryID;
        this.coordinates = coordinates;
        this.records = records;
    }

    public int getLeafID() {
        return leafID;
    }

    public String getBinaryID() {
        return binaryID;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public int getRecords() {
        return records;
    }
}
