package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.IRIVersion;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.reasoner.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by nrobison on 7/3/17.
 */
@Disabled
public class TrestleCacheMockTests {

    private static final String PREFIX = "http://nickrobison.com/test#";
    public static final TrestleIRI TEST_IRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2000, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    public static final String CACHE_NAME = "trestle-object-cache";

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
    public void insertTest() {
//        Write the object
        trestleCache.writeTrestleObject(TEST_IRI, LocalDate.of(2017, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC), LocalDate.of(2017, 2, 28).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
//        Write a new object with temporals that fall within existing temporals
        final TrestleIRI testIRI2 = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(1989, 4, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
        trestleCache.writeTrestleObject(TEST_IRI, LocalDate.of(2017, 1, 10).atStartOfDay().toEpochSecond(ZoneOffset.UTC), LocalDate.of(2017, 2, 10).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
//        Should only get called once
//        assertEquals(1, validIndex.getCacheSize(), "Should only have a single value");

        verify(cache, times(1)).put(any(), any());
    }

    @Test
    public void getTest() {
//        Write initial value
        final TestObject testObject = new TestObject("test1", 1);
        trestleCache.writeTrestleObject(TEST_IRI,
                LocalDate.of(2017, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                LocalDate.of(2017, 2, 28).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                testObject);
        verify(cache, times(1)).put(eq(TEST_IRI.getIRI()), eq(testObject));
        reset(cache);

//        Try to get the object back out
        final TrestleIRI newIRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2017, 1, 10).atTime(OffsetTime.MIN), null);
        @Nullable final TestObject trestleObject = trestleCache.getTrestleObject(TestObject.class, newIRI);
        assertAll(() -> verify(cache, times(1)).get(eq(TEST_IRI.getIRI())),
                () -> verify(cache, never()).get(eq(newIRI.getIRI())));
        reset(cache);

//        Try for a non-existent value
        final TrestleIRI nonExistentIRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(2017, 7, 10).atTime(OffsetTime.MIN), null);
        trestleCache.getTrestleObject(TestObject.class, nonExistentIRI);
        assertAll(() -> verify(cache, times(1)).get(eq(nonExistentIRI.getIRI())),
                () -> verify(cache, never()).get(eq(TEST_IRI.getIRI())));

    }


    private static class TestObject implements Serializable {
        private final String objectName;
        private final Integer objectValue;

        TestObject(String objectName, Integer objectValue) {
            this.objectName = objectName;
            this.objectValue = objectValue;
        }

        public String getObjectName() {
            return objectName;
        }

        public Integer getObjectValue() {
            return objectValue;
        }
    }
}
