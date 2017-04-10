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
    nowTime: Moment;
    upTime: Duration;
    selectedValue: string;
    oldValue = "";
    selectedData: IMetricsData;
    loadingData: boolean;

    constructor(private ms: MetricsService) {
    }

    ngOnInit(): void {
        this.ms.getMetrics()
            .subscribe(metricsResponse => {
                this.meters = [];
                Object.keys(metricsResponse.meters).forEach(key => {
                    this.meters.push(key);
                });
                console.debug("Uptime", metricsResponse.upTime);
                console.debug("Startime", metricsResponse.startTime);
                let upDuration = duration(metricsResponse.upTime);
                console.debug("Duration", upDuration);
                this.upTime = upDuration;
                this.startTime = moment(metricsResponse.startTime);
                this.nowTime = moment();
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
        this.loadingData = true;
        this.ms.getMetricValues(metric, this.startTime.valueOf(), this.nowTime.valueOf())
            .finally(() => this.loadingData = false)
            .subscribe(metricValues => {
                console.debug("Have metric values:", metricValues);
                this.selectedData = metricValues;
            })
    }

    exportAllMetrics = (): void => {
        console.debug("Exporting all metrics");
    }
}