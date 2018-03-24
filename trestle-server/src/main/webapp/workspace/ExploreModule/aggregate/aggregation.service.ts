import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";


@Injectable()
export class AggregationService {

    public constructor(private http: TrestleHttp) {

    }

    public performAggregation(dataset: string, strategy: string, wkt: string): Observable<any> {
        return this.http.post("/aggregate", {
            dataset: dataset,
            strategy: strategy,
            wkt: wkt
        })
            .map((res) => res.json());
    }
}
