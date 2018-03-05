/**
 * Created by nrobison on 3/24/17.
 */
import {Component, DoCheck, OnInit, ViewChild} from "@angular/core";
import {IMetricsData, MetricsService} from "./metrics.service";
import * as moment from "moment";
import {saveAs} from "file-saver";
import {MetricsGraph} from "./metrics-graph.component";

@Component({
    selector: "metrics-root",
    templateUrl: "./metrics.component.html",
    styleUrls: ["./metrics.component.css"]
})

export class MetricsComponent implements OnInit, DoCheck {
    public meters: string[] = [];
    public startTime: moment.Moment;
    public nowTime: moment.Moment;
    public upTime: moment.Duration;
    public selectedValue: string;
    public oldValue = "";
    public selectedData: IMetricsData;
    public loadingData: boolean;
    public disabled: boolean;
    @ViewChild(MetricsGraph) private graph: MetricsGraph;

    constructor(private ms: MetricsService) {
        this.disabled = false;
    }

    public ngOnInit(): void {
        this.ms.getMetrics()
            .subscribe((metricsResponse) => {
                this.meters = [];
                Object.keys(metricsResponse.meters).forEach(key => {
                    this.meters.push(key);
                });
                console.debug("Uptime", metricsResponse.upTime);
                console.debug("Startime", metricsResponse.startTime);
                const upDuration = moment.duration(metricsResponse.upTime);
                console.debug("Duration", upDuration);
                this.upTime = upDuration;
                this.startTime = moment(metricsResponse.startTime);
                this.nowTime = moment();
            }, (error: Response) => {
            // If we get a NOT_IMPLEMENTED response, that means metrics is disabled
                if (error.status === 501) {
                    this.disabled = true;
                    console.debug("Metrician not enabled");
                } else {
                    console.error(error);
                }
            });
    };

    public ngDoCheck() {
        if (this.selectedValue !== this.oldValue) {
            console.debug("Changed to:", this.selectedValue);
            this.oldValue = this.selectedValue;
            if (this.selectedValue != null) {
                this.addData(this.selectedValue);
            }
        }
    }

    /**
     * Add metrics data
     * @param {string} metric
     */
    public addData(metric: string): void {
        console.debug("Adding data:", metric);
        this.loadingData = true;
        this.ms.getMetricValues(metric, this.startTime.valueOf(), this.nowTime.valueOf())
            .finally(() => this.loadingData = false)
            .subscribe((metricValues) => {
                console.debug("Have metric values:", metricValues);
                this.selectedData = metricValues;
            });
    }

    /**
     * Export all metrics as a CSV file
     */
    public exportAllMetrics = (): void => {
        console.debug("Exporting all metrics");
        this.exportMetrics(null, this.startTime.valueOf(), this.nowTime.valueOf());
    };

    /**
     * Export the currently selected and filtered metrics to a CSV file
     */
    public exportVisibleMetrics(): void {
        this.exportMetrics(this.graph.getVisibleMetrics(),
            this.startTime.valueOf(),
            this.nowTime.valueOf());
    }

    private exportMetrics(metrics: null | string[], start: number, end: number): void {
        this.ms.exportMetricValues(metrics, start, end)
            .subscribe((exportedBlob) => {
                console.debug("Has blob of size: ", exportedBlob.size);
                const fileName = "trestle-metrics-" + start + "-" + (end || "current") + ".csv";
                saveAs(exportedBlob, fileName);
            });
    }
};
