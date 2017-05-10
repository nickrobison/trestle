package com.nickrobison.trestle.reasoner.caching;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Created by nrobison on 2/17/17.
 */
public class TrestleCacheModule extends PrivateModule {
    private final Config cacheConfig;
    private final boolean cacheEnabled;

    public TrestleCacheModule(boolean cacheEnabled) {
        this.cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    protected void configure() {
        if (cacheEnabled) {
            bind(TrestleCache.class).to(TrestleCacheImpl.class);
        } else {
            bind(TrestleCache.class).to(TrestleCacheNoop.class);
        }
        expose(TrestleCache.class);
    }

    @Provides
    @Named("valid")
    @Singleton
    public ITrestleIndex<TrestleIRI> validIndex() {
        try {
            return new TDTree<>(cacheConfig.getInt("blockSize"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to build TD-Tree index");
        }
    }

    @Provides
    @Named("database")
    @Singleton
    public ITrestleIndex<TrestleIRI> databaseIndex() {
        try {
            return new TDTree<>(cacheConfig.getInt("blockSize"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to build TD-Tree index");
        }
    }

    @Provides
    @Singleton
    @Named("cacheLock")
    public TrestleUpgradableReadWriteLock provideIndexLock() {
        return new TrestleUpgradableReadWriteLock();
    }
}
