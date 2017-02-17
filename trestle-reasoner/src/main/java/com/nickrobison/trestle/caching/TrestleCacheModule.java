package com.nickrobison.trestle.caching;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.caching.tdtree.TDTree;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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
    public ITrestleIndex<String> provideIndex() {
        try {
            return new TDTree<>(cacheConfig.getInt("blockSize"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to build TD-Tree index");
        }
    }
}
