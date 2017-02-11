package com.nickrobison.trestle.caching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 2/9/17.
 */
public class TDTreeTest {

    @Test
    public void testLeafFind() throws Exception {
        TDTree.maxValue = 10;
        final TDTree tdTree = new TDTree(2);
        int matchingLeaf = tdTree.getMatchingLeaf(8, 8);
        assertEquals(1, matchingLeaf, "Should match root leaf");
        tdTree.setMaxDepth(4);
        matchingLeaf = tdTree.getMatchingLeaf(8, 8);
        assertEquals(31, matchingLeaf, "Should match leaf 11101");
        tdTree.setMaxDepth(0);

        tdTree.insertValue("test-object", 8, 9, "test-object-string");
//        tdTree.insertValue("test-object2", 1, 2, "test-object-string2");
        tdTree.insertValue("test-object2", 6, 9, "test-object-string2");
        tdTree.insertValue("test-object3", 6, 9, "test-object-string3");
        tdTree.insertValue("test-object4", 1, 2, "test-object-string4");
    }


}
