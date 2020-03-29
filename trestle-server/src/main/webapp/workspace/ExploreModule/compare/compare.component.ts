import { AfterViewChecked, AfterViewInit, ChangeDetectorRef, Component, ElementRef, ViewChild } from "@angular/core";
import { TrestleIndividual } from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import { IMapAttributeChange, MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { IndividualService } from "../../SharedModule/individual/individual.service";
import { TrestleTemporal } from "../../SharedModule/individual/TrestleIndividual/trestle-temporal";
import { schemeCategory20b } from "d3-scale";
import { MatSliderChange, MatSlideToggleChange } from "@angular/material";
import { ISpatialComparisonReport, MapService } from "../viewer/map.service";
import * as moment from "moment";
import { Subject } from "rxjs/Subject";
import { LoadingSpinnerService } from "../../UIModule/spinner/loading-spinner.service";
import { interpolateReds } from "d3-scale-chromatic";
import { IDataExport } from "../exporter/exporter.component";
import { parse } from "wellknown";
import { MultiPolygon } from "geojson";
import { ActivatedRoute } from "@angular/router";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { ColorService } from "../../SharedModule/color/color.service";

interface ICompareIndividual {
    individual: TrestleIndividual;
    color: string;
    visible: boolean;
    focused: boolean;
    height: number;
    base: number;
    sliderValue: number;
    report?: ISpatialComparisonReport;
}

interface ILoadingState {
    color: loadingColor;
    type: loadingState;
    visible: boolean;
}

export type loadingState = "determinate" | "indeterminate";
export type loadingColor = "accent" | "warn" | "primary";

@Component({
    selector: "compare",
    templateUrl: "./compare.component.html",
    styleUrls: ["./compare.component.css"]
})
export class CompareComponent implements AfterViewInit, AfterViewChecked {

    public zoomMap = true;
    public mapConfig: mapboxgl.MapboxOptions;
    // public selectedIndividuals: ICompareIndividual[];
    public selectedIndividuals: Map<string, ICompareIndividual>;
    public baseIndividual: ICompareIndividual | null;
    public dataChanges: BehaviorSubject<MapSource | undefined>;
    public layerChanges: Subject<IMapAttributeChange>;
    public exportValues: IDataExport[];
    public loadedOverlap: ISpatialComparisonReport | null;
    public loading: ILoadingState;
    public currentSliderValue: number;

    private filterCompareResults: boolean;
    private layerDepth: number;
    private maxHeight: number;
    private layerNumber: number;
    @ViewChild(TrestleMapComponent)
    private mapComponent: TrestleMapComponent;
    @ViewChild("loadable")
    private mapRef: ElementRef;
    private disabledFocusIndividuals: string[];

    constructor(private is: IndividualService,
                private vs: MapService,
                private spinner: LoadingSpinnerService,
                private route: ActivatedRoute,
                private cdRef: ChangeDetectorRef,
                private cs: ColorService
    ) {

        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8,
            pitch: 40,
            bearing: 20
        };
        this.layerDepth = 50;
        this.maxHeight = 2016;
        this.selectedIndividuals = new Map();
        // Setup layer coloring
        this.layerNumber = 0;
        this.currentSliderValue = 0;
        this.dataChanges = new BehaviorSubject(undefined);
        this.layerChanges = new Subject();
        this.filterCompareResults = true;
        this.exportValues = [{
            dataset: "GAUL",
            individuals: []
        }];
        this.loadedOverlap = null;

        this.loading = {
            color: "primary",
            type: "indeterminate",
            visible: false
        };
        this.disabledFocusIndividuals = [];
    }

    public ngAfterViewInit(): void {
        // Subscribe to route observables
        this.route.queryParams
            .subscribe((queryParams) => {
                console.debug("params", queryParams);
                const individual = queryParams["id"];
                if (individual !== null) {
                    this.addBaseIndividual(individual);
                }
            });

        console.debug("Child", this.mapComponent);
        this.spinner.setViewContainerRef(this.mapRef.nativeElement);
    }

    /**
     * Recheck the view.
     * I'm really not sure why I need this, but SO says so
     * https://stackoverflow.com/questions/43513421/ngif-expression-has-changed-after-it-was-checked
     */
    public ngAfterViewChecked(): void {
        this.cdRef.detectChanges();
    }

    /**
     * Compare the base individual against all the other currently visible objects
     */
    public compareIndividuals(): void {
        // Get all the individuals
        if (this.baseIndividual) {
            this.loading = {
                color: "accent",
                type: "indeterminate",
                visible: true
            };
            this.vs.compareIndividuals({
                compare: this.baseIndividual.individual.getID(),
                compareAgainst: Array.from(this.selectedIndividuals.values())
                // Filter out invisible members
                    .filter((individual) => individual.visible === true)
                    .map((individual) => individual.individual.getID())
            })
                .subscribe((data) => {
                    console.debug("Has data from compare", data);
                    // Add the comparison reports to each individual,
                    // or set them equal to undefined
                    data.reports.forEach((report) => {
                        this.loading.visible = false;
                        const selection = this.selectedIndividuals.get(report.objectBID);
                        if (selection) {
                            if (selection.visible) {
                                selection.report = report;
                                //    Change the color to something on the red scale
                                if (report.spatialOverlapPercentage) {
                                    const interpolated = interpolateReds(
                                        report.spatialOverlapPercentage);
                                    selection.color = interpolated;
                                    this.layerChanges.next({
                                        individual: selection.individual.getID(),
                                        // Change the color and set the opacity a little higher
                                        changes: [
                                            {
                                                attribute: "fill-extrusion-color",
                                                value: interpolated
                                            },
                                            {
                                                attribute: "fill-extrusion-opacity",
                                                value: 0.85
                                            }]
                                    });
                                } else {
                                    // If we don't have any overlap, are we supposed to filter ou those individuals?
                                    if (this.filterCompareResults) {
                                        this.removeIndividual(selection);
                                    }
                                }
                            } else {
                                selection.report = undefined;
                            }
                        }
                    });
                }, (error) => {
                    console.debug(error);
                    this.loading = {
                        color: "warn",
                        type: "determinate",
                        visible: true
                    };
                });
        }
    }

    /**
     * Handler function to load the selected individual
     * @param {string} individual
     */
    public selectedHandler(individual: string): void {
        console.debug("Selected:", individual);
        this.loadSelectedIndividual(individual);
    }

    /**
     * Add base individual to compare
     * @param {string} individual
     */
    public addBaseIndividual(individual: string): void {
        this.loadSelectedIndividual(individual, true);
    }

    /**
     * Reset comparison to the base state
     *
     * If a new individual is provided, add it
     * @param {string} individual
     */
    public reset(individual?: string): void {
        //    Clear the map
        //    Remove all the individuals from map
        this.mapComponent.clearMap();
        this.zoomMap = true;
        // this.selectedIndividuals = [];
        this.selectedIndividuals = new Map();
        //    Clear the base selection
        this.baseIndividual = null;
        this.layerNumber = 0;
        this.currentSliderValue = 0;
        this.cs.reset();

        //    Should we add the given individual to the compare?
        if (individual) {
            this.addBaseIndividual(individual);
        }
    }

    /**
     * Toggle the visibility of the given individual
     * @param {ICompareIndividual} individual
     */
    public toggleVisibility(individual: ICompareIndividual): void {
        individual.visible = !individual.visible;
        this.mapComponent
            .toggleIndividualVisibility(individual
                    .individual.getID(),
                individual.visible);
    }

    /**
     * Toggle the focus of a given individual, this will hide all other currently visible individuals
     * @param {ICompareIndividual} individual
     */
    public toggleFocus(individual: ICompareIndividual): void {
        individual.focused = !individual.focused;

        // If we've selected the focus option, grab all the currently visible layers, disable them, and stash them for later
        if (individual.focused) {
            // If we have previously disabled individuals, reenable them, then do your thing
            if (!(this.disabledFocusIndividuals.length === 0)) {
                this.disabledFocusIndividuals
                    .forEach((fIndividual) => {
                        const mapValue = this.selectedIndividuals.get(fIndividual);
                        if (mapValue) {
                            this.toggleVisibility(mapValue);
                        }
                    });
                // Also, find any other focused layers, and unfocus them.
                this.selectedIndividuals
                    .forEach((value, key) => {
                        if (value !== individual && value.focused) {
                            value.focused = false;
                        }
                    });
                this.disabledFocusIndividuals = [];
            }
        //    Get all the currently visible layers
            this.selectedIndividuals
                .forEach((value, key) => {
                    // If the individual is visible, stash it's value so we can enable it later
                    if (value !== individual && value.visible) {
                        this.disabledFocusIndividuals.push(key);
                        this.toggleVisibility(value);
                    }
                });
        //    If the individual isn't supposed to be focused, grab all the stashed individuals and reenable them.
        } else {
            this.disabledFocusIndividuals
                .forEach((fIndividual) => {
                    const mapValue = this.selectedIndividuals.get(fIndividual);
                    if (mapValue) {
                        this.toggleVisibility(mapValue);
                    }
                });
        //    Reset the focused list
            this.disabledFocusIndividuals = [];
        }
    }

    /**
     * Remove the given individual from the comparison list
     * @param {ICompareIndividual} individual
     */
    public removeIndividual(individual: ICompareIndividual): void {
        console.debug("Remove:", individual);
        // Remove from the array first, then from the map
        this.selectedIndividuals.delete(individual.individual.getID());
        this.mapComponent
            .removeIndividual(individual.individual.getID());

        //    Remove from export
        const idx = this.exportValues[0].individuals.indexOf(individual.individual.getID());
        if (idx > -1) {
            this.exportValues[0].individuals.splice(idx);
        }

    //    Return the color to reuse
        this.cs.returnColor(individual.color);
    }

    /**
     * Update explode slider and change the values on the map
     * @param {MatSliderChange} event
     * @param {ICompareIndividual | null} selection
     */
    public sliderUpdate(event: MatSliderChange, selection = this.baseIndividual) {
        if ((event.value !== null) && (selection !== null)) {
            //     For now, let's just change the base individual,
            // we'll figure out the rest later
            const newOffset = (event.value - selection.sliderValue) * 50;
            this.mapComponent.change3DOffset(selection.height,
                newOffset,
                selection.individual.getID());
            selection.sliderValue = event.value;
            selection.height = selection.height + newOffset;
            selection.base = selection.base + newOffset;
        }
    }

    /**
     * Filter compare results to only return objects which overlap in some way
     * @param {MatSlideToggleChange} event
     */
    public filterChanged(event: MatSlideToggleChange): void {
        this.filterCompareResults = event.checked;
    }

    /**
     * Perform spatial intersection with base individual
     */
    public intersectBaseIndividual(): void {
        if (this.baseIndividual) {
            // this.spinner.reveal();
            this.loading = {
                color: "accent",
                type: "indeterminate",
                visible: true
            };
            this.vs
                .stIntersectIndividual("GAUL",
                    this.baseIndividual.individual.getSpatialValue(),
                    undefined,
                    moment(),
                    0)
                .subscribe((results) => {
                    console.debug("IDs", results.map((indv) => indv.withoutHostname()));
                    // If we have results, turn off the loading bar
                    this.loading.visible = false;
                    results
                        .filter((result) => {
                            // Filter out the base individual,
                            // if it exists, in the grossest way possible
                            if (this.baseIndividual !== null) {
                                return !(result.getID() === this.baseIndividual.individual.getID());
                            }
                            return true;
                        })
                        .forEach((result) => this.addIndividualToCompare(result, false));
                    // this.spinner.hide();
                }, (error) => {
                    console.error(error);
                    this.loading = {
                        color: "warn",
                        type: "determinate",
                        visible: true
                    };
                });
        }
    };

    /**
     * Get all currently selected individuals
     * @returns {ICompareIndividual[]}
     */
    public getSelectedIndividuals(): ICompareIndividual[] {
        return Array.from(this.selectedIndividuals.values());
    }

    /**
     * Pipe function to enable the view to check for changes
     * @returns {ICompareIndividual[]}
     */
    public get mapValues(): ICompareIndividual[] {
        return Array.from(this.selectedIndividuals.values());
    }

    /**
     * Toggle overlap between the base individual and the selected individual
     * @param {ISpatialComparisonReport} overlap
     */
    public toggleOverlap(overlap: ISpatialComparisonReport): void {
        const id = TrestleIndividual.filterID(overlap.objectAID)
            + "-" + TrestleIndividual.filterID(overlap.objectBID);
        // If we have an overlap, and we haven't loaded it yet
        if (overlap.spatialOverlap) {
            // If the loaded overlap is null, add the new one
            if (this.loadedOverlap === null) {
                // Build the change value
                const changes: MapSource = {
                    id,
                    data: {
                        type: "Feature",
                        // TODO(nickrobison): Gross?
                        geometry: (parse(overlap.spatialOverlap) as MultiPolygon),
                        properties: null,
                        id
                    },
                    extrude: {
                        id: id + "-extrude",
                        type: "fill-extrusion",
                        source: id,
                        paint: {
                            "fill-extrusion-color": "blue",
                            "fill-extrusion-height": 3050,
                            "fill-extrusion-base": 3000,
                            "fill-extrusion-opacity": 0.7
                        }
                    }
                };

                //    Turn off all layers except objects A and B that we need
                this.selectedIndividuals.forEach((value) => {
                    if (CompareComponent.filterOverlapIndividuals(value, overlap)) {
                        this.toggleVisibility(value);
                    }
                });

                //    Now, add the new overlap
                this.dataChanges.next(changes);
                this.loadedOverlap = overlap;

                //    If we are the overlap, remove us and turn everything back on
            } else if (this.loadedOverlap === overlap) {
                this.selectedIndividuals.forEach((value) => {
                    if (CompareComponent.filterOverlapIndividuals(value, overlap)) {
                        this.toggleVisibility(value);
                    }
                });
                this.mapComponent.removeIndividual(id);
                this.loadedOverlap = null;
                //    Otherwise, remove the current overlap, and cycle what needs to be toggled
            } else {
                //    Unload the current overlap
                const overlapID = TrestleIndividual.filterID(this.loadedOverlap.objectAID)
                    + "-" + TrestleIndividual.filterID(this.loadedOverlap.objectBID);
                this.mapComponent.removeIndividual(overlapID);
                //    Build the change value
                const changes: MapSource = {
                    id,
                    data: {
                        type: "Feature",
                        // TODO(nickrobison): Gross?
                        geometry: (parse(overlap.spatialOverlap) as MultiPolygon),
                        properties: null,
                        id
                    },
                    extrude: {
                        id: id + "-extrude",
                        type: "fill-extrusion",
                        source: id,
                        paint: {
                            "fill-extrusion-color": "blue",
                            "fill-extrusion-height": 4000,
                            "fill-extrusion-base": 3000,
                            "fill-extrusion-opacity": 0.7
                        }
                    }
                };

                this.selectedIndividuals.forEach((value) => {
                    //    If it's part of the new overlap, and is not visible, turn it on
                    if (!CompareComponent.filterOverlapIndividuals(value, overlap)) {
                        if (!value.visible) {
                            this.toggleVisibility(value);
                        }
                        //    If it's visible and not part of the new overlap, turn it off
                    } else if (value.visible) {
                        this.toggleVisibility(value);
                    }
                });

                //    Set the new overlap
                this.dataChanges.next(changes);
                this.loadedOverlap = overlap;
            }
        }
    }

    /**
     * Load the selected individual by fetching its value from the database
     * @param {string} individual
     * @param {boolean} baseIndividual
     */
    private loadSelectedIndividual(individual: string, baseIndividual = false): void {
        this.is.getTrestleIndividual(individual)
            .subscribe((result) => this.addIndividualToCompare(result, baseIndividual));
    }

    /**
     * Add individual to the comparison set and the map
     * @param {TrestleIndividual} individual
     * @param {boolean} baseIndividual
     */
    private addIndividualToCompare(individual: TrestleIndividual, baseIndividual = false): void {
        console.debug("Adding individual:", individual);
        // Before we add any individuals to the map, we need to see if we're loading the base individual or not
        // This is to deal with some racy behavior between drawing the individual on the map and moving on from the data load
        // It's gross, but what do you expect?
        if (baseIndividual) {
            console.debug("Setting zoom true");
            this.zoomMap = true;
        } else {
            console.debug("Setting zoom false");
            // If zoom is true, manually run the change detection.
            // Why? no idea
            if (this.zoomMap === true) {
                this.zoomMap = false;
                this.cdRef.detectChanges();
            } else {
                this.zoomMap = false;
            }
        }

        // This is one way to filter out the base individual
        console.debug("Adding %s to map", individual.getFilteredID());
        const color = this.cs.getColor(this.layerNumber);
        const height = this.getHeight(individual.getTemporal());
        const baseHeight = CompareComponent.getBase(individual.getTemporal());
        this.dataChanges.next({
            id: individual.getID(),
            data: {
                type: "Feature",
                geometry: individual.getSpatialValue(),
                id: individual.getFilteredID(),
                properties: individual.getFactValues()
            },
            extrude: {
                id: individual.getID() + "-extrude",
                type: "fill-extrusion",
                source: individual.getID(),
                paint: {
                    "fill-extrusion-color": color,
                    "fill-extrusion-height": height,
                    "fill-extrusion-base": baseHeight,
                    "fill-extrusion-opacity": 0.7
                }
            },
            labelField: "adm2_name"
        });

        const compare = {
            individual,
            color,
            visible: true,
            focused: false,
            height,
            base: baseHeight,
            sliderValue: 50
        };

        // Are we loading the base selection, or not?
        if (baseIndividual) {
            // Reset the slider value to 0
            compare.sliderValue = 0;
            this.baseIndividual = compare;
        } else {
            //    Add the selection to the list
            this.selectedIndividuals.set(compare.individual.getID(),
                compare);
            // this.selectedIndividuals.push(compare);
        }
        this.layerNumber++;

        //    Add them to the export record
        this.exportValues[0].individuals.push(compare.individual.getID());
    }

    private getHeight(temporal: TrestleTemporal): number {
        const to = temporal.getTo();
        if (to === undefined) {
            return this.maxHeight;
        } else {
            return to.get("year");
        }
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }

    /**
     * Filter only individuals actually involved in the overlap
     * @param {ICompareIndividual} value
     * @param {ISpatialComparisonReport} overlap
     * @returns {boolean}
     */
    private static filterOverlapIndividuals(value: ICompareIndividual, overlap: ISpatialComparisonReport) {
        const id = value.individual.getID();
        return (id !== overlap.objectAID) &&
            (id !== overlap.objectBID);
    }
}
