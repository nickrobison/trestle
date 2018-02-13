package com.nickrobison.trestle.reasoner.caching;

import com.google.common.collect.ImmutableSet;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.reasoner.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
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
import java.time.*;
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
    //    Offset value to adjust for the fact that the cache can only support positive temporal values
    private static final long OFFSET_MILLIS = Duration.between(LocalDate.of(0, 1, 1)
                    .atStartOfDay().toInstant(ZoneOffset.UTC),
            Instant.ofEpochMilli(0)).toMillis();
    private final TrestleUpgradableReadWriteLock cacheLock;
    private final CacheManager cacheManager;
    private final Cache<IRI, Object> trestleObjectCache;
    private final @GuardedBy("cacheLock") Cache<IRI, TrestleIndividual> trestleIndividualCache;
    private final @GuardedBy("cacheLock") ITrestleIndex<TrestleIRI> validIndex;
    private final @GuardedBy("cacheLock") ITrestleIndex<TrestleIRI> dbIndex;
    private final MutableCacheEntryListenerConfiguration<IRI, Object> objectEvictionListener;

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

        objectEvictionListener = new MutableCacheEntryListenerConfiguration<>(FactoryBuilder.factoryOf(listener), null, false, cacheConfig.getBoolean("synchronous"));
        trestleObjectCache.registerCacheEntryListener(objectEvictionListener);

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
            @Nullable final TrestleIRI validIndexValue = validIndex.getValue(individualID, adjustOffsetDateTime(offsetDateTime));
            if (validIndexValue != null) {
                logger.trace("Valid Index has {} for {} @{}", validIndexValue, individualIRI, offsetDateTime);
                @Nullable final TrestleIRI dbIndexValue;
                final Optional<OffsetDateTime> dbTemporal = individualIRI.getDbTemporal();
                if (dbTemporal.isPresent()) {
                    dbIndexValue = dbIndex.getValue(validIndexValue.toString(), adjustOffsetDateTime(dbTemporal.get()));
                } else {
                    dbIndexValue = dbIndex.getValue(validIndexValue.toString(), adjustOffsetDateTime(OffsetDateTime.now()));
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
            logger.debug("Adding {} to cache from {} to {}", individualIRI, startTemporal, endTemporal == null ? "" : endTemporal);
            trestleObjectCache.put(individualIRI.getIRI(), value);
            logger.debug("Added to cache");
            final long endTemporalMillis;
            final long dbEndTemporalMillis;
            if (endTemporal == null) {
                endTemporalMillis = validIndex.getMaxValue();
            } else {
                endTemporalMillis = adjustOffsetDateTime(endTemporal);
            }
            if (dbEndTemporal == null) {
//                Make sure to add the OFFSET_MILLIS
                dbEndTemporalMillis = dbIndex.getMaxValue();
            } else {
                dbEndTemporalMillis = adjustOffsetDateTime(dbEndTemporal);
            }
            final long startTemporalMillis = adjustOffsetDateTime(startTemporal);
            final long dbStartTemporalMillis = adjustOffsetDateTime(dbStartTemporal);

//            Begin writing into the cache

//            Figure out if we already have a record for this object at this validity interval
            final OffsetDateTime validAt = individualIRI.getObjectTemporal().orElseThrow(() -> new IllegalStateException("Cannot add object to cache without a temporal"));
            final long validAtMillis = adjustOffsetDateTime(validAt);
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

            final long dbAtMillis = adjustOffsetDateTime(individualIRI.getDbTemporal().orElse(OffsetDateTime.now()));

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
                    adjustOffsetDateTime(atTemporal),
                    individualIRI.withoutDatabase());
            logger.trace("Added {} to valid index", withoutDatabase);

            final long dbEndTemporalMillis;
            if (dbEndTemporal == null) {
                dbEndTemporalMillis = dbIndex.getMaxValue();
            } else {
                dbEndTemporalMillis = adjustOffsetDateTime(dbEndTemporal);
            }
            dbIndex.insertValue(
                    withoutDatabase.toString(),
                    adjustOffsetDateTime(dbStartTemporal),
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
            final long objectMillis = adjustOffsetDateTime(objectTemporal);
            @Nullable final TrestleIRI value = validIndex.getValue(trestleIRI.getObjectID(), objectMillis);
            if (value != null) {

//                If we have a record, try to find the one that exists in the database cache
                logger.debug("Removing {} from index and cache", trestleIRI);
                validIndex.deleteValue(trestleIRI.getObjectID(), objectMillis);
                trestleIRI.getDbTemporal().ifPresent(temporal -> dbIndex.deleteValue(trestleIRI.withoutDatabase().toString(), adjustOffsetDateTime(temporal)));
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

    @Override
    public @Nullable TrestleCacheStatistics getCacheStatistics() {
        try {
            cacheLock.lockRead();
            final TrestleCacheStatistics cacheStats = new TrestleCacheStatistics(
                    OFFSET_MILLIS,
//                    These are currently the same value, so we can ignore them
                    validIndex.getMaxValue(),
                    this.validIndex.getIndexSize(),
                    this.validIndex.calculateFragmentation(),
                    this.dbIndex.getIndexSize(),
                    this.dbIndex.calculateFragmentation());

//            Check if the indexes are TDTrees, if so, dump the leafs
            if (this.validIndex instanceof TDTree) {
                cacheStats.addValidLeafStats(this.validIndex.getLeafStatistics());
            }

            if (this.dbIndex instanceof TDTree) {
                cacheStats.addDBLeafStats(this.dbIndex.getLeafStatistics());
            }
            return cacheStats;
        } catch (InterruptedException e) {
            logger.error("Cannot get read lock", e);
            return null;
        } finally {
            cacheLock.unlockRead();
        }
    }

    @Override
    public void rebuildValidIndex() {
        try {
            cacheLock.lockWrite();
            this.validIndex.rebuildIndex();
        } catch (InterruptedException e) {
            logger.error("Cannot get write lock to rebuild valid index", e);
            Thread.currentThread().interrupt();
        } finally {
            cacheLock.unlockWrite();
        }
    }

    @Override
    public void rebuildDBIndex() {
        try {
            cacheLock.lockWrite();
            this.dbIndex.rebuildIndex();
        } catch (InterruptedException e) {
            logger.error("Cannot get write lock to rebuild db index", e);
            Thread.currentThread().interrupt();
        } finally {
            cacheLock.unlockWrite();
        }
    }

    @Override
    public void purgeIndividualCache() {
        logger.debug("Purging individual cache");
        this.trestleIndividualCache.removeAll();
    }

    @Override
    public void purgeObjectCache() {
        logger.debug("Purging object cache");
        try {
            cacheLock.lockWrite();
//            Remove the listener and do everything
            this.trestleObjectCache.deregisterCacheEntryListener(this.objectEvictionListener);
            this.trestleObjectCache.removeAll();
            this.validIndex.dropIndex();
            this.dbIndex.dropIndex();
//            Purge indexes
        } catch (InterruptedException e) {
            logger.error("Cannot get write lock to Purge object cache", e);
            Thread.currentThread().interrupt();
        } finally {
            cacheLock.unlockWrite();
//            Re-enable the listener
            this.trestleObjectCache.registerCacheEntryListener(this.objectEvictionListener);
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

    /**
     * Adjusts a given {@link OffsetDateTime} by adding the number of milliseconds between 0000-01-01 and the Unix epoch
     * Provided by {@link TrestleCacheImpl#OFFSET_MILLIS}
     *
     * @param odt - {@link OffsetDateTime} to adjust
     * @return - {@link Long} milliseconds from {@link TrestleCacheImpl#OFFSET_MILLIS}
     */
    private static long adjustOffsetDateTime(OffsetDateTime odt) {
        return odt.atZoneSameInstant(ZoneOffset.UTC)
                .toInstant().toEpochMilli() + OFFSET_MILLIS;
    }
}
