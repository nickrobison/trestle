package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import com.nickrobison.trestle.caching.ITrestleIndex;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.caching.tdtree.TriangleHelpers.*;

/**
 * Created by nrobison on 2/9/17.
 */
public class TDTree<Value> implements ITrestleIndex<Value> {

    private static final Logger logger = LoggerFactory.getLogger(TDTree.class);
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();
    static long maxValue = LocalDate.of(3000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    //    static long maxValue = LocalDateTime.MAX.toInstant(ZoneOffset.UTC).toEpochMilli();
    static final TupleSchema leafSchema = buildLeafSchema();
    static final double ROOTTWO = FastMath.sqrt(2);
    private final int blockSize;
    private final List<LeafNode<Value>> leafs = new ArrayList<>();
    private int maxDepth;


    public TDTree(int blockSize) throws Exception {
        this.blockSize = blockSize;
        this.maxDepth = 0;

//        Init the root node
        final FastTuple rootTuple = leafSchema.createTuple();
        rootTuple.setDouble(1, 0);
        rootTuple.setDouble(2, maxValue);
        rootTuple.setShort(3, (short) 7);
        leafs.add(new SplittableNode<>(1, rootTuple, this.blockSize));
    }

    @Override
    public void insertValue(String objectID, long startTime, Value value) {
        insertValue(objectID, startTime, maxValue, value);
    }

    @Override
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void insertValue(String objectID, long startTime, long endTime, Value value) {
//        Take the write lock
        rwlock.writeLock().lock();
        try {
//        Find the leaf at maxDepth that would contain the objectID
            final int matchingLeaf = getMatchingLeaf(startTime, endTime);
//        Find the region in list with the most number of matching bits
//        Notice the l2/l1 reordering, otherwise it finds the leaf with the fewest number of matching bits, because why not?
            final Optional<LeafNode<Value>> first = leafs
                    .stream()
//                .sorted(comparator)
                    .sorted((l1, l2) -> Integer.compare(matchLength(l2.getID(), matchingLeaf), matchLength(l1.getID(), matchingLeaf)))
                    .findFirst();

//        We can do this because it will always match on, at least, the root node
            //noinspection unchecked
            final LeafSplit split = first.get().insert(objectID, startTime, endTime, value);
//        If we split, we need to add the new leafs to the tree, and remove the old ones
            if (split != null) {
                leafs.remove(first.get());
                parseSplit(split);
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public @Nullable Value getValue(String objectID, long atTime) {

        rwlock.readLock().lock();
        try {
            final List<LeafNode<Value>> candidateLeafs = findCandidateLeafs(atTime);

            for (LeafNode node : candidateLeafs) {
                //noinspection unchecked
                @Nullable final Value value = (Value) node.getValue(objectID, atTime);
                if (value != null) {
                    return value;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void deleteValue(String objectID, long atTime) {
        rwlock.writeLock().lock();
        try {
            final List<LeafNode<Value>> candidateLeafs = findCandidateLeafs(atTime);
            for (LeafNode node : candidateLeafs) {
                if (node.delete(objectID, atTime)) {
                    return;
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void updateValue(String objectID, long atTime, Value value) {
        rwlock.writeLock().lock();
        try {
            final List<LeafNode<Value>> candidateLeafs = findCandidateLeafs(atTime);
            for (LeafNode<Value> node : candidateLeafs) {
                if (node.update(objectID, atTime, value)) {
                    return;
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void replaceKeyValue(String objectID, long atTime, long startTime, long endTime, Value value) {
        rwlock.writeLock().lock();
        try {
            deleteValue(objectID, atTime);
            insertValue(objectID, startTime, endTime, value);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void setKeyTemporals(String objectID, long atTime, long startTime) {
        setKeyTemporals(objectID, atTime, startTime, maxValue);
    }

    @Override
    public void setKeyTemporals(String objectID, long atTime, long startTime, long endTime) {
        rwlock.writeLock().lock();
        try {
            final @Nullable Value value = getValue(objectID, atTime);
            if (value != null) {
                deleteValue(objectID, atTime);
                insertValue(objectID, startTime, endTime, value);
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private List<LeafNode<Value>> findCandidateLeafs(long atTime) {
        List<LeafNode<Value>> candidateLeafs = new ArrayList<>();
        long[] rectApex = {atTime, atTime};
        int length = 1;
        int parentDirection = 7;
        TriangleHelpers.TriangleApex parentApex = new TriangleHelpers.TriangleApex(0, maxValue);
        final ArrayDeque<LeafNode<Value>> populatedLeafs = this.leafs.stream()
                .filter(leaf -> leaf.getRecordCount() > 0)
                .collect(Collectors.toCollection(ArrayDeque::new));
        while (!populatedLeafs.isEmpty()) {
            final LeafNode<Value> first = populatedLeafs.pop();
            final int firstID = first.getID();
            int overlappingPrefix = firstID >> (getIDLength(firstID) - length);
            final TriangleHelpers.TriangleApex childApex;
            final TriangleHelpers.ChildDirection childDirection;

            final TriangleHelpers.TriangleApex triangleApex = calculateTriangleApex(overlappingPrefix, 0, 7, 0., maxValue);
            final int triangleDirection = calculateTriangleDirection(overlappingPrefix, 0, 7);
//            Filter the triangle results
            final int intersection = checkRectangleIntersection(triangleApex, triangleDirection, length, rectApex, maxValue);
//                If the triangle is fully contained within the rectangle, add all the leafs with the same prefix
            if (intersection == 1) {
                final int currentSize = populatedLeafs.size();
                for (int i = 0; i < currentSize; i++) {
                    final LeafNode<Value> next = populatedLeafs.pop();
                    if (idSimilarity(next.getID(), overlappingPrefix) == length) {
                        candidateLeafs.add(next);
                    } else {
                        populatedLeafs.add(next);
                    }
                }
                candidateLeafs.add(first);
                length = 1;
//                If it's fully disjoint from the rectangle, remove all sub-leafs
            } else if (intersection == -2) {
                final int currentSize = populatedLeafs.size();
                for (int i = 0; i < currentSize; i++) {
                    final LeafNode<Value> next = populatedLeafs.pop();
                    if (!(idSimilarity(next.getID(), overlappingPrefix) == length)) {
                        populatedLeafs.add(next);
                    }
                }
                length = 1;
//                If R matches F, add it to the candidate list
            } else if (firstID == overlappingPrefix) {
                candidateLeafs.add(first);
                length = 1;
            } else {
                populatedLeafs.push(first);
                length++;
            }
        }

        return candidateLeafs;
    }

    /**
     * Recursively parse a {@link LeafSplit} to add all the new leaves, with records to the directory
     *
     * @param split {@link LeafSplit}
     */
    private void parseSplit(LeafSplit split) {
        if (split.higherSplit == null) {
//        Increment the max depth, if we need to
            if (this.maxDepth < getIDLength(split.higherLeaf.getID())) {
                this.maxDepth++;
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
                this.maxDepth++;
            }
            if (!this.leafs.contains(split.lowerLeaf)) {
                this.leafs.add(split.lowerLeaf);
            }
        } else {
            parseSplit(split.lowerSplit);
        }
    }

    private int getMatchingLeaf(long startTime, long endTime) {
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
        return getMatchingLeaf(startTime, endTime, (leafID << 1) | 1, childDirection.higherChild, childApex);
    }

    /**
     * Determines how many bits match between two numbers
     * Shifts the numbers so that they're the same length
     *
     * @param leafID  - LeafID to match
     * @param matchID - matchID to match LeafID against
     * @return - Number of common bits left->right
     */
    private static int idSimilarity(int leafID, int matchID) {
        if (leafID == 0 | matchID == 0) {
            return 0;
        }
        final int idLength = TriangleHelpers.getIDLength(leafID);
        final int matchLength = TriangleHelpers.getIDLength(matchID);
        if (matchLength > idLength) {
            return idLength - Integer.bitCount(leafID ^ (matchID >> (matchLength - idLength)));
        } else {
            return matchLength - Integer.bitCount(matchID ^ (leafID >> (idLength - matchLength)));
        }
    }

    /**
     * Moves left->right through a string representation of two binary numbers, and counts the number of bits in common, until they start to diverge
     *
     * @param leafID  - Leaf ID to match
     * @param matchID - Match ID to match leaf against
     * @return - number of bits in common, until they diverge
     */
    private static int matchLength(int leafID, int matchID) {
        final int minIDLength = FastMath.min(getIDLength(leafID), getIDLength(matchID));
        String leafString = Integer.toBinaryString(leafID);
        String matchString = Integer.toBinaryString(matchID);
        int match = 0;
        while ((match < minIDLength) && (leafString.charAt(match) == matchString.charAt(match))) {
            match++;
        }
        return match;
    }

    private static TupleSchema buildLeafSchema() {
        try {
            return TupleSchema
                    .builder()
                    .addField("start", Double.TYPE)
                    .addField("end", Double.TYPE)
                    .addField("direction", Short.TYPE)
                    .implementInterface(LeafSchema.class)
                    .heapMemory()
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build leaf schema", e);
        }
    }

}
