package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
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
    public @Nullable TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public void writeTrestleIndividual(OWLNamedIndividual key, TrestleIndividual value) {

    }

    @Override
    public void deleteTrestleIndividual(OWLNamedIndividual individual) {

    }

    @Override
    public void shutdown(boolean drop) {

    }
}
