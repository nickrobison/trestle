package com.nickrobison.trestle.benchmarks;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Created by nrobison on 8/1/16.
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(OntologyFloatParsing.class.getSimpleName())
//                .include(OntologyBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
