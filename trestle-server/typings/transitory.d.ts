declare module "transitory" {

    export const RemovalCause: RemovalCause;

    export interface RemovalCause {
        EXPLICIT: symbol;
        EXPIRED: symbol;
    }

    export type CacheLoader<K, V> = (key: K) => V | Promise<V>;

    export interface ITransitoryBuilder<K, V> {
        maxSize(size: number | string): ITransitoryBuilder<K, V>;

        expireAfterWrite(time: number | string): ITransitoryBuilder<K, V>;

        expireAfterRead(time: number | string): ITransitoryBuilder<K, V>;

        withWeigher(weigher: (key: K, value: V) => number): ITransitoryBuilder<K, V>;

        withRemovalListener(listener: (key: K, value: V, reason: symbol) => void): ITransitoryBuilder<K, V>;

        metrics(): ITransitoryBuilder<K, V>;

        withLoader(loader: CacheLoader<K, V>): ILoadingTransitoryBuilder<K, V>;

        loading(): ILoadingTransitoryBuilder<K, V>;

        build(): TransitoryCache<K, V>;
    }

    export interface ILoadingTransitoryBuilder<K, V> {
        done(): TransitoryLoadingCache<K, V>;
    }

    export interface ICacheMetrics {
        hitRate: number;
        hits: number;
        misses: number;
    }

    interface ITransitoryBaseCache<K, V> {
        maxSize: number;
        weightedSize: number;
        metrics: ICacheMetrics | null;

        set(key: K, value: V): V | null;

        getIfPresent(key: K, recordStats: boolean): V | null;

        peek(key: K): V | null;

        has(key: K): boolean;

        delete(key: K): V | null;

        clear(): void;

        cleanUp(): void;

        keys(): K[];
    }

    export interface TransitoryCache<K, V> extends ITransitoryBaseCache<K, V> {
        get(key: K): V | null;
    }

    export interface TransitoryLoadingCache<K, V> extends ITransitoryBaseCache<K, V> {
        get(key: K, loader?: CacheLoader<K, V>): Promise<V>;
    }

    function memoryUsageWeigher(key: any, value: any): number;

    export function transitory<K, V>(): ITransitoryBuilder<K, V>;

    export default transitory;
}
