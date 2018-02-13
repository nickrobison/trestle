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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.semanticweb.owlapi.model.IRI;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.time.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by nrobison on 7/3/17.
 */
public class TrestleCacheMockTests {
    private static final String PREFIX = "http://nickrobison.com/test#";
    private static final TrestleIRI TEST_IRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2000, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
    private static final TrestleIRI TEST_IRI_2 = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2000, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
    private static final TrestleIRI TEST2_IRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object-2", null, LocalDate.of(2017, 1, 10).atTime(OffsetTime.MIN), LocalDate.of(2017, 5, 14).atTime(OffsetTime.MIN));
    private static final TrestleIRI TEST2_IRI_2 = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object-2", null, LocalDate.of(2017, 1, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 7, 1).atTime(OffsetTime.MIN));
    private static final String CACHE_NAME = "trestle-object-cache";
    public static final OffsetDateTime JANUARY_TEST_DATE = LocalDate.of(2017, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final OffsetDateTime FEB_TEST_DATE = LocalDate.of(2017, 2, 28).atStartOfDay().atOffset(ZoneOffset.UTC);
    public static final OffsetDateTime MAY_TEST_DATE = LocalDate.of(2017, 5, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
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
        when(manager.getCache(eq(CACHE_NAME), any(), eq(Object.class))).thenReturn(cache);
        trestleCache = new TrestleCacheImpl(validIndex, dbIndex, new TrestleUpgradableReadWriteLock(), listener, metrician, manager);
        verify(manager, times(1)).getCache(eq(CACHE_NAME), eq(IRI.class), eq(Object.class));
//        Reset everything, in case the cache object gets instantiated in a running VM. Like in the test suite
        reset(cache);
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

//        Try without database temporals
        reset(cache);
        final CacheTestObject testObject2 = new CacheTestObject("test1", 2);
        trestleCache.writeTrestleObject(TEST2_IRI, JANUARY_TEST_DATE, FEB_TEST_DATE, MAY_TEST_DATE, JUNE_TEST_DATE, testObject);
        trestleCache.writeTrestleObject(TEST2_IRI_2, JANUARY_TEST_DATE, FEB_TEST_DATE, JUNE_TEST_DATE, null, testObject2);
        final TrestleIRI firstVersion = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object-2", null, LocalDate.of(2017, 1, 20).atTime(OffsetTime.MIN), LocalDate.of(2017, 5, 14).atTime(OffsetTime.MIN));
        final TrestleIRI secondVersion = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object-2", null, LocalDate.of(2017, 1, 20).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 14).atTime(OffsetTime.MIN));
        trestleCache.getTrestleObject(CacheTestObject.class, firstVersion);
        trestleCache.getTrestleObject(CacheTestObject.class, secondVersion);
//        verify(cache, never()).get(eq(TEST2_IRI_2.getIRI()));
//        verify(cache, times(1)).get(eq(TEST2_IRI.getIRI()));
        assertAll(() -> verify(cache, times(1)).get(eq(TEST2_IRI.getIRI())),
                () -> verify(cache, times(1)).get(TEST2_IRI_2.getIRI()));


    }

    @AfterEach
    public void tearDown() {
        trestleCache.shutdown(true);
    }


}
