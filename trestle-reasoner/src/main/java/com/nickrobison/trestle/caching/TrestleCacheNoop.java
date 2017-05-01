package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.iri.TrestleIRI;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 5/1/17.
 */
public class TrestleCacheNoop implements TrestleCache {
    private static final Logger logger = LoggerFactory.getLogger(TrestleCacheNoop.class);

    TrestleCacheNoop() {
        logger.warn("Caching disabled, instantiating No-Op cache");
    }

    @Override
    public <T> @Nullable T getTrestleObject(Class<T> clazz, TrestleIRI individualIRI) {
        return null;
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, long startTemporal, long endTemporal, Object value) {
    }

    @Override
    public void deleteTrestleObject(TrestleIRI trestleIRI) {

    }

    @Override
    public void shutdown(boolean drop) {

    }
}
