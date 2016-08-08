package com.nickrobison.trestle.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Objects;

/**
 * Created by nrobison on 8/7/16.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@SuppressWarnings({"argument.type.incompatible"})
public class OntologyFloatParsing {

    private static String startDouble = Objects.toString(Long.MAX_VALUE);

    @Benchmark
    public void measureExceptionHandling(Blackhole bh) {
        Integer testInt = null;
        Long testLong = null;
        try {
            testInt = Integer.parseInt(startDouble);
        } catch (NumberFormatException e) {

        }

        if (testInt == null) {
            try {
                testLong = Long.parseLong(startDouble);
            } catch (NumberFormatException e) {

            }
        }

        bh.consume(testLong);
    }

    @Benchmark
    public void measureLongParsing(Blackhole bh) {
        Integer testInt = null;
        Long testLong = null;
        if (!startDouble.contains(".")) {
//            Try to get the int out
            long l = Long.parseLong(startDouble);
            l = l >> (Long.BYTES * 8);

            if (l == 0) {
                bh.consume(Integer.parseInt(startDouble));
            }
            if (l > 1) {
                bh.consume(l);
            }
        }
    }
}
