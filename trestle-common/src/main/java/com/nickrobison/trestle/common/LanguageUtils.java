package com.nickrobison.trestle.common;

import com.ibm.icu.util.ULocale;

import java.util.Arrays;

/**
 * Created by nrobison on 12/5/16.
 */
public class LanguageUtils {

    /**
     * Check if a given language shortcode matches anything in the ICU database
     * @param language - String shortcode to match
     * @return - Boolean if shortcode matches database
     */
    public static boolean checkLanguageCodeIsValid(String language) {
        return Arrays.stream(ULocale.getAvailableLocales())
                .map(ULocale::getBaseName)
                .anyMatch(locale -> locale.equals(language));
    }
}
