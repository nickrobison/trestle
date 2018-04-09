import { Inject, Injectable, InjectionToken } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { CacheService } from "../cache/cache.service";
import { Observable } from "rxjs/Observable";
import { Response, URLSearchParams } from "@angular/http";
import { TrestleIndividual } from "../individual/TrestleIndividual/trestle-individual";

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

    public getDatasetProperties(dataset: string): Observable<string[]> {
        return this.http.get("/datasets/" + dataset)
            .map((res) => res.json())
            .map(DatasetService.filterPropertyNames);
    }

    public getDatasetFactValues(dataset: string, fact: string, limit = 100): Observable<string[]> {

        const datsetURL = "/datasets/" + dataset + "/" + fact + "/values";

        const params = new URLSearchParams();
        params.append("limit", limit.toString());

        return this.http.get(datsetURL, {
            search: params
        })
            .map((res) => res.json())
            .catch(DatasetService.errorHandler);
    }

    private dsAPICall(): Observable<string[]> {
        return this.http.get("/datasets")
            .do((res) => console.debug("Available datasets:", res.text()))
            .map((res: Response) => res.json())
            .catch(DatasetService.errorHandler);
    }

    public static errorHandler(error: any): Observable<Error> {
        return Observable.throw(error || "Server Error");
    }

    public static filterPropertyNames(properties: string[]): string[] {
        return properties
            .map((property) => {
                return TrestleIndividual.extractSuffix(property)
            });
    }
}
