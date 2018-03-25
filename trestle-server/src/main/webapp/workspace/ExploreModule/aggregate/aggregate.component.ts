import { Component, OnInit } from "@angular/core";
import { MapService } from "../viewer/map.service";
import { AggregationService } from "./aggregation.service";


@Component({
    selector: "aggregate",
    templateUrl: "./aggregate.component.html",
    styleUrls: ["./aggregate.component.css"]
})
export class AggregateComponent implements OnInit {

    public selectedAggregation: string;
    public datasets: string[];
    public selectedDs: string;

    public constructor(private ms: MapService, private as: AggregationService) {
        this.datasets = [];
    }

    public ngOnInit(): void {
        this.ms
            .getAvailableDatasets()
            .do(console.log)
            .subscribe((ds) => {
                this.datasets = ds;
            });
    }

    public aggregate(): void {
        this.as.performAggregation("gaul-test", "exists", "hi")
            .subscribe((agg) => {
                console.debug("Done", agg);
            });
    }
}
