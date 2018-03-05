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

    public constructor(@Inject(CACHE_SERVICE_CONFIG) private config: ICacheServiceConfig) {
        this.cache = transitory<K, V>()
            .expireAfterRead(this.config.maxAge)
            .maxSize(this.config.maxSize)
            .withRemovalListener(this.evictionHandler)
            .build();
    }

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
