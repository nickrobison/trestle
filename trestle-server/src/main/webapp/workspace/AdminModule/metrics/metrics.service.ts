/**
 * Created by nrobison on 3/24/17.
 */
import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";
import { ResponseContentType } from "@angular/http";

export interface ITrestleMetricsHeader {
    upTime: number;
    startTime: number;
    meters: Map<string, string>;
}

export interface IMetricsData {
    metric: string;
    values: IMetricsValue[];
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
            .map((res) => {
                console.debug("Metrics header:", res.json());
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public getMetricValues(metricID: string, start: number, end: number): Observable<IMetricsData> {
        console.debug("Retrieving values for metric: " + metricID + " from: " + start + " to: " + end);
        const params = new URLSearchParams();
        params.append("start", start.toString());
        params.append("end", end.toString());
        return this.authHttp.get("/metrics/metric/" + metricID, {
            search: params
        })
            .map((res) => {
                const json = res.json();
                console.debug("Metric values:", json);
                const metricValues: IMetricsValue[] = [];
                Object.keys(json).forEach((key) => {
                    const longKey = parseInt(key, 10);
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
                        if (a.timestamp === b.timestamp) {
                            return 0;
                        }
                        if (a.timestamp < b.timestamp) {
                            return -1;
                        }
                        return 1;
                    })
                };
            })
            .catch((error: Error) => Observable.throw(error || "Sever Error"));
    }

    public exportMetricValues(metrics: null | string[], start: number, end?: number): Observable<Blob> {
        return this.authHttp.post("/metrics/export", {
                metrics,
                start,
                end
            },
            {
                responseType: ResponseContentType.Blob
            })
            .map((res) => {
                return res.blob();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
