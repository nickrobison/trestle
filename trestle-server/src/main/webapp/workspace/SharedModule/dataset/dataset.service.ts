import { Inject, Injectable, InjectionToken } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { CacheService } from "../cache/cache.service";
import { Observable } from "rxjs/Observable";
import { Response, URLSearchParams } from "@angular/http";

export const DATASET_CACHE = new InjectionToken<CacheService<string, string[]>>("dataset.cache");

@Injectable()
export class DatasetService {

    constructor(private http: TrestleHttp,
                @Inject(DATASET_CACHE) private cache: CacheService<string, string[]>) {

    }

    /**
     * Returns the list of currently registered datasets from the database
     * @returns {Observable<string[]>}
     */
    public getAvailableDatasets(): Observable<string[]> {
        // Try from cache first, then hit the API
        return this.cache.get("datasets", this.dsAPICall());
    }

    public getDatasetFactValues(dataset: string, fact: string, limit = 100): Observable<string[]> {

        const datsetURL = "/datasets/" + dataset + "/" + fact + "/values";

        const params =new URLSearchParams();
        params.append("limit", limit.toString());

        return this.http.get(datsetURL, {
            search: params
        })
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    private dsAPICall(): Observable<string[]> {
        return this.http.get("/datasets")
            .do((res) => console.debug("Available datasets:", res.text()))
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

}