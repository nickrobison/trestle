package com.nickrobison.gaulintegrator;

import com.nickrobison.gaulintegrator.UnitTests.GAULRecordTests;
import com.nickrobison.gaulintegrator.UnitTests.IDTests;
import com.nickrobison.gaulintegrator.UnitTests.TemporalAdjustmentTests;
import com.nickrobison.gaulintegrator.UnitTests.TestJUnit;
import org.apache.log4j.Logger;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Created by nrobison on 4/29/16.
 */
public class TestRunner {

    private static final Logger logger = Logger.getLogger(TestRunner.class);
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(TestJUnit.class, GAULRecordTests.class, IDTests.class, TemporalAdjustmentTests.class);
        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
            logger.error(failure.toString());
        }

        logger.info(result.wasSuccessful());
    }
}
