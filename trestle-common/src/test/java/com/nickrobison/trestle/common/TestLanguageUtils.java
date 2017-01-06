package com.nickrobison.trestle.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.nickrobison.trestle.common.LanguageUtils.checkLanguageCodeIsValid;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 12/5/16.
 */
@Tag("unit")
public class TestLanguageUtils {

    @Test
    public void testCodeValidation() {
        assertAll(() -> assertTrue(checkLanguageCodeIsValid("en"), "English should be ok"),
                () -> assertTrue(checkLanguageCodeIsValid("en-GB"), "English GB should be ok"),
                () -> assertFalse(checkLanguageCodeIsValid("en_GB"), "English underscores should fail"),
                () -> assertFalse(checkLanguageCodeIsValid("nick"), "Nick should fail"),
                () -> assertFalse(checkLanguageCodeIsValid("fr_123_br"), "Random string should fail"));
    }
}
