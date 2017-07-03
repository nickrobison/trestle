package com.nickrobison.trestle.common;

import java.nio.ByteBuffer;
import java.time.LocalDate;

/**
 * Created by nrobison on 5/9/16.
 */
@SuppressWarnings({"pmd:LawOfDemeter"})
public class DateFieldUtils {
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
        final ByteBuffer bb = ByteBuffer.wrap(new byte[TIMEDATASIZE]);
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
        final ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong());
    }

    /**
     * Returns a constructed LocalDate from a byte[] of start/end long pair
     * @param dateField byte[] - Start/end long pair
     * @return LocalDate - End date from byte[]
     */
    public static LocalDate readExpirationDate(byte[] dateField) {
        final ByteBuffer bb = ByteBuffer.wrap(dateField);
        return LocalDate.ofEpochDay(bb.getLong(Long.BYTES));
    }
}
