import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";

@Injectable()
export class AggregationService {

    constructor(private http: TrestleHttp) {

    }

    public performAggregation(dataset: string, strategy: string, wkt: string): Observable<any> {
        const postBody = {
            dataset,
            strategy,
            wkt
        };
        console.debug("Aggregating!", postBody);
        return this.http.post("/aggregate", postBody)
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
