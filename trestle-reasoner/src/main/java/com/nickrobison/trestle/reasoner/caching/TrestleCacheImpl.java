package com.nickrobison.trestle.reasoner.caching;

import com.google.common.collect.ImmutableSet;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.reasoner.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Created by nrobison on 8/18/16.
 */
@SuppressWarnings({"squid:S2142"})
@Singleton
public class TrestleCacheImpl implements TrestleCache {

    private static final Logger logger = LoggerFactory.getLogger(TrestleCacheImpl.class);
    private static final String TRESTLE_OBJECT_CACHE = "trestle-object-cache";
    private static final String TRESTLE_INDIVIDUAL_CACHE = "trestle-individual-cache";
    private final TrestleUpgradableReadWriteLock cacheLock;
    private final CacheManager cacheManager;
    private final Cache<IRI, Object> trestleObjectCache;
    private final @GuardedBy("cacheLock") Cache<IRI, TrestleIndividual> trestleIndividualCache;
    private final @GuardedBy("cacheLock") ITrestleIndex<TrestleIRI> validIndex;
    private final @GuardedBy("cacheLock") ITrestleIndex<TrestleIRI> dbIndex;

    @Inject
    @SuppressWarnings({"argument.type.incompatible"})
    TrestleCacheImpl(@Named("valid") ITrestleIndex<TrestleIRI> validIndex,
                     @Named("database") ITrestleIndex<TrestleIRI> dbIndex,
                     @Named("cacheLock") TrestleUpgradableReadWriteLock lock,
                     TrestleObjectCacheEntryListener listener,
                     Metrician metrician,
                     CacheManager manager) {
//        Setup the indexes
        this.validIndex = validIndex;
        this.dbIndex = dbIndex;
//        Create the lock
        this.cacheLock = lock;
//        Setup the cache manager
        this.cacheManager = manager;
        final Config cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
//        Create trestle object cache
        final MutableConfiguration<IRI, Object> trestleObjectCacheConfiguration = new MutableConfiguration<>();
        trestleObjectCacheConfiguration
                .setTypes(IRI.class, Object.class)
                .setStatisticsEnabled(true);
        logger.debug("Creating cache {}", TRESTLE_OBJECT_CACHE);
        this.trestleObjectCache = cacheManager.getCache(TRESTLE_OBJECT_CACHE, IRI.class, Object.class);

        final MutableCacheEntryListenerConfiguration<IRI, Object> config = new MutableCacheEntryListenerConfiguration<>(FactoryBuilder.factoryOf(listener), null, false, cacheConfig.getBoolean("synchronous"));
        trestleObjectCache.registerCacheEntryListener(config);

//        Create the trestle individual cache
        logger.debug("Creating cache {}", TRESTLE_INDIVIDUAL_CACHE);
        this.trestleIndividualCache = cacheManager.getCache(TRESTLE_INDIVIDUAL_CACHE, IRI.class, TrestleIndividual.class);

//        Enable metrics
        metrician.registerMetricSet(new TrestleCacheMetrics(ImmutableSet.of(TRESTLE_INDIVIDUAL_CACHE, TRESTLE_OBJECT_CACHE)));
    }

//    TrestleObject methods


