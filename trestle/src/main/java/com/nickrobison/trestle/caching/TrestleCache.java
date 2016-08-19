package com.nickrobison.trestle.caching;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 8/18/16.
 */
public class TrestleCache {

    private final Cache<IRI, Object> objectCache;
    private final Cache<String, OWLDataPropertyAssertionAxiom> dataPropertyCache;
    private final Cache<String, OWLObjectPropertyAssertionAxiom> objectPropertyCache;

    private TrestleCache(TrestleCacheBuilder builder) {

//        Build the object cache
//        The object cache can't have a loading mechanism, because it's generified, I think.
        objectCache = Caffeine.newBuilder()
        .maximumSize(builder.maxSize.orElse(10000L))
        .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
        .build();



//        Build the data property cache
        dataPropertyCache = Caffeine.newBuilder()
                .maximumSize(builder.maxSize.orElse(10000L))
                .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
                .build();

        objectPropertyCache = Caffeine.newBuilder()
                .maximumSize(builder.maxSize.orElse(10000L))
                .expireAfterWrite(builder.time.orElse(10L), builder.unit.orElse(TimeUnit.HOURS))
                .build();
    }

    public @NonNull Cache<IRI, Object> ObjectCache() {
        return objectCache;
    }

    public Cache<String, OWLDataPropertyAssertionAxiom> DataPropertyCache() {
        return dataPropertyCache;
    }

    public Cache<String, OWLObjectPropertyAssertionAxiom> ObjectPropertyCache() {
        return objectPropertyCache;
    }




    public static class TrestleCacheBuilder {
//        private
        Optional<Long> maxSize = Optional.empty();
        Optional<Long> time = Optional.empty();
        Optional<TimeUnit> unit = Optional.empty();

        public TrestleCacheBuilder maximumSize(long size) {
            maxSize = Optional.of(size);
            return this;
        }

        public TrestleCacheBuilder expirationTime(long time, TimeUnit unit) {
            this.time = Optional.of(time);
            this.unit = Optional.of(unit);
            return this;
        }

        public TrestleCache build() {
            return new TrestleCache(this);
        }
    }
}
