package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

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
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime startTemporal, OffsetDateTime endTemporal, OffsetDateTime dbStartTemporal, OffsetDateTime dbEndTemporal, Object value) {
//        Not implemented
    }

    @Override
    public void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime atTemporal, OffsetDateTime dbStartTemporal, OffsetDateTime dbEndTemporal, Object value) {
//        Not implemented
    }


//    @Override
//    public void writeTrestleObject(TrestleIRI individualIRI, long startTemporal, long endTemporal, Object value) {
//    }

//    @Override
//    public void writeTrestleObject(TrestleIRI individualIRI, long atTemporal, @NonNull Object value) {
//    }

    @Override
    public void deleteTrestleObject(TrestleIRI trestleIRI) {
//        Not implemented
    }

    @Override
    public @Nullable TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return null;
    }

    @Override
    public void writeTrestleIndividual(OWLNamedIndividual key, TrestleIndividual value) {
//        Not implemented
    }

    @Override
    public void deleteTrestleIndividual(OWLNamedIndividual individual) {
//        Not implemented
    }

    @Override
    public void shutdown(boolean drop) {
//        Not implemented
    }
}
