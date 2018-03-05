import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";

export interface IIndexLeafStatistics {
    binaryID: string;
    type: string;
    coordinates: number[];
    leafID: number;
    direction: number;
    records: number;
}

export interface ICacheStatistics {
    offsetValue: number;
    maxValue: number;
    dbIndexFragmentation: number;
    dbIndexSize: number;
    dbLeafStats: IIndexLeafStatistics[];
    validIndexFragmentation: number;
    validIndexSize: number;
    validLeafStats: IIndexLeafStatistics[];
}

@Injectable()
export class IndexService {

    public constructor(private http: TrestleHttp) {

    }

    /**
     * Get statistics for both caches
     * @returns {Observable<ICacheStatistics>}
     */
    public getIndexStatistics(): Observable<ICacheStatistics> {
        return this.http
            .get("/cache/index")
            .map((res) => {
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Rebuild specified index
     * @param {string} index to rebuild
     * @returns {Observable<void>}
     */
    public rebuildIndex(index: string): Observable<void> {
        return this.http
            .get("/cache/rebuild/" + index.toLocaleLowerCase())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Purge the specified index
     * @param {string} cache to purge
     * @returns {Observable<void>}
     */
    public purgeCache(cache: string): Observable<void> {
        return this.http
            .get("/cache/purge/" + cache.toLocaleLowerCase())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
