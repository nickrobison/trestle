package com.nickrobison.trestle.reasoner.caching.tdtree;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 5/3/17.
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.SingleShotTime})
@CompilerControl(CompilerControl.Mode.EXCLUDE)
@SuppressWarnings("Duplicates")
public class TDTreeGetBenchmark {
    private Random r;
    private TDTree<IndexValues> index;
    private IndexValues[] values;

    @Param({"100", "1000", "10000"})
    public int limit;


    @Setup(Level.Trial)
    public void setup() throws Exception {
        index = new TDTree<>(10);
        values = IndexValues.generateTestRecords(1234, limit);
        System.out.println("Writing records");
        Arrays.stream(values)
                .forEach(value -> index.insertValue(value.getKey(), value.getStart(), value.getEnd(), value));
        System.out.println(String.format("%s records, %s leafs", index.getCacheSize(), index.getLeafCount()));
        System.out.println("Starting benchmark");
    }


    @Benchmark
    public void testGet(Blackhole bh) {
        for (int i = 0; i < values.length; i++) {
            IndexValues value = values[i];
            bh.consume(index.getValue(value.getKey(), value.getStart()));
        }
    }

    @TearDown
    public void tearDown() {
        System.out.println("Tearing down");
        System.out.println(String.format("%s values, %s leafs", index.getCacheSize(), index.getLeafCount()));
    }

    public static void main(String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
                .include(TDTreeGetBenchmark.class.getSimpleName())
                .threads(1)
                .forks(1)
                .warmupIterations(8)
                .measurementIterations(8)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(options).run();
    }
}
