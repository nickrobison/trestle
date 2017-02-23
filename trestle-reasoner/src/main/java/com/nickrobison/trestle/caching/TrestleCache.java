package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Created by nrobison on 8/18/16.
 */
@Singleton
public class TrestleCache {

    private static final Logger logger = LoggerFactory.getLogger(TrestleCache.class);
    public static final String INDIVIDUAL_CACHE = "individual-cache";
    private final TrestleUpgradableReadWriteLock cacheLock;
    private final CacheManager cacheManager;
    private final Cache<IRI, Object> individualCache;
    private final ITrestleIndex<TrestleIRI> validIndex;
    private final ITrestleIndex<TrestleIRI> dbIndex;

    @Inject
    TrestleCache(@Named("valid") ITrestleIndex<TrestleIRI> validIndex,
                 @Named("database") ITrestleIndex<TrestleIRI> dbIndex,
                 @Named("cacheLock") TrestleUpgradableReadWriteLock lock,
                 IndividualCacheEntryListener listener) {
//        Create the index
        this.validIndex = validIndex;
        this.dbIndex = dbIndex;
//        Create the lock
        this.cacheLock = lock;

        final Config cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
        final String cacheImplementation = cacheConfig.getString("cacheImplementation");
        logger.info("Creating TrestleCache with implementation {}", cacheImplementation);
        final CachingProvider cachingProvider = Caching.getCachingProvider(cacheImplementation);
        cacheManager = cachingProvider.getCacheManager();
        final MutableConfiguration<IRI, Object> individualCacheConfiguration = new MutableConfiguration<>();
        individualCacheConfiguration
                .setTypes(IRI.class, Object.class)
                .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<>(FactoryBuilder.factoryOf(listener), null, false, cacheConfig.getBoolean("synchronous")))
                .setStatisticsEnabled(true);
        logger.debug("Creating cache {}", INDIVIDUAL_CACHE);
        individualCache = cacheManager.createCache(INDIVIDUAL_CACHE, individualCacheConfiguration);
    }


    public <T> @Nullable T getIndividual(Class<T> clazz, TrestleIRI individualIRI) {
//        Valid first, then db
        try {
            cacheLock.lockRead();
            final String individualID = individualIRI.getObjectID();
            final OffsetDateTime offsetDateTime = individualIRI.getObjectTemporal().orElse(OffsetDateTime.now());
            logger.debug("Getting {} from cache @{}", individualIRI, offsetDateTime);
//        TODO(nrobison): This shouldn't be Epoch second
            @Nullable final TrestleIRI indexValue = validIndex.getValue(individualID, offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond());
            if (indexValue != null) {
                logger.debug("Index has {} for {} @{}", indexValue, individualIRI, offsetDateTime);
                return clazz.cast(individualCache.get(indexValue.getIRI()));
            } else {
                logger.debug("Index does not have {} @{}, going to cache", individualIRI, offsetDateTime);
                return clazz.cast(individualCache.get(individualIRI.getIRI()));
            }
        } catch (InterruptedException e) {
            logger.error("Unable to get read lock, returning null for {}", individualIRI.getIRI(), e);
            return null;
        } finally {
            cacheLock.unlockRead();
        }
    }

    public void writeIndividual(TrestleIRI individualIRI, long startTemporal, long endTemporal, Object value) {
//        Write to the cache and the index
        try {
            cacheLock.lockWrite();
            logger.debug("Adding {} to cache from {} to {}", individualIRI, startTemporal, endTemporal);
            individualCache.put(individualIRI.getIRI(), value);
            validIndex.insertValue(individualIRI.getObjectID(), startTemporal, endTemporal, individualIRI);
        } catch (InterruptedException e) {
            logger.error("Unable to get write lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }

    public void deleteIndividual(TrestleIRI trestleIRI) {
        final OffsetDateTime objectTemporal = trestleIRI.getObjectTemporal().orElse(OffsetDateTime.now());
        try {
            cacheLock.lockRead();
            @Nullable final TrestleIRI value = validIndex.getValue(trestleIRI.getObjectID(), objectTemporal.toEpochSecond());
            if (value != null) {
                try {
                   cacheLock.lockWrite();
                    logger.debug("Removing {} from index and cache", trestleIRI);
                    individualCache.remove(value.getIRI());
                    validIndex.getValue(value.getObjectID(), objectTemporal.toEpochSecond());
                } finally {
                    cacheLock.unlockWrite();
                }
            }
            logger.debug("{} does not exist in index and cache", trestleIRI);
        } catch (InterruptedException e) {
            logger.error("Unable to get lock", e);
        } finally {
            cacheLock.unlockRead();
        }
    }

    public void shutdown(boolean drop) {
        logger.info("Shutting down TrestleCache");
        if (drop) {
            logger.debug("Deleting caches");
            cacheManager.destroyCache(INDIVIDUAL_CACHE);
        }
        cacheManager.close();
    }

}
