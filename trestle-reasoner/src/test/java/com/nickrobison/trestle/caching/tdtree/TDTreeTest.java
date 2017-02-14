package com.nickrobison.trestle.caching.tdtree;

import com.nickrobison.trestle.caching.tdtree.TDTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 2/9/17.
 */
public class TDTreeTest {

    @Test
    public void testLeafFind() throws Exception {
        TDTree.maxValue = 10;
        final TDTree<String> tdTree = new TDTree<>(3);
        int matchingLeaf = tdTree.getMatchingLeaf(8, 8);
        assertEquals(1, matchingLeaf, "Should match root leaf");
        tdTree.setMaxDepth(4);
        matchingLeaf = tdTree.getMatchingLeaf(8, 8);
        assertEquals(31, matchingLeaf, "Should match leaf 11101");
        tdTree.setMaxDepth(0);

        tdTree.insertValue("test-object", 8, 9, "test-object-string");
        tdTree.insertValue("test-object2", 6, 9, "test-object-string2");
        tdTree.insertValue("test-object3", 6, 9, "test-object-string3");
        tdTree.insertValue("test-object4", 1, 2, "test-object-string4");
        tdTree.insertValue("test-object", 1, 3, "test-object-string-early");
        @Nullable final String value = tdTree.getValue("test-object", 2);
        assertEquals("test-object-string-early", value, "Should have early value");

//        Test correct temporal provisioning
        final String temporalTestID = "temporal-test";
        tdTree.insertValue(temporalTestID, 1, 5, "first-value");
        tdTree.insertValue(temporalTestID, 5, 5, "second-value");
        tdTree.insertValue(temporalTestID, 6, "third-value");
        assertAll(() -> assertEquals("first-value", tdTree.getValue(temporalTestID, 4)),
                () -> assertEquals("second-value", tdTree.getValue(temporalTestID, 5)),
                () -> assertEquals("third-value", tdTree.getValue(temporalTestID, 9)));
    }


}
