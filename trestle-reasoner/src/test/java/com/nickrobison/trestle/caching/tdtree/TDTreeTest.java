package com.nickrobison.trestle.caching.tdtree;

import com.arjuna.ats.arjuna.AtomicAction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 2/9/17.
 */
@SuppressWarnings("Duplicates")
public class TDTreeTest {

    @Test
    public void testSimpleFunction() throws Exception {

        int[] maxValueArray = {10, 93, 100, 174, 1000, 1233, 10000, 12346, 100000, 973456, 1000000, Integer.MAX_VALUE};

        Arrays.stream(maxValueArray)
                .forEach(value -> {
                    try {
                        simpleTest(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void simpleTest(int maxValue) throws Exception {
        TDTree.maxValue = maxValue;
        final TDTree<String> tdTree = new TDTree<>(2);
        System.out.println(String.format("Running with %d max value", maxValue));

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


    @Test
    public void testConcurrency() throws Exception {
        int maxValue = 20;
        final Random random = new Random();
        TDTree.maxValue = maxValue;
        final TDTree<Long> tree = new TDTree<>(2);
        final Thread t1 = new Thread(() -> {
            while (true) {
                AtomicAction a = new AtomicAction();
                a.begin();
                System.out.println("Thread 1 running");
                final int randomKey = random.nextInt();
                final int randomStart = random.nextInt(maxValue);
                final long randomValue = random.nextLong();
                tree.insertValue(Integer.toString(randomKey), randomStart, randomValue);
                a.commit();
//                a = new AtomicAction();
//                a.begin();
//                @Nullable final Long value = tree.getValue(Integer.toString(randomKey), randomStart);
//                a.commit();
//                assertAll(() -> assertNotNull(value),
//                        () -> assertEquals(randomValue, (long) value));
            }
        });
        final Thread t2 = new Thread(() -> {
            while (true) {
                AtomicAction a = new AtomicAction();
                System.out.println("Thread 2 running");
                final int randomKey = random.nextInt();
                final int randomStart = random.nextInt(maxValue);
                final long randomValue = random.nextLong();
                a.begin();
                tree.insertValue(Integer.toString(randomKey), randomStart, randomValue);
                a.commit();
//                a = new AtomicAction();
//                a.begin();
//                @Nullable final Long value = tree.getValue(Integer.toString(randomKey), randomStart);
//                a.commit();
//                assertAll(() -> assertNotNull(value),
//                        () -> assertEquals(randomValue, (long) value));
            }
        });
//        Try to read/write from two threads
        t1.start();
        t2.start();
        Thread.sleep(10000);
    }


}
