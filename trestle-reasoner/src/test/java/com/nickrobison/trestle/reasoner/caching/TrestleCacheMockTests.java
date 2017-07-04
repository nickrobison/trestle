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

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by nrobison on 7/3/17.
 */
public class TrestleCacheMockTests {
    private static final String PREFIX = "http://nickrobison.com/test#";
    private static final TrestleIRI TEST_IRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2000, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
    private static final String CACHE_NAME = "trestle-object-cache";
    public static final OffsetDateTime JANUARY_TEST_DATE = LocalDate.of(2017, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final OffsetDateTime FEB_TEST_DATE = LocalDate.of(2017, 2, 28).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final OffsetDateTime JUNE_TEST_DATE = LocalDate.of(2017, 6, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final OffsetDateTime JULY_TEST_DATE = LocalDate.of(2017, 7, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

    @Mock
    CacheManager manager;
    @Mock
    Cache<Object, Object> cache;
    @Mock
    TrestleObjectCacheEntryListener listener;
    @Mock
    Metrician metrician;
    ITrestleIndex<TrestleIRI> validIndex;
    ITrestleIndex<TrestleIRI> dbIndex;
    TrestleCacheImpl trestleCache;

    @BeforeEach
    public void setup() throws Exception {
        this.validIndex = new TDTree<>(10);
        this.dbIndex = new TDTree<>(10);
        MockitoAnnotations.initMocks(this);
        when(manager.createCache(eq(CACHE_NAME), any())).thenReturn(cache);
        trestleCache = new TrestleCacheImpl(validIndex, dbIndex, new TrestleUpgradableReadWriteLock(), listener, metrician, manager);
        verify(manager, times(1)).createCache(eq(CACHE_NAME), any());
    }

    @Test
    public void getTest() {
//        Write initial value
        final CacheTestObject testObject = new CacheTestObject("test1", 1);
        trestleCache.writeTrestleObject(TEST_IRI,
                JANUARY_TEST_DATE,
                FEB_TEST_DATE,
                JUNE_TEST_DATE,
                JULY_TEST_DATE,
                testObject);
        verify(cache, times(1)).put(eq(TEST_IRI.getIRI()), eq(testObject));

//        Try to get the object back out, with a database temporal
        final TrestleIRI newIRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2017, 1, 10).atTime(OffsetTime.MIN), JUNE_TEST_DATE.plusDays(10));
        trestleCache.getTrestleObject(CacheTestObject.class, newIRI);
        assertAll(() -> verify(cache, times(1)).get(eq(TEST_IRI.getIRI())),
                () -> verify(cache, never()).get(eq(newIRI.getIRI())));

//        Try for a non-existent value, with database temporal
        final TrestleIRI nonExistentIRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2017, 7, 10).atTime(OffsetTime.MIN), JUNE_TEST_DATE.plusDays(10));
        trestleCache.getTrestleObject(CacheTestObject.class, nonExistentIRI);
        assertAll(() -> verify(cache, times(1)).get(eq(nonExistentIRI.getIRI())),
                () -> verify(cache, times(1)).get(eq(TEST_IRI.getIRI())));
    }

    @AfterEach
    public void tearDown() {
        trestleCache.shutdown(true);
    }


}
