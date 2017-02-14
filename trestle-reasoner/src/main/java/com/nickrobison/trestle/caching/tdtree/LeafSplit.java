package com.nickrobison.trestle.caching.tdtree;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 2/10/17.
 */
public class LeafSplit {

    final int leafID;
    final LeafNode lowerLeaf;
    final LeafNode higherLeaf;
    @Nullable LeafSplit lowerSplit;
    @Nullable LeafSplit higherSplit;

    LeafSplit(int leafID, LeafNode lowerLeaf, LeafNode higherLeaf) {
        this.leafID = leafID;
        this.lowerLeaf = lowerLeaf;
        this.higherLeaf = higherLeaf;
        this.lowerSplit = null;
        this.higherSplit = null;
    }
}
