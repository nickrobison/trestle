package com.nickrobison.trestle.caching.tdtree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by nrobison on 2/9/17.
 */
public class TDTreeTest {

    @Test
    public void testLeafFind() throws Exception {
        TDTree.maxValue = 10;
        final TDTree<String> tdTree = new TDTree<>(2);

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

//        Try for some deletions
        tdTree.deleteValue("test-object", 2);
        assertNull(tdTree.getValue("test-object", 2), "Should have null value");
        assertEquals("test-object-string4", tdTree.getValue("test-object4", 1), "Shouldn't throw an error after deleting a key/value pair");

//        Try to update values and temporals
        tdTree.updateValue(temporalTestID, 5, "new-value");
        assertEquals("new-value", tdTree.getValue(temporalTestID, 5));
        tdTree.setKeyTemporals(temporalTestID, 6, 6, 8);
        assertAll(() -> assertNull(tdTree.getValue(temporalTestID, 10), "Should not have any value valid at time 10"),
                () -> assertEquals("third-value", tdTree.getValue(temporalTestID, 7)));
        tdTree.replaceKeyValue(temporalTestID, 3, 3, 4, "updated-temporal-value");
        assertAll(() -> assertEquals("updated-temporal-value", tdTree.getValue(temporalTestID, 3)),
                () -> assertNull(tdTree.getValue(temporalTestID, 1)));

    }


}
