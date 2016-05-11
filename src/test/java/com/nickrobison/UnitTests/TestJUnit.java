package com.nickrobison.UnitTests;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nrobison on 4/29/16.
 */
public class TestJUnit {

    @Test
    public void testAdd() {
        String str = "Junit is working fine";
        assertEquals("Junit is working fine", str);
    }
}
