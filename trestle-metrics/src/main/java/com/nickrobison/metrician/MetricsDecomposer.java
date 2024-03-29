package com.nickrobison.metrician;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Sampling;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by nrobison on 3/20/17.
 */
class MetricsDecomposer {

    private static final List<MetricPart<Counting, Long>> COUNTINGS;
    private static final List<MetricPart<Metered, Object>> METERED;
    private static final List<MetricPart<Sampling, Object>> SAMPLING;

    private final Map<String, Set<String>> namedMetricsComposition;
    private final Collection<RegexContainer<Set<String>>> regexComposition;

    static {
        COUNTINGS = new ArrayList<>(1);
        COUNTINGS.add(part(Counting::getCount, "count", MetricsListener.METRIC_TYPE_COUNTER));
        METERED = new ArrayList<>(4);
        METERED.add(part(Metered::getOneMinuteRate, "1minrt", MetricsListener.METRIC_TYPE_GAUGE));
        METERED.add(part(Metered::getFiveMinuteRate, "5minrt", MetricsListener.METRIC_TYPE_GAUGE));
        METERED.add(part(Metered::getFifteenMinuteRate, "15minrt", MetricsListener.METRIC_TYPE_GAUGE));
        METERED.add(part(Metered::getMeanRate, "meanrt", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING = new ArrayList<>(10);
        SAMPLING.add(part(s -> s.getSnapshot().getMin(), "min", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().getMax(), "max", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().getMean(), "mean", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().getMedian(), "median", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().getStdDev(), "stddev", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().get75thPercentile(), "75perc", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().get95thPercentile(), "95perc", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().get98thPercentile(), "98perc", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().get99thPercentile(), "99perc", MetricsListener.METRIC_TYPE_GAUGE));
        SAMPLING.add(part(s -> s.getSnapshot().get999thPercentile(), "999perc", MetricsListener.METRIC_TYPE_GAUGE));

    }

    MetricsDecomposer(Map<String, Set<String>> namedMetricsComposition,
                      Collection<RegexContainer<Set<String>>> regexComposition) {
        this.namedMetricsComposition = namedMetricsComposition;
        this.regexComposition = regexComposition;
    }

    Optional<Collection<String>> getAllowedParts(String metricName) {
        if (namedMetricsComposition.containsKey(metricName)) {
            return Optional.of(namedMetricsComposition.get(metricName));
        } else {
            for (RegexContainer<Set<String>> reg : regexComposition) {
                final Optional<Set<String>> match = reg.match(metricName);
                if (match.isPresent()) {
                    return Optional.of(match.get());
                }
            }
        }
        return Optional.empty();
    }

    private static <T, U> MetricPart<T, U> part(Function<T,U> getter, String suffix, String type) {
        return new MetricPart<T, U>() {
            @Override
            public U getData(T input) {
                return getter.apply(input);
            }

            @Override
            public String getSuffix() {
                return suffix;
            }

            @Override
            public String getMetricType() {
                return type;
            }
        };
    }

    PartsStreamer streamParts(String metricName) {
        final Predicate<String> p = getAllowedParts(metricName)
                .map(allowed -> (Predicate<String>) (allowed::contains))
                .orElse(part -> true);
        return new PartsStreamer(p);
    }

    static class PartsStreamer {
        private final Predicate<String> metricPredicate;

        private PartsStreamer(Predicate<String> metricPredicate) {
            this.metricPredicate = metricPredicate;
        }

        Stream<MetricPart<Counting, Long>> countings() {
            return COUNTINGS.stream()
                    .filter(metricPart -> metricPredicate.test(metricPart.getSuffix()));
        }

        Stream<MetricPart<Metered, Object>> metered() {
            return METERED.stream()
                    .filter(metricPart -> metricPredicate.test(metricPart.getSuffix()));
        }

        Stream<MetricPart<Sampling, Object>> samplings() {
            return SAMPLING.stream()
                    .filter(metricPart -> metricPredicate.test(metricPart.getSuffix()));
        }
    }
}
