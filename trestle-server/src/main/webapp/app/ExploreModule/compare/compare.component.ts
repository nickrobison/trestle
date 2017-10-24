import {Component, ViewChild} from "@angular/core";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {MapSource, TrestleMapComponent} from "../../UIModule/map/trestle-map.component";
import {IndividualService} from "../../SharedModule/individual/individual.service";
import {TrestleTemporal} from "../../SharedModule/individual/TrestleIndividual/trestle-temporal";
import {schemeCategory10} from "d3-scale";

interface ICompareIndividual {
    individual: TrestleIndividual;
    color: string;
    visible: boolean;
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
    // public selectedIndividuals: TrestleIndividual[];
    public selectedIndividuals: ICompareIndividual[];
    public baseIndividual: TrestleIndividual | null;
    private layerDepth: number;
    private maxHeight: number;
    private layerNumber: number;
    private colorScale: string[];
    @ViewChild(TrestleMapComponent)
    private mapComponent: TrestleMapComponent;

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
        // Use this to pull out colors for the map
        this.colorScale = schemeCategory10;
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
        this.mapComponent
            .removeIndividual(individual.individual.getID());
        const idx = this.selectedIndividuals.indexOf(individual);
        if (idx > -1) {
            this.selectedIndividuals.splice(idx, 1);
        }
    }

    private loadSelectedIndividual(individual: string, base = false): void {
        this.is.getTrestleIndividual(individual)
            .subscribe((result) => {
                const color = this.getColor(this.layerNumber);
                this.mapData = {
                    id: result.getID(),
                    data: {
                        type: "FeatureCollection",
                        features: [
                            {
                                type: "Feature",
                                geometry: result.getSpatialValue(),
                                id: result.getID(),
                                properties: result.getFactValues()
                            }
                        ]
                    },
                    extrude: {
                        id: result.getID() + "-extrude",
                        type: "fill-extrusion",
                        source: result.getID(),
                        paint: {
                            "fill-extrusion-color": color,
                            "fill-extrusion-height": this.getHeight(result.getTemporal()),
                            "fill-extrusion-base": this.getBase(result.getTemporal()),
                            "fill-extrusion-opacity": 0.7
                        }
                    }
                };

                // Are we loading the base selection, or not?
                if (base) {
                    this.baseIndividual = result;
                    //    Lock the map so it doesn't move anymore
                    this.zoomMap = false;
                } else {
                    //    Add the selection to the list
                    this.selectedIndividuals.push({
                        individual: result,
                        color,
                        visible: true
                    });
                }
                this.layerNumber++;
            });
    }

    private getBase(temporal: TrestleTemporal): number {
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
        const color = this.colorScale[layer];
        if (color === null) {
            return "white";
        }
        return color;
    }
}