    @Override
    public <T> @Nullable T getTrestleObject(Class<T> clazz, TrestleIRI individualIRI) {
//        Valid first, then db
        try {
            cacheLock.lockRead();
            final String individualID = individualIRI.getObjectID();
            final OffsetDateTime offsetDateTime = individualIRI.getObjectTemporal().orElse(OffsetDateTime.now());
            logger.trace("Looking for {} from cache @{}", individualIRI, offsetDateTime);
            @Nullable final TrestleIRI validIndexValue = validIndex.getValue(individualID, offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
            if (validIndexValue != null) {
                logger.trace("Valid Index has {} for {} @{}", validIndexValue, individualIRI, offsetDateTime);
                @Nullable final TrestleIRI dbIndexValue;
                final Optional<OffsetDateTime> dbTemporal = individualIRI.getDbTemporal();
                if (dbTemporal.isPresent()) {
                    dbIndexValue = dbIndex.getValue(validIndexValue.toString(), dbTemporal.get().toInstant().toEpochMilli());
                } else {
                    dbIndexValue = dbIndex.getValue(validIndexValue.toString(), OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());
                }
                if (dbIndexValue != null) {
                    logger.trace("DB Index has {} for {} @{}", dbIndexValue, individualIRI, offsetDateTime);
                    return clazz.cast(trestleObjectCache.get(dbIndexValue.getIRI()));
                }
            }
            logger.debug("Indexes do not have {} @{}, going directly to cache", individualIRI, offsetDateTime);
            return clazz.cast(trestleObjectCache.get(individualIRI.getIRI()));
        } catch (InterruptedException e) {
            logger.error("Unable to get read lock, returning null for {}", individualIRI.getIRI(), e);
            return null;
        } finally {
            cacheLock.unlockRead();
        }
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal, Object value) {
        writeTrestleObject(individualIRI, startTemporal, endTemporal, OffsetDateTime.now(ZoneOffset.UTC), null, value);
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal, OffsetDateTime dbStartTemporal, @Nullable OffsetDateTime dbEndTemporal, Object value) {
        //        Write to the cache and the index
        try {
            cacheLock.lockWrite();
            logger.debug("Adding {} to cache from {} to {}", individualIRI, startTemporal, endTemporal);
            trestleObjectCache.put(individualIRI.getIRI(), value);
            logger.debug("Added to cache");
            final long endTemporalMillis;
            final long dbEndTemporalMillis;
            if (endTemporal == null) {
                endTemporalMillis = validIndex.getMaxValue();
            } else {
                endTemporalMillis = endTemporal.toInstant().toEpochMilli();
            }
            if (dbEndTemporal == null) {
                dbEndTemporalMillis = dbIndex.getMaxValue();
            } else {
                dbEndTemporalMillis = dbEndTemporal.toInstant().toEpochMilli();
            }
            final long startTemporalMillis = startTemporal.toInstant().toEpochMilli();
            final long dbStartTemporalMillis = dbStartTemporal.toInstant().toEpochMilli();

//            Begin writing into the cache

//            Figure out if we already have a record for this object at this validity interval
            final OffsetDateTime validAt = individualIRI.getObjectTemporal().orElseThrow(() -> new IllegalStateException("Cannot add object to cache without a temporal"));
            final long validAtMillis = validAt.toInstant().toEpochMilli();
            @Nullable final TrestleIRI validIndexValue = validIndex.getValue(individualIRI.getObjectID(), validAtMillis);
            final TrestleIRI dbIndexKey;
//            If we don't have anything in the cache, write us as the new key, otherwise use the old key, but update the temporals
            if (validIndexValue == null) {
                dbIndexKey = individualIRI.withoutDatabase();
                validIndex.insertValue(individualIRI.getObjectID(),
                        startTemporalMillis,
                        endTemporalMillis,
                        dbIndexKey);
                logger.trace("Added {} to valid index", dbIndexKey);
            } else {
                validIndex.setKeyTemporals(validIndexValue.getObjectID(), validAtMillis, startTemporalMillis, endTemporalMillis);
                dbIndexKey = validIndexValue.withoutDatabase();
            }

//            Do we have a DB temporal that's currently valid?

            final long dbAtMillis = individualIRI.getDbTemporal().orElse(OffsetDateTime.now()).toInstant().toEpochMilli();

            @Nullable final TrestleIRI dbIndexValue = dbIndex.getValue(dbIndex.toString(), dbAtMillis);
//            If we don't have a DB record at this time, insert a new one
            if (dbIndexValue == null) {
                dbIndex.insertValue(dbIndexKey.toString(),
                        dbStartTemporalMillis,
                        dbEndTemporalMillis,
                        individualIRI);
            } else { // If we have a record, just update the key temporals
                dbIndex.setKeyTemporals(dbIndexKey.toString(),
                        dbStartTemporalMillis,
                        dbEndTemporalMillis);
            }
            logger.trace("Added {} to db index", dbIndexKey);
        } catch (InterruptedException e) {
            logger.error("Unable to get write lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }


    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime atTemporal, Object value) {
        writeTrestleObject(individualIRI, atTemporal, OffsetDateTime.now(ZoneOffset.UTC), null, value);
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime atTemporal, OffsetDateTime dbStartTemporal, @Nullable OffsetDateTime dbEndTemporal, Object value) {
        //        Write to the cache and the index
        try {
            cacheLock.lockWrite();
            logger.debug("Adding {} to cache at {}", individualIRI, atTemporal);
            trestleObjectCache.put(individualIRI.getIRI(), value);
            final TrestleIRI withoutDatabase = individualIRI.withoutDatabase();
            validIndex.insertValue(individualIRI.getObjectID(),
                    atTemporal.toInstant().toEpochMilli(),
                    individualIRI.withoutDatabase());
            logger.trace("Added {} to valid index", withoutDatabase);

            final long dbEndTemporalMillis;
            if (dbEndTemporal == null) {
                dbEndTemporalMillis = dbIndex.getMaxValue();
            } else {
                dbEndTemporalMillis = dbEndTemporal.toInstant().toEpochMilli();
            }
            dbIndex.insertValue(
                    withoutDatabase.toString(),
                    dbStartTemporal.toInstant().toEpochMilli(),
                    dbEndTemporalMillis,
                    individualIRI
            );
        } catch (InterruptedException e) {
            logger.error("Unable to get write lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }

    @Override
    @SuppressWarnings({"squid:S1141"})
    public void deleteTrestleObject(TrestleIRI trestleIRI) {
        final OffsetDateTime objectTemporal = trestleIRI.getObjectTemporal().orElse(OffsetDateTime.now());
        try {
            cacheLock.lockWrite();
            @Nullable final TrestleIRI value = validIndex.getValue(trestleIRI.getObjectID(), objectTemporal.toInstant().toEpochMilli());
            if (value != null) {

//                If we have a record, try to find the one that exists in the database cache
                logger.debug("Removing {} from index and cache", trestleIRI);
                validIndex.deleteValue(trestleIRI.getObjectID(), objectTemporal.toInstant().toEpochMilli());
                trestleIRI.getDbTemporal().ifPresent(temporal -> dbIndex.deleteValue(trestleIRI.withoutDatabase().toString(), temporal.toInstant().toEpochMilli()));
                trestleObjectCache.remove(value.getIRI());
            } else {
                logger.trace("{} does not exist in index", trestleIRI);
//                If we don't have anything in the index, try to delete from the cache anyways
                this.trestleIndividualCache.remove(trestleIRI.getIRI());
            }
        } catch (InterruptedException e) {
            logger.error("Unable to get lock", e);
        } finally {
            cacheLock.unlockWrite();
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
                    this.validIndex.getIndexSize(),
                    this.validIndex.calculateFragmentation(),
                    this.dbIndex.getIndexSize(),
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
