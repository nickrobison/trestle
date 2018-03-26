import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";
import { MapService, wktValue } from "../viewer/map.service";
import { stringify } from "wellknown";

@Injectable()
export class AggregationService {

    constructor(private http: TrestleHttp) {

    }

    public performAggregation(dataset: string, strategy: string, wkt: wktValue): Observable<any> {
        const postBody = {
            dataset,
            strategy,
            wkt: stringify(MapService.normalizeToGeoJSON(wkt))
        };
        console.debug("Aggregating!", postBody);
        return this.http.post("/aggregate", postBody)
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
