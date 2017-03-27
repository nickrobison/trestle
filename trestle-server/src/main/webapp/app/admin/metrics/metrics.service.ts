/**
 * Created by nrobison on 3/24/17.
 */
import {Injectable} from "@angular/core";
import {AuthHttp} from "angular2-jwt";
import {Observable} from "rxjs";
import {Response} from "@angular/http";

export interface ITrestleMetricsHeader {
    uptime: number;
    meters: Map<string, string>;
}

@Injectable()
export class MetricsService {

    constructor(private authHttp: AuthHttp) {}

    public getMetrics(): Observable<ITrestleMetricsHeader> {
        return this.authHttp.get("/metrics")
            .map((res: Response) => {
            console.debug("Metrics header:", res.json());
            let test  = res.json;
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}