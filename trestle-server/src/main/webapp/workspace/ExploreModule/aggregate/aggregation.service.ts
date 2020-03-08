import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";
import { GeometryObject } from "geojson";

export const BBOX_PROPERTY = "BOUNDING_BOX";
export type AggregationOperation = "EQ" | "NEQ" | "GT" | "GTEQ" | "LT" | "LTEQ";

export interface IAggregationRestriction {
    dataset: string;
    property: string;
    value: object;
}

export interface IAggregationStrategy {
    field: string;
    operation: AggregationOperation;
    value: object;
}

export interface IAggregationRequest {
    restriction: IAggregationRestriction;
    strategy: IAggregationStrategy;
}

@Injectable()
export class AggregationService {

    constructor(private http: TrestleHttp) {

    }

    public performAggregation<T>(request: IAggregationRequest): Observable<GeometryObject> {
        console.debug("Aggregating!", request);
        return this.http.post("/aggregate", request)
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
