package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.caching.tdtree.TDTree;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Created by nrobison on 8/18/16.
 */
@Singleton
public class TrestleCache {

    private final CacheManager cacheManager;
    private final Cache<TrestleIRI, Object> individualCache;
    private final ITrestleIndex<TrestleIRI> validIndex;
    private final ITrestleIndex<TrestleIRI> dbIndex;

    @Inject
    TrestleCache(ITrestleIndex<TrestleIRI> validIndex, ITrestleIndex<TrestleIRI> dbIndex) {
//        Create the index
        this.validIndex = validIndex;
        this.dbIndex = dbIndex;

        final Config cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
        final CachingProvider cachingProvider = Caching.getCachingProvider(cacheConfig.getString("cacheImplementation"));
        cacheManager = cachingProvider.getCacheManager();
        final MutableConfiguration<TrestleIRI, Object> individualCacheConfiguration = new MutableConfiguration<>();
        individualCacheConfiguration
                .setTypes(TrestleIRI.class, Object.class)
                .setStatisticsEnabled(true);
        individualCache = cacheManager.createCache("individual-cache", individualCacheConfiguration);
    }


    public <T> @Nullable T getIndividual(Class<T> clazz, TrestleIRI individualIRI) {
//        Valid first, then db
        final String individualID = individualIRI.getObjectID();
        final OffsetDateTime offsetDateTime = individualIRI.getObjectTemporal().orElse(OffsetDateTime.now());
//        TODO(nrobison): This shouldn't be Epoch second
        @Nullable final TrestleIRI indexValue = validIndex.getValue(individualID, offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond());
        if (indexValue != null) {
            return clazz.cast(individualCache.get(indexValue));
        } else {
            return clazz.cast(individualCache.get(individualIRI));
        }
    }

    public void writeIndividual(TrestleIRI individualIRI, long startTemporal, long endTemporal, Object value) {
//        Write to the cache and the index
        individualCache.put(individualIRI, value);
        validIndex.insertValue(individualIRI.getObjectID(), startTemporal, endTemporal, individualIRI);
    }
}
