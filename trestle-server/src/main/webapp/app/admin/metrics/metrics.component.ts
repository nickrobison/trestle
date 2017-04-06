/**
 * Created by nrobison on 3/24/17.
 */
import {Component, DoCheck, OnInit} from "@angular/core";
import {IMetricsData, MetricsService} from "./metrics.service";
import {Moment, utc, duration, Duration} from "moment";
import moment = require("moment");

@Component({
    selector: "metrics-root",
    templateUrl: "./metrics.component.html",
    styleUrls: ["./metrics.component.css"]
})

export class MetricsComponent implements OnInit, DoCheck {
    meters: Array<string> = [];
    startTime: Moment;
    upTime: Duration;
    selectedValue: string;
    oldValue = "";
    selectedData: IMetricsData;

    constructor(private ms: MetricsService) {}

    ngOnInit(): void {
        this.ms.getMetrics()
            .subscribe(metricsResponse => {
                this.meters = [];
                Object.keys(metricsResponse.meters).forEach(key => {
                    this.meters.push(key);
                });
                console.debug("Uptime", metricsResponse.upTime);
                let test = duration(metricsResponse.upTime);
                console.debug("Duration", test);
                this.upTime = test;
                this.startTime = utc().subtract(this.upTime);
            }, (error: Error) => {
                console.error(error);
            })
    }

    ngDoCheck() {
        if (this.selectedValue !== this.oldValue) {
            console.debug("Changed to:", this.selectedValue);
            this.oldValue = this.selectedValue;
            if (this.selectedValue != null) {
                this.addData(this.selectedValue);
            }
        }
    }

    addData(metric: string): void {
        console.debug("Adding data:", metric);
        this.ms.getMetricValues(metric, this.startTime.unix())
            .subscribe(metricValues => {
                console.debug("Have metric values:", metricValues);
                this.selectedData = metricValues;
            })
    }
}