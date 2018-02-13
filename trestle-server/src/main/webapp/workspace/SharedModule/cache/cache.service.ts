import {Injectable} from "@angular/core";
import {Subject} from "rxjs/Subject";
import {Observable} from "rxjs/Observable";

interface CacheContent<C> {
    expiry: number;
    value: C;
}

/**
 * HTTP cache based on:
 * https://hackernoon.com/angular-simple-in-memory-cache-service-on-the-ui-with-rxjs-77f167387e39
 */
@Injectable()
export class CacheService<K, V> {
    private cache: Map<K, CacheContent<V>> = new Map<K, CacheContent<V>>();
    private inFlightObservables: Map<K, Subject<V>> = new Map<K, Subject<V>>();
    private readonly DEFAULT_MAX_AGE = 30000;

    public constructor() {
        // if (maxAge) {
        //     this.DEFAULT_MAX_AGE = maxAge;
        // } else {
        //     this.DEFAULT_MAX_AGE = 30000;
        // }
    }

    public get(key: K, fallback?: Observable<V>, maxAge?: number): Observable<V> | Subject<V> {
        if (this.hasValidCachedKey(key)) {
            return Observable.of((this.cache.get(key) as CacheContent<V>).value);
        }

        if (!maxAge) {
            maxAge = this.DEFAULT_MAX_AGE;
        }

        if (this.inFlightObservables.has(key)) {
            return (this.inFlightObservables.get(key) as Subject<V>);
        } else if (fallback && fallback instanceof Observable) {
            this.inFlightObservables.set(key, new Subject());
            return fallback.do((value) => this.set(key, value, maxAge));
        } else {
            return Observable.throw("Requested key is not available in Cache");
        }
    }

    public set(key: K, value: V, maxAge: number = this.DEFAULT_MAX_AGE): void {
        this.cache.set(key, {value, expiry: Date.now() + maxAge});
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

    /**
     * Does the cache has a valid key of type K?
     * If so, is the key passed the expiry date?
     * If so, delete it
     * @param {K} key - Key to check for
     * @returns {boolean} - Does the key exist and is it valid?
     */
    private hasValidCachedKey(key: K): boolean {
        if (this.cache.has(key)) {
            if ((this.cache.get(key) as CacheContent<V>).expiry < Date.now()) {
                this.cache.delete(key);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}