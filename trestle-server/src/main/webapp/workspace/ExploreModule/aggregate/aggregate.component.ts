import { Component, OnInit, ViewChild } from "@angular/core";
import { MapService } from "../viewer/map.service";
import { AggregationOperation, AggregationService, BBOX_PROPERTY, IAggregationRestriction } from "./aggregation.service";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { MatSelectChange } from "@angular/material";
import { stringify } from "wellknown";
import { DatasetService } from "../../SharedModule/dataset/dataset.service";
import { AbstractControl, FormBuilder, FormGroup, Validators } from "@angular/forms";

@Component({
    selector: "aggregate",
    templateUrl: "./aggregate.component.html",
    styleUrls: ["./aggregate.component.css"]
})
export class AggregateComponent implements OnInit {

    @ViewChild("map")
    public map: TrestleMapComponent;
    public aggregationForm: FormGroup;
    public selectedAggregation: string;
    public datasets: string[];
    public properties: string[];
    public values: string[];
    public aggregationFields: string[];
    public selectedDs: string;
    public selectedProperty: string;
    public selectedValue: string;
    public mapConfig: mapboxgl.MapboxOptions;
    public dataChanges: ReplaySubject<MapSource>;
    public availableAggregations: { name: string, value: AggregationOperation }[] = [
        {name: "Equals", value: "EQ"},
        {name: "Not Equals", value: "NEQ"},
        {name: "Greater than", value: "GT"},
        {name: "Greater than or equal to", value: "GTEQ"},
        {name: "Less than", value: "LT"},
        {name: "Less than or equal to", value: "LTEQ"}
    ];

    public constructor(private ms: MapService,
                       private as: AggregationService,
                       private ds: DatasetService,
                       private formBuilder: FormBuilder) {
        this.datasets = [];
        this.properties = [];
        this.values = [];

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

        const restrictionGroup = this.formBuilder.group({
            dataset: ["", Validators.required],
            property: ["", [Validators.required, Validators.minLength(1)]],
            value: [undefined, Validators.required]
        });

        const strategyGroup = this.formBuilder.group({
            field: ["", Validators.required],
            operation: ["", Validators.required],
            value: [undefined, Validators.required]
        });

        this.aggregationForm = this.formBuilder.group({
            restriction: restrictionGroup,
            strategy: strategyGroup
        });
    }

    public aggregate(): void {
        console.debug("Aggregate values:", this.aggregationForm.value);

        // Reset the map
        this.map.removeIndividual("aggregation-query");
        let restriction: IAggregationRestriction;
        if (this.getFormValue("restriction", "property") === BBOX_PROPERTY) {
            this.getFormControl("restriction", "property").setValue("asWKT");
            this.getFormControl("restriction", "value").setValue(stringify(MapService.normalizeToGeoJSON(this.map.getMapBounds())));
        }

        this.as.performAggregation(this.aggregationForm.value)
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
                this.properties = [BBOX_PROPERTY].concat(values);
                this.aggregationFields = ["EXISTENCE"].concat(values);
            });
    };

    public propertyChanged = (change: MatSelectChange): void => {
        if (change.value === BBOX_PROPERTY) {
            return;
        }

        this.ds
            .getDatasetFactValues(this.getFormValue("restriction", "dataset"), change.value)
            .subscribe((values) => {
                this.values = values;
            });
    };

    private getFormValue(group: "restriction" | "strategy", control: string): any {
        return this.getFormControl(group, control).value;
    }

    private getFormControl(group: "restriction" | "strategy", control: string): AbstractControl {
        const fControl = this.aggregationForm.get([group, control]);
        if (fControl) {
            return fControl;
        }
        throw new Error("Cannot get control");
    }
}
