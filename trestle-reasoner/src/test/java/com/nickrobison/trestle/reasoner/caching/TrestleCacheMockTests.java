package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.locking.TrestleUpgradableReadWriteLock;
import com.nickrobison.trestle.iri.IRIBuilder;
import com.nickrobison.trestle.iri.IRIVersion;
import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.reasoner.caching.listeners.TrestleObjectCacheEntryListener;
import com.nickrobison.trestle.reasoner.caching.tdtree.TDTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by nrobison on 7/3/17.
 */
@Disabled
public class TrestleCacheMockTests {

    private static final String PREFIX = "http://nickrobison.com/test#";
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
        final TrestleIRI testIRI = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(1989, 3, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
//        Write the object
        trestleCache.writeTrestleObject(testIRI, LocalDate.of(2017, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC), LocalDate.of(2017,2,28).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
//        Write a new object with temporals within existing temporals
        final TrestleIRI testIRI2 = IRIBuilder.encodeIRI(IRIVersion.V1, PREFIX, "test-object", null, LocalDate.of(1989, 4, 26).atTime(OffsetTime.MIN), LocalDate.of(2017, 6, 1).atTime(OffsetTime.MIN));
        trestleCache.writeTrestleObject(testIRI, LocalDate.of(2017, 1, 10).atStartOfDay().toEpochSecond(ZoneOffset.UTC), LocalDate.of(2017,2,10).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
//        Should only get called once
//        assertEquals(1, validIndex.getCacheSize(), "Should only have a single value");

        verify(cache, times(1)).put(any(), any());
    }

    public void getTest() {

    }
}
