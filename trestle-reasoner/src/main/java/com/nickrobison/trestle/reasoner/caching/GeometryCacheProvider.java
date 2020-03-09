package com.nickrobison.trestle.reasoner.caching;

import com.google.common.collect.ImmutableSet;
import com.nickrobison.metrician.Metrician;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by nickrobison on 12/18/17.
 */
public class GeometryCacheProvider implements Provider<Cache<Integer, Geometry>> {
    private static final Logger logger = LoggerFactory.getLogger(GeometryCacheProvider.class);
    private static final String TRESTLE_GEOMETRY_CACHE = "trestle-geometry-cache";

    private final Cache<Integer, Geometry> cache;

    @Inject
    public GeometryCacheProvider(CacheManager manager, Metrician metrician) {
//        Create the cache config
        final MutableConfiguration<Integer, Geometry> cacheConfig = new MutableConfiguration<>();
        cacheConfig
                .setTypes(Integer.class, Geometry.class).
                setStatisticsEnabled(true);
        this.cache = manager.getCache(TRESTLE_GEOMETRY_CACHE, Integer.class, Geometry.class);

//        Register with Metrician
        metrician.registerMetricSet(new TrestleCacheMetrics(ImmutableSet.of(TRESTLE_GEOMETRY_CACHE)));
    }

    /**
     * Gets the geometry cache
     * @return - {@link Cache} with key type {@link Integer} and value {@link Geometry}
     */
    @Override
    public Cache<Integer, Geometry> get() {
        return this.cache;
    }
}
