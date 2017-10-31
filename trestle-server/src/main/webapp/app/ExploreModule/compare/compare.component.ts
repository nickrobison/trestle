import {AfterViewInit, Component, ElementRef, ViewChild} from "@angular/core";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {MapSource, TrestleMapComponent} from "../../UIModule/map/trestle-map.component";
import {IndividualService} from "../../SharedModule/individual/individual.service";
import {TrestleTemporal} from "../../SharedModule/individual/TrestleIndividual/trestle-temporal";
import {schemeCategory20b} from "d3-scale";
import {MatSliderChange} from "@angular/material";
import {MapService} from "../viewer/map.service";
import * as moment from "moment";
import {Subject} from "rxjs/Subject";
import {LoadingSpinnerService} from "../../UIModule/spinner/loading-spinner.service";

interface ICompareIndividual {
    individual: TrestleIndividual;
    color: string;
    visible: boolean;
    height: number;
    base: number;
    sliderValue: number;
}

@Component({
    selector: "compare",
    templateUrl: "./compare.component.html",
    styleUrls: ["./compare.component.css"]
})
export class CompareComponent implements AfterViewInit {

    public zoomMap = true;
    public mapConfig: mapboxgl.MapboxOptions;
    public selectedIndividuals: ICompareIndividual[];
    public baseIndividual: ICompareIndividual | null;
    public dataChanges: Subject<MapSource> = new Subject();
    private layerDepth: number;
    private maxHeight: number;
    private layerNumber: number;
    private colorScale: string[];
    // When we delete a layer, we need to recycle its color
    private availableColors: string[];
    @ViewChild(TrestleMapComponent)
    private mapComponent: TrestleMapComponent;
    private currentSliderValue: number;
    @ViewChild("loadable")
    private mapRef: ElementRef;

    constructor(private is: IndividualService,
                private vs: MapService,
                private spinner: LoadingSpinnerService) {


        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8,
            pitch: 40,
            bearing: 20
        };
        this.layerDepth = 50;
        this.maxHeight = 2016;
        this.selectedIndividuals = [];
        // Setup layer coloring
        this.layerNumber = 0;
        this.availableColors = [];
        // Use this to pull out colors for the map
        this.colorScale = schemeCategory20b;
        this.currentSliderValue = 0;
    }

    public ngAfterViewInit(): void {
        console.debug("Child", this.mapComponent);
        this.spinner.setViewContainerRef(this.mapRef.nativeElement);
    }

    public compareIndividuals(): void {

        // Get all the individuals
        if (this.baseIndividual) {
            this.vs.compareIndividuals({
                compare: this.baseIndividual.individual.getID(),
                compareAgainst: this.selectedIndividuals
                // Filter out invisible members
                    .filter((individual) => individual.visible === true)
                    .map((individual) => individual.individual.getID())
            })
                .subscribe((data) => {
                    console.debug("Has data from compare", data);
                });
        }
    }


    public selectedHandler(individual: string): void {
        console.debug("Selected:", individual);
        this.loadSelectedIndividual(individual);
    }

    public addBaseIndividual(individual: string): void {
        this.loadSelectedIndividual(individual, true);
    }

    public reset(): void {
        //    Clear the map
        //    Remove all the individuals from map
        this.mapComponent.clearMap();
        this.zoomMap = true;
        this.selectedIndividuals = [];
        //    Clear the base selection
        this.baseIndividual = null;
        this.layerNumber = 0;
        this.availableColors = [];
    }

    public toggleVisibility(individual: ICompareIndividual): void {
        individual.visible = !individual.visible;
        this.mapComponent
            .toggleIndividualVisibility(individual
                    .individual.getID(),
                individual.visible);
    }

    public removeIndividual(individual: ICompareIndividual): void {
        console.debug("Remove:", individual);
        // Remove from the array first, then from the map
        const idx = this.selectedIndividuals.indexOf(individual);
        if (idx > -1) {
            this.selectedIndividuals.splice(idx, 1);
            this.availableColors.push(individual.color);
        }
        this.mapComponent
            .removeIndividual(individual.individual.getID());
    }

    public sliderUpdate(event: MatSliderChange, selection = this.baseIndividual) {
        if ((event.value !== null) && selection !== null) {
            console.debug("Individual", selection);
            //     For now, let's just change the base individual,
            // we'll figure out the rest later
            const newOffset = (event.value - selection.sliderValue) * 50;
            // If we're changing the base individual, we want to move everything on that level
            // Otherwise, we just want to move the single individual
            const changeIndividual = selection === this.baseIndividual ?
                undefined :
                selection.individual.getID();
            this.mapComponent.change3DOffset(selection.height,
                newOffset,
                changeIndividual);
            selection.sliderValue = event.value;
            selection.height = selection.height + newOffset;
            selection.base = selection.base + newOffset;
        }
    }

    public intersectBaseIndividual(): void {
        if (this.baseIndividual) {
            // this.spinner.reveal();
            this.vs
                .stIntersectIndividual("gaul-test",
                    this.baseIndividual.individual.getSpatialValue(),
                    undefined,
                    moment(),
                    0)
                .subscribe((results) => {
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
                });
        }
    }


    private loadSelectedIndividual(individual: string, baseIndividual = false): void {
        this.is.getTrestleIndividual(individual)
            .subscribe((result) => this.addIndividualToCompare(result, baseIndividual));
    }

    private addIndividualToCompare(individual: TrestleIndividual, baseIndividual = false): void {
        // This is one way to filter out the base individual
        console.debug("Adding %s to map", individual.getFilteredID());
        const color = this.getColor(this.layerNumber);
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
            }
        });

        const compare = {
            individual,
            color,
            visible: true,
            height,
            base: baseHeight,
            sliderValue: 50
        };

        // Are we loading the base selection, or not?
        if (baseIndividual) {
            // Reset the slider value to 0
            compare.sliderValue = 0;
            this.baseIndividual = compare;
            //    Lock the map so it doesn't move anymore
            this.zoomMap = false;
        } else {
            //    Add the selection to the list
            this.selectedIndividuals.push(compare);
        }
        this.layerNumber++;
    }

    private getHeight(temporal: TrestleTemporal): number {
        const to = temporal.getTo();
        if (to === undefined) {
            return this.maxHeight;
        } else {
            return to.get("year");
        }
    }

    private getColor(layer: number): string {
        // See if we have a color available
        const aColor = this.availableColors.pop();
        if (aColor === undefined) {
            const color = this.colorScale[layer];
            if (color === null) {
                return "white";
            }
            return color;
        }
        return aColor;
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }
}
