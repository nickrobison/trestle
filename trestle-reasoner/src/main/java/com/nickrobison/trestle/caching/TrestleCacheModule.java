package com.nickrobison.trestle.caching;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.caching.tdtree.TDTree;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Created by nrobison on 2/17/17.
 */
public class TrestleCacheModule extends AbstractModule {
    private Config cacheConfig;

    @Override
    protected void configure() {
        cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
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
