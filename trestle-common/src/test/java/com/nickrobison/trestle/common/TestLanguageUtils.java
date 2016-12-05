package com.nickrobison.trestle.common;

import org.junit.jupiter.api.Test;

import static com.nickrobison.trestle.common.LanguageUtils.checkLanguageCodeIsValid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 12/5/16.
 */
public class TestLanguageUtils {

    @Test
    public void testCodeValidation() {
        assertTrue(checkLanguageCodeIsValid("en"));
        assertTrue(checkLanguageCodeIsValid("en_GB"));
        assertFalse(checkLanguageCodeIsValid("nick"));
        assertFalse(checkLanguageCodeIsValid("fr_123_br"));
    }
}
