import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MapService, wktValue } from "../viewer/map.service";
import { AggregationService, IAggregationRestriction } from "./aggregation.service";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { MatSelectChange } from "@angular/material";
import { stringify } from "wellknown";
import { DatasetService } from "../../SharedModule/dataset/dataset.service";

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
    public properties: string[];
    public countries: string[];
    public selectedDs: string;
    public selectedProperty: string;
    public selectedValue: string;
    public mapConfig: mapboxgl.MapboxOptions;
    public dataChanges: ReplaySubject<MapSource>;

    private readonly BBOX_PROPERTY = "BOUNDING_BOX";

    public constructor(private ms: MapService,
                       private as: AggregationService,
                       private ds: DatasetService) {
        this.datasets = [];
        this.properties = [];
        this.countries = [];

        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8
        };
        this.dataChanges = new ReplaySubject<MapSource>(50);
    }

    public ngOnInit(): void {
        this.ds
            .getAvailableDatasets()
            .do(console.log)
            .subscribe((ds) => {
                this.datasets = ds;
            });
    }

    public aggregate(): void {
        // Reset the map
        this.map.removeIndividual("aggregation-query");
        let restriction: IAggregationRestriction<string>;
        if (this.selectedProperty !== this.BBOX_PROPERTY) {
            restriction = {
                fact: this.selectedProperty,
                value: this.selectedValue
            };
        } else {
            restriction = {
                fact: "asWKT",
                value: stringify(MapService.normalizeToGeoJSON(this.map.getMapBounds()))
            };
        }

        this.as.performAggregation(this.selectedDs, "exists", restriction)
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
        this.ds
            .getDatasetProperties(change.value)
            .subscribe((values) => {
                this.properties = [this.BBOX_PROPERTY].concat(values);
            });
    };

    public propertyChanged = (change: MatSelectChange): void => {
        if (change.value === this.BBOX_PROPERTY) {
            return;
        }

        this.ds
            .getDatasetFactValues(this.selectedDs, change.value)
            .subscribe((values) => {
                this.countries = values;
            });
    };

    public showPropertyValues(): boolean {
        return this.selectedProperty !== undefined && this.selectedProperty !== this.BBOX_PROPERTY;
    }
}
