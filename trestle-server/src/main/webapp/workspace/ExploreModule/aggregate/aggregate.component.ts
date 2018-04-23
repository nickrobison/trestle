import { Component, OnInit, ViewChild } from "@angular/core";
import { MapService } from "../viewer/map.service";
import { AggregationOperation, AggregationService, BBOX_PROPERTY } from "./aggregation.service";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { stringify } from "wellknown";
import { DatasetService } from "../../SharedModule/dataset/dataset.service";
import { AbstractControl, FormBuilder, FormGroup, Validators } from "@angular/forms";
import { filter, switchMap, tap } from "rxjs/operators";
import { distinctTime } from "../../SharedModule/operators/rxjs.operators";

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
    public inProgress = false;
    public restrictionValueHidden = true;

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
            zoom: 1
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
            property: "",
            value: undefined,
            validator: AggregateComponent.restrictionValidation
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

        // Monitor Restriction dataset changes
        this.getFormControl("restriction", "dataset")
            .valueChanges
            .pipe(
                distinctTime(400),
                // Reset form values
                tap(() => this.getFormControl("restriction", "value").reset()),
                // Fetch dataset properties
                tap((value) => this.getFormControl("restriction", "dataset").setValue(value)),
                switchMap((value) => this.ds.getDatasetProperties(value))
            )
            .subscribe((values) => {
                this.properties = [BBOX_PROPERTY].concat(values);
                this.aggregationFields = ["EXISTENCE"].concat(values);
            });

        // Monitor Restriction property changes
        this.getFormControl("restriction", "property")
            .valueChanges
            .pipe(
                distinctTime(400),
                // Hide value selection if BBOX
                tap((value) => {
                    // If bounding box, hide the value, but give it a dummy value
                    const valueControl = this.getFormControl("restriction", "property");
                    if (!value || value === BBOX_PROPERTY) {
                        this.restrictionValueHidden = true;
                        valueControl.setValue("wktValue");
                    } else {
                        valueControl.reset();
                        this.restrictionValueHidden = false;
                    }
                    this.getFormControl("restriction", "property").setValue(value)
                }),
                // Filter out BBOX and empty values
                filter((value) => value && value !== BBOX_PROPERTY),
                // Fetch Property values
                switchMap((value) => this.ds
                    .getDatasetFactValues(this.getFormValue("restriction", "dataset"), value)))
            .subscribe((propertyValues) => {
                this.values = propertyValues;
            });
    }

    public aggregate(): void {
        console.debug("Aggregate values:", this.aggregationForm.value);

        // Reset the map
        this.map.removeIndividual("aggregation-query");
        // Special casing of WKT
        if (this.getFormValue("restriction", "property") === BBOX_PROPERTY) {
            this.getFormControl("restriction", "property").setValue("asWKT");
            this.getFormControl("restriction", "value").setValue(stringify(MapService.normalizeToGeoJSON(this.map.getMapBounds())));
        }

        // Special casing of ExistsFrom
        if (this.getFormValue("strategy", "field") === "EXISTENCE") {
            this.getFormControl("strategy", "field").setValue("trestle:existsFrom");
        }
        this.inProgress = true;
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
                this.inProgress = false;
            });
        //    Reset from the special casing
        if (this.getFormValue("restriction", "property") === "asWKT") {
            this.getFormControl("restriction", "property").setValue(BBOX_PROPERTY);
            this.getFormControl("restriction", "value").setValue(undefined);
        }

        if (this.getFormValue("strategy", "field") === "trestle:existsFrom") {
            this.getFormControl("strategy", "field").setValue("EXISTENCE");
        }

    }

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

    private static restrictionValidation(f: FormGroup): void {
        const propertyControl = f.controls["property"];
        const valueControl = f.controls["value"];
        const value = valueControl.value;
        if (propertyControl.value === BBOX_PROPERTY) {
            valueControl.setErrors(null);
        } else if (value === "") {
            valueControl.setErrors({
                minimumLength: true
            });
        } else if (value === undefined) {
            valueControl.setErrors({
                required: true
            });
        } else {
            valueControl.setErrors(null);
        }
    };
}
