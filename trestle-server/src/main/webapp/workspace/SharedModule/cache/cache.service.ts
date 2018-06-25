import { Inject, Injectable } from "@angular/core";
import transitory, { RemovalCause, TransitoryCache } from "transitory";
import { Observable } from "rxjs/Observable";
import { Subject } from "rxjs/Subject";
import { CACHE_SERVICE_CONFIG, ICacheServiceConfig } from "./cache.service.config";

/**
 * HTTP cache based on:
 * https://hackernoon.com/angular-simple-in-memory-cache-service-on-the-ui-with-rxjs-77f167387e39
 */
@Injectable()
export class CacheService<K, V> {
    private cache: TransitoryCache<K, V>;
    private inFlightObservables: Map<K, Subject<V>> = new Map<K, Subject<V>>();

    /**
     * We only ever call this constructor manually, and provide it the cache specific config settings.
     * If it does get called by the Angular DI, it'll get some default config properties
     *
     * @param {ICacheServiceConfig} config
     */
    public constructor(@Inject(CACHE_SERVICE_CONFIG) private config: ICacheServiceConfig) {
        console.debug("Creating with config:", config);
        this.cache = transitory<K, V>()
            .expireAfterRead(this.config.maxAge)
            .maxSize(this.config.maxSize)
            .withRemovalListener(this.evictionHandler)
            .build();
    }

    /**
     * Attempt to get the individual from the Cache,
     * if that fails, call the fallback value and return the result to both the subscriber and the cache
     * @param {K} key to attempt to fetch from the cache
     * @param {Observable<V>} fallback function to call if key is missing
     * @returns {Observable<V> | Subject<V>} of result
     */
    public get(key: K, fallback?: Observable<V>): Observable<V> | Subject<V> {
        const value = this.cache.get(key);
        if (value) {
            return Observable.of(value);
        }

        if (this.inFlightObservables.has(key)) {
            return (this.inFlightObservables.get(key) as Subject<V>);
        } else if (fallback && fallback instanceof Observable) {
            this.inFlightObservables.set(key, new Subject());
            return fallback.do((oValue) => this.set(key, oValue));
        } else {
            return Observable.throw("Requested key is not available in Cache");
        }
    }

    /**
     * Add key/value pair to cache
     * @param {K} key
     * @param {V} value
     */
    public set(key: K, value: V): void {
        this.cache.set(key, value);
        this.notifyInFlightObservers(key, value);
    }

    private notifyInFlightObservers(key: K, value: V): void {
        if (this.inFlightObservables.has(key)) {
            const inFlight = (this.inFlightObservables.get(key) as Subject<V>);
            const observersCount = inFlight.observers.length;
            if (observersCount) {
                inFlight.next(value);
            }
            inFlight.complete();
            this.inFlightObservables.delete(key);
        }
    }

    private evictionHandler(key: K, value: V, reason: symbol): void {
        switch (reason) {
            case RemovalCause.EXPIRED: {
                console.log("Key %s expire:", key);
                break;
            }
            case RemovalCause.EXPLICIT: {
                console.log("Key %s was removed", key);
                break;
            }
        }
    }
}
