import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";
import { GeometryObject } from "geojson";

export interface IAggregationRestriction<T> {
    fact: string;
    value: T;
}

@Injectable()
export class AggregationService {

    constructor(private http: TrestleHttp) {

    }

    public performAggregation<T>(dataset: string, strategy: string, restriction: IAggregationRestriction<T>): Observable<GeometryObject> {
        const postBody = {
            dataset,
            strategy,
            restriction
        };
        console.debug("Aggregating!", postBody);
        return this.http.post("/aggregate", postBody)
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
