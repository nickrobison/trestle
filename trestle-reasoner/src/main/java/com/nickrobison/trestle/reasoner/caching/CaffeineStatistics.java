package com.nickrobison.trestle.reasoner.caching;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.nickrobison.metrician.Metrician;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Shared class for gathering statistics from a given Caffeine {@link com.github.benmanes.caffeine.cache.Cache}
 */
public class CaffeineStatistics implements StatsCounter {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineStatistics.class);

    private final Meter hits;
    private final Meter misses;
    private final Meter success;
    private final Meter failure;
    private final Timer loadsTimer;
    private final Meter evictions;
    private final Meter evictionsWeight;

    public CaffeineStatistics(Metrician metrician, String cachePrefix) {
        logger.debug("Instantiating metrics for {}", cachePrefix);
        hits = metrician.registerMeter(cachePrefix + "-hits");
        misses = metrician.registerMeter(cachePrefix + "-misses");
        success = metrician.registerMeter(cachePrefix + "-loads-success");
        failure = metrician.registerMeter(cachePrefix + "-loads-failure");
        loadsTimer = metrician.registerTimer(cachePrefix + "-loads-timer");
        evictions = metrician.registerMeter(cachePrefix + "-evictions");
        evictionsWeight = metrician.registerMeter(cachePrefix + "-evictions-weight");
    }


    @Override
    public void recordHits(int i) {
        this.hits.mark(i);
    }

    @Override
    public void recordMisses(int i) {
        this.misses.mark(i);
    }

    @Override
    public void recordLoadSuccess(long l) {
        success.mark();
        loadsTimer.update(l, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordLoadFailure(long l) {
        failure.mark();
        loadsTimer.update(l, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordEviction() {
//        This will be going away in 3.0
        this.recordEviction(1);
    }

    @Override
    public void recordEviction(int weight) {
        this.evictions.mark();
        this.evictionsWeight.mark(weight);
    }

    @Nonnull
    @Override
    public CacheStats snapshot() {
        return new CacheStats(
                hits.getCount(),
                misses.getCount(),
                success.getCount(),
                failure.getCount(),
                loadsTimer.getCount(),
                evictions.getCount(),
                evictionsWeight.getCount());
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
