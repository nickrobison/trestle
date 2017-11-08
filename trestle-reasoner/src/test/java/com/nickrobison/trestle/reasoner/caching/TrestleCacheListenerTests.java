package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.IRIVersion;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.reasoner.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.cache.CacheManager;
import javax.cache.Caching;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by nrobison on 7/3/17.
 */
@Disabled
public class TrestleCacheListenerTests {
    private static final String PREFIX = "http://nickrobison.com/test#";
    private static final TrestleIRI TEST_IRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2000, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
    private static final OffsetDateTime JAN_TEST_DATE = LocalDate.of(2017, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime FEB_TEST_DATE = LocalDate.of(2017, 2, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime MAR_TEST_DATE = LocalDate.of(2017, 3, 26).atStartOfDay().atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime JUN_TEST_DATE = LocalDate.of(2017, 6, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime JUL_TEST_DATE = LocalDate.of(2017, 7, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

    @Mock
    TrestleObjectCacheEntryListener listener;
    @Mock
    Metrician metrician;
    ITrestleIndex<TrestleIRI> validIndex;
    ITrestleIndex<TrestleIRI> dbIndex;
    TrestleCacheImpl trestleCache;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.validIndex = new TDTree<>(10);
        this.dbIndex = new TDTree<>(10);
        final CacheManager manager = Caching.getCachingProvider("com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider").getCacheManager();
        final TrestleObjectCacheEntryListener listener1 = new TrestleObjectCacheEntryListener(validIndex, new TrestleUpgradableReadWriteLock());
        trestleCache = new TrestleCacheImpl(validIndex, dbIndex, new TrestleUpgradableReadWriteLock(), listener1, metrician, manager);
    }

    @Test
    public void testEviction() {
        final CacheTestObject cacheTestObject = new CacheTestObject("cache test", 1);
        trestleCache.writeTrestleObject(TEST_IRI, JAN_TEST_DATE, JUN_TEST_DATE, JUL_TEST_DATE, cacheTestObject);
        trestleCache.writeTrestleObject(TEST_IRI, FEB_TEST_DATE, JUN_TEST_DATE, JUL_TEST_DATE, cacheTestObject);
        trestleCache.writeTrestleObject(TEST_IRI, MAR_TEST_DATE, JUN_TEST_DATE, JUL_TEST_DATE, cacheTestObject);
        trestleCache.getTrestleObject(CacheTestObject.class, TEST_IRI);
        trestleCache.getTrestleObject(CacheTestObject.class, TEST_IRI);
        trestleCache.getTrestleObject(CacheTestObject.class, TEST_IRI);
        trestleCache.getTrestleObject(CacheTestObject.class, TEST_IRI);
        verify(listener, times(1)).onRemoved(any());
        verify(listener, never()).onExpired(any());
    }

    @Test
    public void testRemoval() {
        final CacheTestObject cacheTestObject = new CacheTestObject("cache test", 1);
        trestleCache.writeTrestleObject(TEST_IRI,
                LocalDate.of(2000, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC),
                LocalDate.of(2000, 5, 1).atStartOfDay().atOffset(ZoneOffset.UTC),
                JUN_TEST_DATE,
                JUL_TEST_DATE,
                cacheTestObject);
        trestleCache.deleteTrestleObject(TEST_IRI);
        verify(listener, times(1)).onRemoved(any());
    }

    @AfterEach
    public void tearDown() {
        trestleCache.shutdown(true);
        reset(listener);
    }
}
