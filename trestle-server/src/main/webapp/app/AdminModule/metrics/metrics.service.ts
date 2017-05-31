/**
 * Created by nrobison on 3/24/17.
 */
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {Response, ResponseContentType, URLSearchParams} from "@angular/http";
import {TrestleHttp} from "../../UserModule/trestle-http.provider";

export interface ITrestleMetricsHeader {
    upTime: number;
    startTime: number;
    meters: Map<string, string>;
}

export interface IMetricsData {
    metric: string;
    values: Array<IMetricsValue>;
}

export interface IMetricsValue {
    timestamp: Date;
    value: number;
}

@Injectable()
export class MetricsService {

    constructor(private authHttp: TrestleHttp) {
    }

    public getMetrics(): Observable<ITrestleMetricsHeader> {
        return this.authHttp.get("/metrics")
            .map((res: Response) => {
                console.debug("Metrics header:", res.json());
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public getMetricValues(metricID: string, start: number, end: number): Observable<IMetricsData> {
        console.debug("Retrieving values for metric: " + metricID + " from: " + start + " to: " + end);
        let params = new URLSearchParams();
        params.append("start", start.toString());
        params.append("end", end.toString());
        return this.authHttp.get("/metrics/metric/" + metricID, {
            search: params
        })
            .map((res: Response) => {
                let json = res.json();
                console.debug("Metric values:", json);
                let metricValues: Array<IMetricsValue> = [];
                Object.keys(json).forEach((key) => {
                    let longKey = parseInt(key, 10);
                    if (longKey !== 0) {
                        metricValues.push({
                            timestamp: new Date(longKey),
                            value: json[longKey]
                        });
                    }
                });
                return {
                    metric: metricID,
                    values: metricValues.sort((a, b) => {
                        if (a.timestamp == b.timestamp) {
                            return 0;
                        }
                        if (a.timestamp < b.timestamp) {
                            return -1;
                        }
                        if (a.timestamp > b.timestamp) {
                            return 1;
                        }
                        return null;
                    })
                }
            })
            .catch((error: Error) => Observable.throw(error || "Sever Error"));
    }

    public exportMetricValues(metrics: null | Array<string>, start: number, end?: number): Observable<Blob> {
        return this.authHttp.post("/metrics/export", {
                metrics: metrics,
                start: start,
                end: end
            },
            {
                responseType: ResponseContentType.Blob
            })
            .map((res: Response) => {
                return res.blob();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}