package com.nickrobison.trestle.reasoner.caching;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Created by nrobison on 2/17/17.
 */
public class TrestleCacheModule extends PrivateModule {
    private final Logger logger = LoggerFactory.getLogger(TrestleCacheModule.class);
    private final Config cacheConfig;
    private final boolean cacheEnabled;
    private final CacheManager cacheManager;

    public TrestleCacheModule(boolean cacheEnabled) {
        this.cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
        this.cacheEnabled = cacheEnabled;
        final String cacheImplementation = cacheConfig.getString("cacheImplementation");
        logger.info("Creating TrestleCache with implementation {}", cacheImplementation);
//        Do I need to shut this down?
        CachingProvider cachingProvider = Caching.getCachingProvider(cacheImplementation);
        cacheManager = cachingProvider.getCacheManager();
    }

    @Override
    protected void configure() {
        if (cacheEnabled) {
            bind(TrestleCache.class)
                    .to(TrestleCacheImpl.class)
                    .in(Singleton.class);
        } else {
            bind(TrestleCache.class)
                    .to(TrestleCacheNoop.class)
                    .in(Singleton.class);
        }
        expose(TrestleCache.class);

//        Register the geometry cache provider
//        This cannot currently be disabled, it's always watching, always
        final TypeLiteral<Cache<Integer, Geometry>> typeLiteral = new TypeLiteral<Cache<Integer, Geometry>>() {
        };
        bind(typeLiteral)
                .toProvider(GeometryCacheProvider.class)
                .in(Singleton.class);
        expose(typeLiteral);
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

    @Provides
    public CacheManager provideCacheManager() {
        return this.cacheManager;
    }


}
