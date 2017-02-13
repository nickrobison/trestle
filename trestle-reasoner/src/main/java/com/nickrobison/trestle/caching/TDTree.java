package com.nickrobison.trestle.caching;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.caching.TriangleHelpers.*;

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


    /**
     * Insert a key/value pair with an open interval
     *
     * @param objectID  - String object key
     * @param startTime - Long temporal of start temporal
     * @param value     - Value
     */
    public void insertValue(String objectID, long startTime, String value) {
        insertValue(objectID, startTime, maxValue, value);
    }

    public void insertValue(String objectID, long startTime, long endTime, String value) {
//        Find the leaf at maxDepth that would contain the objectID
        final int matchingLeaf = getMatchingLeaf(startTime, endTime);
//        Find the region in list with the most number of matching bits
//        Notice the l2/l1 reordering, otherwise it finds the leaf with the fewest number of matching bits, because why not?
        final Optional<LeafNode> first = leafs
                .stream()
//                .sorted(comparator)
                .sorted((l1, l2) -> Integer.compare(idSimilarity(l2.getID(), matchingLeaf), idSimilarity(l1.getID(), matchingLeaf)))
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
     * Moves left->right through a binary string to determine how many bits match
     * @param leafID - LeafID to match
     * @param matchID - matchID to match LeafID against
     * @return - Number of common bits left->right
     */
    private static int idSimilarity(int leafID, int matchID) {
        final int minIDLength = FastMath.min(getIDLength(leafID), getIDLength(matchID));
        String leafString = Integer.toBinaryString(leafID);
        String matchString = Integer.toBinaryString(matchID);
        int match = 0;
        for (int i = 0; i < minIDLength; i++) {
            if (leafString.charAt(i) == matchString.charAt(i)) {
                match++;
            }
        }
        return match;
    }

    @SuppressWarnings("Duplicates")
    public @Nullable String getValue(String objectID, long atTime) {
        List<LeafNode> fullyContained = new ArrayList<>();
        long[] rectApex = {atTime, atTime};
        int length = 1;
        int parentDirection = 7;
        TriangleHelpers.TriangleApex parentApex = new TriangleHelpers.TriangleApex(0, maxValue);
        final ArrayDeque<LeafNode> populatedLeafs = this.leafs.stream()
                .filter(leaf -> leaf.getRecordCount() > 0)
                .collect(Collectors.toCollection(ArrayDeque::new));
        while (!populatedLeafs.isEmpty()) {
            final LeafNode first = populatedLeafs.pop();
            final int firstID = first.getID();
            int overlappingPrefix = firstID >> (getIDLength(firstID) - length);
            final TriangleApex childApex;
            final ChildDirection childDirection;
            if (length == 1) {
                childApex = new TriangleApex(0, maxValue);
                childDirection = new ChildDirection(7, 7);
            } else {
                childApex = calculateChildApex(length, parentDirection, parentApex.start, parentApex.end);
                childDirection = calculateChildDirection(parentDirection);
            }
//                Are we the lower child?
            final int intersectionResult;
            if (overlappingPrefix < getMaximumValue(overlappingPrefix)) {
                intersectionResult = filterTriangleResults(populatedLeafs, fullyContained, overlappingPrefix, childApex, childDirection.lowerChild, length, rectApex);
                parentDirection = childDirection.lowerChild;
            } else {
                intersectionResult = filterTriangleResults(populatedLeafs, fullyContained, overlappingPrefix, childApex, childDirection.higherChild, length, rectApex);
                parentDirection = childDirection.higherChild;
            }

//            If not fully contained or disjoint, check to see if ancestor equals leaf
            if (!(intersectionResult == 1) & !(intersectionResult == -2)) {
                if (firstID == overlappingPrefix) {
                    fullyContained.add(first);
//                    This?
                    length = 1;
                } else { // Return the leaf to the queue, and step down another level
                    populatedLeafs.push(first);
                    parentApex = childApex;
                    length++;
                }
            } else { // If this leaf is either fully within the rectangle, or completely outside of it. Reset back to the initial state and keep going
                parentApex = new TriangleHelpers.TriangleApex(0, maxValue);
                parentDirection = 7;
                length = 1;
            }
        }
        for (LeafNode node : fullyContained) {
            @Nullable final String value = node.getValue(objectID, atTime);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private int filterTriangleResults(ArrayDeque<LeafNode> populatedLeafs, List<LeafNode> fullyContained, int overlappingPrefix, TriangleApex childApex, int childDirection, int length, long[] rectApex) {
        final int intersectionResult = checkRectangleIntersection(childApex, childDirection, length, rectApex, maxValue);
        if (intersectionResult == 1) {
            final int currentSize = populatedLeafs.size();
            for (int i = 0; i < currentSize; i++) {
                final LeafNode next = populatedLeafs.pop();
                if (idSimilarity(next.getID(), overlappingPrefix) == length) {
                    fullyContained.add(next);
                } else {
                    populatedLeafs.add(next);
                }
            }
        } else if (intersectionResult == -2) {
            final int currentSize = populatedLeafs.size();
            for (int i = 0; i < currentSize; i++) {
                final LeafNode next = populatedLeafs.pop();
                if (!(idSimilarity(next.getID(), overlappingPrefix) == length)) {
                    populatedLeafs.add(next);
                }
            }
        }

        return intersectionResult;
    }

    /**
     * Recursively parse a {@link LeafSplit} to add all the new leaves, with records to the directory
     *
     * @param split
     */
    private void parseSplit(LeafSplit split) {
        if (split.higherSplit == null) {
//        Increment the max depth, if we need to
            if (this.maxDepth < getIDLength(split.higherLeaf.getID())) {
                this.maxDepth ++;
            }
            if (!this.leafs.contains(split.higherLeaf)) {
                this.leafs.add(split.higherLeaf);
            }
        } else {
            parseSplit(split.higherSplit);
        }

        if (split.lowerSplit == null) {
//        Increment the max depth, if we need to
            if (this.maxDepth < getIDLength(split.lowerLeaf.getID())) {
                this.maxDepth ++;
            }
            if (!this.leafs.contains(split.lowerLeaf)) {
                this.leafs.add(split.lowerLeaf);
            }
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
        final int idLength = getIDLength(leafID);
        if (idLength > this.maxDepth) {
            return leafID;
        }
        final TriangleHelpers.ChildDirection childDirection = TriangleHelpers.calculateChildDirection(parentDirection);
        final TriangleHelpers.TriangleApex childApex = TriangleHelpers.calculateChildApex(idLength + 1, parentDirection, parentApex.start, parentApex.end);
//                Intersects with low child?
        if (TriangleHelpers.checkPointIntersection(childApex, childDirection.lowerChild, idLength + 1, startTime, endTime)) {
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
