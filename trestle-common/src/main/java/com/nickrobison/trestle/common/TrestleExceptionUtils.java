package com.nickrobison.trestle.common;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nickrobison on 2/12/18.
 */
public class TrestleExceptionUtils {

    /**
     * Simple helper to deal with Checker warnings about potentially null strings.
     * If the string is null, it returns an empty string
     * Usually these happen when throwing exceptions or logging
     *
     * @param string - {@link String} potentially null string
     * @return - {@link String} input string or empty string if null
     */
    public static String handleNullOrEmptyString(@Nullable String string) {
        return string == null ? "" : string;
    }
}
