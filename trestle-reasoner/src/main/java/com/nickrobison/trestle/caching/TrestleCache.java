package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.caching.tdtree.TDTree;
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

/**
 * Created by nrobison on 8/18/16.
 */
@Singleton
public class TrestleCache {

    private final CacheManager cacheManager;
    private final Cache<String, Object> individualCache;
    private final ITrestleIndex<String> validIndex;
    private final ITrestleIndex<String> dbIndex;

    @Inject
    private TrestleCache(ITrestleIndex<String> validIndex, ITrestleIndex<String> dbIndex) {
//        Create the index
        this.validIndex = validIndex;
        this.dbIndex = dbIndex;

        final Config cacheConfig = ConfigFactory.load().getConfig("trestle.cache");
        final CachingProvider cachingProvider = Caching.getCachingProvider(cacheConfig.getString("cacheImplementation"));
        cacheManager = cachingProvider.getCacheManager();
        final MutableConfiguration<String, Object> individualCacheConfiguration = new MutableConfiguration<>();
        individualCacheConfiguration
                .setTypes(String.class, Object.class)
                .setStatisticsEnabled(true);
        individualCache = cacheManager.createCache("individual-cache", individualCacheConfiguration);
    }


    public @Nullable Object getIndividual(String individualID, long validTime, long dbTime) {
//        Valid first, then db
        @Nullable final String indexValue = validIndex.getValue(individualID, validTime);
        if (indexValue != null) {
            return individualCache.get(indexValue);
        } else {
            return individualCache.get(individualID);
        }
    }
//
//    public void putIndividual(String individualID, long validFrom, long validTo, long dbFrom, long dbTo, Object value) {
//        this.validIndex.in
//        this.validIndex.insertValue(individualID, validFrom, validTo, value);
//    }




////        Build the object cache
////        The object cache can't have a loading mechanism, because it's generified, I think.
//        objectCache = Caffeine.newBuilder()
//        .maximumSize(builder.maxSize.orElse(10000L))
//        .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
//        .build();
//
//
//
////        Build the data property cache
//        dataPropertyCache = Caffeine.newBuilder()
//                .maximumSize(builder.maxSize.orElse(10000L))
//                .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
//                .build();
//
//        objectPropertyCache = Caffeine.newBuilder()
//                .maximumSize(builder.maxSize.orElse(10000L))
//                .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
//                .build();
//
//        individualCache = Caffeine.newBuilder()
//                .maximumSize(builder.maxSize.orElse(10000L))
//                .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
//                .build();
//    }
//
//    public @NonNull Cache<IRI, Object> ObjectCache() {
//        return objectCache;
//    }
//
//    public @NonNull Cache<String, TrestleIndividual> IndividualCache() { return individualCache; }
//
////    TODO(nrobison): To implement
//    public Cache<String, OWLDataPropertyAssertionAxiom> DataPropertyCache() {
//        return dataPropertyCache;
//    }
//
////    TODO(nrobison): To implement
//    public Cache<String, OWLObjectPropertyAssertionAxiom> ObjectPropertyCache() {
//        return objectPropertyCache;
//    }


//
//    public static class TrestleCacheBuilder {
////        private
//        Optional<Long> maxSize = Optional.empty();
//        Optional<Long> time = Optional.empty();
//        Optional<TimeUnit> unit = Optional.empty();
//
//        public TrestleCacheBuilder maximumSize(long size) {
//            maxSize = Optional.of(size);
//            return this;
//        }
//
//        public TrestleCacheBuilder expirationTime(long time, TimeUnit unit) {
//            this.time = Optional.of(time);
//            this.unit = Optional.of(unit);
//            return this;
//        }
//
//        public TrestleCache build() {
//            return new TrestleCache(this);
//        }
//    }
}
