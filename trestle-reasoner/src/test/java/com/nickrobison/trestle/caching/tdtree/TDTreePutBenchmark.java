package com.nickrobison.trestle.caching.tdtree;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 5/2/17.
 */
@SuppressWarnings({"ForLoopReplaceableByForEach", "Duplicates"})
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.SingleShotTime})
public class TDTreePutBenchmark {
    private Random r;
    private TDTree<IndexValues> index;
    private IndexValues[] values;

    @Param({"100", "1000", "10000"})
    public int limit;


    @Setup(Level.Iteration)
    public void setup() throws Exception {
        index = new TDTree<>(10);
        values = IndexValues.generateTestRecords(1234, limit);
        System.out.println("Starting benchmark");
    }

    @Benchmark
    public void testUnwrap(Blackhole bh) {
        for (int i = 0; i < values.length; i++) {
            IndexValues value = values[i];
            bh.consume(value);
        }
    }

    @Benchmark
    public void testInsert() {
        for (int i = 0; i < values.length; i++) {
            IndexValues value = values[i];
            index.insertValue(value.getKey(), value.getStart(), value.getEnd(), value);
        }
    }

    @TearDown
    public void tearDown() {
        System.out.println("Tearing down");
        System.out.println(String.format("%s values, %s leafs", index.getCacheSize(), index.getLeafCount()));
    }

    public static void main(String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
                .include(TDTreePutBenchmark.class.getSimpleName())
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
