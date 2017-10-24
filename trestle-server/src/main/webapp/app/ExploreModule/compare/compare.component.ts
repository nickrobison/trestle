import {Component, ViewChild} from "@angular/core";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {MapSource, TrestleMapComponent} from "../../UIModule/map/trestle-map.component";
import {IndividualService} from "../../SharedModule/individual/individual.service";
import {TrestleTemporal} from "../../SharedModule/individual/TrestleIndividual/trestle-temporal";
import {schemeCategory10} from "d3-scale";
import {MatSliderChange} from "@angular/material";

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
export class CompareComponent {

    public zoomMap = true;
    public mapData: MapSource;
    public mapConfig: mapboxgl.MapboxOptions;
    public selectedIndividuals: ICompareIndividual[];
    public baseIndividual: ICompareIndividual | null;
    private layerDepth: number;
    private maxHeight: number;
    private layerNumber: number;
    private colorScale: string[];
    // When we delete a layer, we need to recycle its color
    private availableColors: string[];
    @ViewChild(TrestleMapComponent)
    private mapComponent: TrestleMapComponent;
    private currentSliderValue: number;

    constructor(private is: IndividualService) {
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
        this.colorScale = schemeCategory10;
        this.currentSliderValue = 0;
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


    private loadSelectedIndividual(individual: string, baseIndividual = false): void {
        this.is.getTrestleIndividual(individual)
            .subscribe((result) => {
                const color = this.getColor(this.layerNumber);
                const height = this.getHeight(result.getTemporal());
                const baseHeight = CompareComponent.getBase(result.getTemporal());
                this.mapData = {
                    id: result.getID(),
                    data: {
                        type: "Feature",
                        geometry: result.getSpatialValue(),
                        id: result.getFilteredID(),
                        properties: result.getFactValues()
                    },
                    extrude: {
                        id: result.getID() + "-extrude",
                        type: "fill-extrusion",
                        source: result.getID(),
                        paint: {
                            "fill-extrusion-color": color,
                            "fill-extrusion-height": height,
                            "fill-extrusion-base": baseHeight,
                            "fill-extrusion-opacity": 0.7
                        }
                    }
                };
                console.debug("new map data:", this.mapData);
                const compare = {
                    individual: result,
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
            });
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
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
}
