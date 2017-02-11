package com.nickrobison.trestle.caching;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 2/9/17.
 */
public class TDTree {

    private static final Logger logger = LoggerFactory.getLogger(TDTree.class);
    static long maxValue = LocalDate.of(3000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    //    static long maxValue = LocalDateTime.MAX.toInstant(ZoneOffset.UTC).toEpochMilli();
    static final TupleSchema leafSchema = buildLeafSchema();
    public static final double ROOTTWO = FastMath.sqrt(2);
    private final int blockSize;
    private final List<LeafNode> leafs = new ArrayList<>();
    protected int maxDepth;


    public TDTree(int blockSize) throws Exception {
        this.blockSize = blockSize;
        this.maxDepth = 0;

//        Init the root node
        final FastTuple rootTuple = leafSchema.createTuple();
        rootTuple.setDouble(1, 0);
        rootTuple.setDouble(2, maxValue);
        rootTuple.setShort(3, (short) 7);
        leafs.add(new LeafNode(1, rootTuple, this.blockSize));
    }

    void setMaxDepth(int depth) {
        this.maxDepth = depth;
    }


    public void insertValue(String objectID, long startTime, long endTime, String value) {
//        Find the leaf at maxDepth that would contain the objectID
        final int matchingLeaf = getMatchingLeaf(startTime, endTime);
//        Find the region in D with the most number of matching bits
        final Optional<LeafNode> first = leafs
                .stream()
                .sorted(Comparator.comparingInt(l -> Integer.bitCount(l.getID() ^ matchingLeaf)))
                .findFirst();
//        We can do this because it will always match on, at least, the root node
        final LeafSplit split = first.get().insert(objectID, startTime, endTime, value);
//        If we split, we need to add the new leafs to the tree, and remove the old ones
        if (split != null) {
            leafs.remove(first.get());
            parseSplit(split);
        }
    }

    /**
     * Recursively parse a {@link LeafSplit} to add all the new leaves, with records to the directory
     *
     * @param split
     */
    private void parseSplit(LeafSplit split) {
        if (split.higherSplit == null) {
            this.leafs.add(split.higherLeaf);
        } else {
            parseSplit(split.higherSplit);
        }

        if (split.lowerSplit == null) {
            this.leafs.add(split.lowerLeaf);
        } else {
            parseSplit(split.lowerSplit);
        }
    }

    int getMatchingLeaf(long startTime, long endTime) {
        if (this.maxDepth == 0) {
            return 1;
        }
        return getMatchingLeaf(startTime, endTime, 1, 7, new TriangleHelpers.TriangleApex(0, maxValue));
    }

    private int getMatchingLeaf(long startTime, long endTime, int leafID, int parentDirection, TriangleHelpers.TriangleApex parentApex) {
        final int idLength = TriangleHelpers.getIDLength(leafID);
        if (idLength > this.maxDepth) {
            return leafID;
        }
        final TriangleHelpers.ChildDirection childDirection = TriangleHelpers.calculateChildDirection(parentDirection);
        final TriangleHelpers.TriangleApex childApex = TriangleHelpers.calculateChildApex(idLength + 1, parentDirection, parentApex.start, parentApex.end);
//                Intersects with low child?
        if (TriangleHelpers.checkIntersection(childApex, childDirection.lowerChild, idLength + 1, startTime, endTime)) {
            return getMatchingLeaf(startTime, endTime, leafID << 1, childDirection.lowerChild, childApex);
        }
        return getMatchingLeaf(startTime, endTime, (leafID << 1) | 1, childDirection.higherChild, parentApex);
    }


    private static TupleSchema buildLeafSchema() {
        try {
            return TupleSchema
                    .builder()
                    .addField("start", Double.TYPE)
                    .addField("end", Double.TYPE)
                    .addField("direction", Short.TYPE)
                    .heapMemory()
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build leaf schema", e);
        }
    }

}
