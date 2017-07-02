package com.nickrobison.trestle.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.LocalDate;

/**
 * Created by nrobison on 5/9/16.
 */
public class DateFieldUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateFieldUtils.class);
        public static final int TIMEDATASIZE = 2 * Long.BYTES;

    private DateFieldUtils() {

    }

    /**
     * Writes a LocalDate pair into a byte array as longs
     * @param start LocalDate - Start date
     * @param end LocalDate - End date
     * @return byte[] - Byte array of start/end long pair
     */
    public static byte[] writeDateField(LocalDate start, LocalDate end) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[TIMEDATASIZE]);
        bb.putLong(start.toEpochDay());
        bb.putLong(end.toEpochDay());

        return bb.array();
    }

    /**
     * Returns a constructed LocalDate from a byte[] of start/end long pair
     * @param dateField byte[] - Start/end long pair
     * @return LocalDate - Start date from byte[]
     */
    public static LocalDate readStartDate(byte[] dateField) {
        ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong());
    }

    /**
     * Returns a constructed LocalDate from a byte[] of start/end long pair
     * @param dateField byte[] - Start/end long pair
     * @return LocalDate - End date from byte[]
     */
    public static LocalDate readExpirationDate(byte[] dateField) {
        ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong(Long.BYTES));
    }
}
