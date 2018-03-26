import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MapService, wktValue } from "../viewer/map.service";
import { AggregationService } from "./aggregation.service";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";


@Component({
    selector: "aggregate",
    templateUrl: "./aggregate.component.html",
    styleUrls: ["./aggregate.component.css"]
})
export class AggregateComponent implements OnInit {

    @ViewChild("map")
    public map: TrestleMapComponent;
    public selectedAggregation: string;
    public datasets: string[];
    public selectedDs: string;
    public mapConfig: mapboxgl.MapboxOptions;
    public dataChanges: ReplaySubject<MapSource>;

    public constructor(private ms: MapService, private as: AggregationService) {
        this.datasets = [];

        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8
        };
        this.dataChanges = new ReplaySubject<MapSource>(50);
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
        this.as.performAggregation("gaul-test", "exists", this.map.getMapBounds())
            .subscribe((agg) => {
                console.debug("Done", agg);
            });
    }
}
