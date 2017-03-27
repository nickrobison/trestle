/**
 * Created by nrobison on 3/24/17.
 */
import {Component, OnInit} from "@angular/core";
import {ITrestleMetricsHeader, MetricsService} from "./metrics.service";
import {Moment, unix, duration, Duration} from "moment";

@Component({
    selector: "metrics-root",
    templateUrl: "./metrics.component.html",
    styleUrls: ["./metrics.component.css"]
})

export class MetricsComponent implements OnInit {
    meters: Array<string> = [];
    startTime: Duration;
    selectedValue: string;

    constructor(private ms: MetricsService) {}

    ngOnInit(): void {
        this.ms.getMetrics()
            .subscribe(metricsResponse => {
                this.meters = [];
                Object.keys(metricsResponse.meters).forEach(key => {
                    this.meters.push(key);
                });
                this.startTime = duration(metricsResponse.uptime);
            }, (error: Error) => {
                console.error(error);
            })
    }
}