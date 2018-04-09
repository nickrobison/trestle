import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MapService, wktValue } from "../viewer/map.service";
import { AggregationService, IAggregationRestriction } from "./aggregation.service";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { MatSelectChange } from "@angular/material";
import { stringify } from "wellknown";

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
    public countries: string[];
    public selectedDs: string;
    public selectedCountry: string;
    public mapConfig: mapboxgl.MapboxOptions;
    public dataChanges: ReplaySubject<MapSource>;

    public constructor(private ms: MapService, private as: AggregationService) {
        this.datasets = [];
        this.countries = [];

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
        let restriction: IAggregationRestriction<string>;
        if (this.selectedCountry) {
            restriction = {
                fact: "adm0_name",
                value: this.selectedCountry
            };
        } else {
            restriction = {
                fact: "asWKT",
                value: stringify(MapService.normalizeToGeoJSON(this.map.getMapBounds()))
            };
        }

        this.as.performAggregation("gaul-test", "exists", restriction)
            .subscribe((agg) => {
                console.debug("Done", agg);
                this.dataChanges.next({
                    id: "aggregation-query",
                    idField: "id",
                    data: {
                        type: "Feature",
                        geometry: agg,
                        properties: null,
                        id: "test"
                    }
                });
            });
    }

    public datasetChanged = (change: MatSelectChange): void => {
        console.debug("Changed to:", change);
        this.ms
            .getDatasetFactValues(change.value, "adm0_name", 100)
            .subscribe((values) => {
                this.countries = values;
            });
    }
}
