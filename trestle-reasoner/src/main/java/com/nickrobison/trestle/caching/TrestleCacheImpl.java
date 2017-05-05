package com.nickrobison.trestle.caching;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
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
import java.io.NotSerializableException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Created by nrobison on 8/18/16.
 */
@Singleton
public class TrestleCacheImpl implements TrestleCache {

    private static final Logger logger = LoggerFactory.getLogger(TrestleCache.class);
    public static final String TRESTLE_OBJECT_CACHE = "trestle-object-cache";
    public static final String TRESTLE_INDIVIDUAL_CACHE = "trestle-individual-cache";
    private final TrestleUpgradableReadWriteLock cacheLock;
    private final CacheManager cacheManager;
    private final Cache<IRI, Object> trestleObjectCache;
    private final Cache<IRI, TrestleIndividual> trestleIndividualCache;
    private final ITrestleIndex<TrestleIRI> validIndex;
    private final ITrestleIndex<TrestleIRI> dbIndex;

    @Inject
    TrestleCacheImpl(@Named("valid") ITrestleIndex<TrestleIRI> validIndex,
                     @Named("database") ITrestleIndex<TrestleIRI> dbIndex,
                     @Named("cacheLock") TrestleUpgradableReadWriteLock lock,
                     TrestleObjectCacheEntryListener listener,
                     Metrician metrician) {
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
//        Create trestle object cache
        final MutableConfiguration<IRI, Object> trestleObjectCacheConfiguration = new MutableConfiguration<>();
        trestleObjectCacheConfiguration
                .setTypes(IRI.class, Object.class)
                .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<>(FactoryBuilder.factoryOf(listener), null, false, cacheConfig.getBoolean("synchronous")))
                .setStatisticsEnabled(true);
        logger.debug("Creating cache {}", TRESTLE_OBJECT_CACHE);
        trestleObjectCache = cacheManager.createCache(TRESTLE_OBJECT_CACHE, trestleObjectCacheConfiguration);

//        Create the trestle individual cache
        final MutableConfiguration<IRI, TrestleIndividual> trestleIndividualCacheConfiguration = new MutableConfiguration<>();
        trestleIndividualCacheConfiguration
                .setTypes(IRI.class, TrestleIndividual.class)
                .setStatisticsEnabled(true);
        logger.debug("Creating cache {}", TRESTLE_INDIVIDUAL_CACHE);
        this.trestleIndividualCache = cacheManager.createCache(TRESTLE_INDIVIDUAL_CACHE, trestleIndividualCacheConfiguration);

//        Enable metrics
        metrician.registerMetricSet(new TrestleCacheMetrics());
    }

//    TrestleObject methods


    @Override
    public <T> @Nullable T getTrestleObject(Class<T> clazz, TrestleIRI individualIRI) {
//        Valid first, then db
        try {
            cacheLock.lockRead();
            final String individualID = individualIRI.getObjectID();
            final OffsetDateTime offsetDateTime = individualIRI.getObjectTemporal().orElse(OffsetDateTime.now());
            logger.debug("Getting {} from cache @{}", individualIRI, offsetDateTime);
            @Nullable final TrestleIRI indexValue = validIndex.getValue(individualID, offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
            if (indexValue != null) {
                logger.debug("Index has {} for {} @{}", indexValue, individualIRI, offsetDateTime);
                return clazz.cast(trestleObjectCache.get(indexValue.getIRI()));
            } else {
                logger.debug("Index does not have {} @{}, going to cache", individualIRI, offsetDateTime);
                return clazz.cast(trestleObjectCache.get(individualIRI.getIRI()));
            }
        } catch (InterruptedException e) {
            logger.error("Unable to get read lock, returning null for {}", individualIRI.getIRI(), e);
            return null;
        } finally {
            cacheLock.unlockRead();
        }
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, long startTemporal, long endTemporal, @NonNull Object value) {
//        Write to the cache and the index
        try {
            cacheLock.lockWrite();
            logger.debug("Adding {} to cache from {} to {}", individualIRI, startTemporal, endTemporal);
            trestleObjectCache.put(individualIRI.getIRI(), value);
            validIndex.insertValue(individualIRI.getObjectID(), startTemporal, endTemporal, individualIRI);
        } catch (InterruptedException e) {
            logger.error("Unable to get write lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, long atTemporal, @NonNull Object value) {
        //        Write to the cache and the index
        try {
            cacheLock.lockWrite();
            logger.debug("Adding {} to cache at {}", individualIRI, atTemporal);
            trestleObjectCache.put(individualIRI.getIRI(), value);
            validIndex.insertValue(individualIRI.getObjectID(), atTemporal, individualIRI);
        } catch (InterruptedException e) {
            logger.error("Unable to get write lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }

    @Override
    public void deleteTrestleObject(TrestleIRI trestleIRI) {
        final OffsetDateTime objectTemporal = trestleIRI.getObjectTemporal().orElse(OffsetDateTime.now());
        try {
            cacheLock.lockRead();
            @Nullable final TrestleIRI value = validIndex.getValue(trestleIRI.getObjectID(), objectTemporal.toInstant().toEpochMilli());
            if (value != null) {
                try {
                    cacheLock.lockWrite();
                    logger.debug("Removing {} from index and cache", trestleIRI);
                    trestleObjectCache.remove(value.getIRI());
                    validIndex.deleteValue(value.getObjectID(), objectTemporal.toInstant().toEpochMilli());
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

//    Trestle Individual methods

    @Override
    public @Nullable TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return this.trestleIndividualCache.get(individual.getIRI());
    }

    @Override
    public void writeTrestleIndividual(OWLNamedIndividual key, TrestleIndividual value) {
        this.trestleIndividualCache.put(key.getIRI(), value);
    }

    @Override
    public void deleteTrestleIndividual(OWLNamedIndividual individual) {
        final boolean remove = this.trestleIndividualCache.remove(individual.getIRI());
        if (remove) {
            logger.debug("Removed {} from the cache", individual);
        } else {
            logger.debug("Cache does not have {}", individual);
        }
    }

    /**
     * Get statistics for the underlying caches
     *
     * @return - {@link TrestleCacheStatistics}
     */
    public @Nullable TrestleCacheStatistics getCacheStatistics() {
        try {
            cacheLock.lockRead();
            return new TrestleCacheStatistics(
                    this.validIndex.getCacheSize(),
                    this.validIndex.calculateFragmentation(),
                    this.dbIndex.getCacheSize(),
                    this.dbIndex.calculateFragmentation());
        } catch (InterruptedException e) {
            logger.error("Cannot get read lock", e);
            return null;
        } finally {
            cacheLock.unlockRead();
        }
    }

    @Override
    public void shutdown(boolean drop) {
        logger.info("Shutting down TrestleCache");
        if (drop) {
            logger.debug("Deleting caches");
            cacheManager.destroyCache(TRESTLE_OBJECT_CACHE);
            cacheManager.destroyCache(TRESTLE_INDIVIDUAL_CACHE);
        }
        cacheManager.close();
    }

}
