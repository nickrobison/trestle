package com.nickrobison.trestle.reasoner.caching.listeners;

import com.nickrobison.trestle.reasoner.caching.ITrestleIndex;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.TrestleIRI;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Created by nrobison on 2/22/17.
 */
@SuppressWarnings({"override.param.invalid"})
public class TrestleObjectCacheEntryListener implements
        CacheEntryExpiredListener<IRI, Object>,
        CacheEntryRemovedListener<IRI, Object>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TrestleObjectCacheEntryListener.class);
    private final ITrestleIndex<TrestleIRI> validIndex;
    private final TrestleUpgradableReadWriteLock cacheLock;

    @Inject
    TrestleObjectCacheEntryListener(@Named("valid") ITrestleIndex<TrestleIRI> validIndex, @Named("cacheLock") TrestleUpgradableReadWriteLock cacheLock) {
        this.validIndex = validIndex;
        this.cacheLock = cacheLock;
        logger.debug("Registering cache listener, waiting for expired or removed events");
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends IRI, ?>> cacheEntryEvents) throws CacheEntryListenerException {
        removeFromIndex(cacheEntryEvents);
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends IRI, ?>> cacheEntryEvents) throws CacheEntryListenerException {
        removeFromIndex(cacheEntryEvents);
    }

    private void removeFromIndex(Iterable<CacheEntryEvent<? extends IRI, ?>> cacheEntryEvents) {
//            Remove from index
        try {
            logger.debug("Evicting entries from cache");
            cacheLock.lockWrite();
            cacheEntryEvents.forEach(event -> {
                final TrestleIRI parsedTrestleIRI = IRIBuilder.parseIRIToTrestleIRI(event.getKey());
                logger.debug("{} is being evicted from the cache", parsedTrestleIRI);
                validIndex.deleteKeysWithValue(parsedTrestleIRI);
            });
        } catch (InterruptedException e) {
            logger.debug("Error taking lock", e);
        } finally {
            cacheLock.unlockWrite();
        }
    }
}
