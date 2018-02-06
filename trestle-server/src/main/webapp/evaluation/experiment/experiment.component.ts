import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { EvaluationService, MapState } from "../eval-service/evaluation.service";
import { IMapEventHandler, MapSource, TrestleMapComponent } from "../../workspace/UIModule/map/trestle-map.component";
import { TrestleTemporal } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-temporal";
import { ITableData, SelectionTableComponent } from "./selection-table/selection-table.component";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MatSliderChange } from "@angular/material";
import { ColorService } from "../../workspace/SharedModule/color/color.service";
import { Feature, FeatureCollection, GeometryObject } from "geojson";
import { Router } from "@angular/router";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements OnInit, AfterViewInit {

    public experimentValue: number;
    public answered: boolean | undefined;
    public dataChanges: ReplaySubject<MapSource>;
    public tableData: ITableData[];
    @ViewChild(TrestleMapComponent)
    public map: TrestleMapComponent;
    @ViewChild(SelectionTableComponent)
    public selectionTable: SelectionTableComponent;
    public minimalSelection: boolean;
    public mapVisible: "visible" | "hidden";
    public currentSliderValue: number;
    public mapConfig: mapboxgl.MapboxOptions;
    public mapEventHandlers: IMapEventHandler[];

    private experimentState: MapState;
    private maxHeight: number;
    private currentHeight: number;
    private oldSliderValue: number;
    private baseIndividualID: string;
    private startTime: number;
    private unionIndividuals: string[];

    public constructor(private es: EvaluationService,
                       private cs: ColorService,
                       private router: Router) {
        // If we directly navigated to the page, redirect to the intro
        this.es.isRegistered();
        this.experimentValue = 1;
        this.dataChanges = new ReplaySubject<MapSource>(50);
        this.maxHeight = 2016;
        this.minimalSelection = false;
        this.unionIndividuals = [];

        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8
        };
    }

    public ngOnInit(): void {
        this.currentSliderValue = 0;
        this.oldSliderValue = 0;
        this.currentHeight = 3001;
        this.mapVisible = "hidden";
        // Register map handlers
        this.mapEventHandlers = [{
            event: "moveend",
            handler: this.moveHandler
        }];
    }

    public moveHandler = (event: any) => {
        console.debug("MapMoved", event);
        this.es.addMapMove();
    };

    public ngAfterViewInit(): void {
        this.loadNextMatch();
    }

    public next(finish = false): void {
        const selectedRows = this.selectionTable.getSelectedRows();
        // Figure out what individuals the rows correspond to
        const selectedIndividuals: string[] = [];
        selectedRows.forEach((row) => {
            // These idx values need to be decremented, because they start at 1
            selectedIndividuals.push(this.unionIndividuals[row.idxValue - 1]);
        });
        console.debug("Selected:", selectedIndividuals);
        // Add results
        this.es.submitResults(finish, this.experimentValue, this.startTime, this.experimentState, this.answered === true, selectedIndividuals);

        if (!finish) {
            this.answered = undefined;
            this.experimentValue += 1;
            this.selectionTable.reset();
            this.loadNextMatch();
        } else {
            this.router.navigate(["/conclusion"])
        }
    }

    public loadNextMatch() {
        // Make the map invisible and remove everything
        this.mapVisible = "hidden";
        this.map.clearMap();

        // Reset the slider values
        this.currentSliderValue = 0;
        this.oldSliderValue = 0;
        this.currentHeight = 3001;

        this.es.loadExperiment(this.experimentValue)
            .subscribe((experiment) => {
                this.experimentState = experiment.state;

                // Do we need to offset the bearing, or keep it overhead?
                if (this.es.isBearing(this.experimentState)) {
                    this.map.setPitchBearing(40, 20);
                }

                // Add to table
                this.unionIndividuals = experiment.unionOf;

                // Build the geojson feature collection, so we know what to zoom to.
                const features: Array<Feature<GeometryObject>> = [];
                const individualsForTable: ITableData[] = [];

                experiment.individuals.forEach((individual, idx) => {
                    console.debug("Individual:", individual, idx);
                    // Increment the offset because people hate counting by zero
                    const idxOffset = idx + 1;
                    const filteredID = individual.getFilteredID();
                    const isBase = ExperimentComponent.isBaseIndividual(experiment.union, filteredID);
                    const color = this.cs.getColor(idx);
                    if (!isBase) {
                        individualsForTable.push({
                            idxValue: idxOffset,
                            color});
                    }

                    const height = this.getHeight(individual.getTemporal());
                    const baseHeight = ExperimentComponent.getBase(individual.getTemporal());
                    if (isBase) {
                        this.baseIndividualID = filteredID;
                    }

                    const feature: Feature<GeometryObject> = {
                        type: "Feature",
                        geometry: individual.getSpatialValue(),
                        id: filteredID,
                        properties: individual.getFactValues()
                    };
                    features.push(feature);

                    this.dataChanges.next({
                        id: filteredID,
                        data: feature,
                        extrude: {
                            id: filteredID + "-extrude",
                            type: "fill-extrusion",
                            source: filteredID,
                            paint: {
                                // If we're the base individual, make us red and put us on top of everything else
                                "fill-extrusion-color": isBase ? "red" : color,
                                "fill-extrusion-height": isBase ? height + 3001 : height,
                                "fill-extrusion-base": isBase ? baseHeight + 3001 : baseHeight,
                                "fill-extrusion-opacity": this.getLayerOpacity(isBase)
                            }
                        },
                        labelValue: isBase ? undefined : idxOffset.toString()
                    });
                });

                this.tableData = individualsForTable;

                // If we're no-context, reset the base layer with our empty one
                if (this.es.noContext(this.experimentState)) {
                console.debug("Setting no context");
                this.map.setMapStyle("mapbox://styles/nrobison/cjd9tp9o8aa7a2rke3l4i9esq");
                }

                // Build the feature collection and zoom the map
                this.map.centerMap({
                    type: "FeatureCollection",
                    features
                });

                //    Sleep a couple of seconds and then set the map visible again
                //    This is really gross, but who cares?
                this.sleep(4000)
                    .then(() => {
                        this.mapVisible = "visible";
                        this.startTime = Date.now();
                    });
            });
    }

    public minimalSelectionHandler(): void {
        this.minimalSelection = true;
    }

    public showOptions(): boolean {
        return this.answered === true && this.mapVisible === "visible";
    }

    public showNext(): boolean {
        return (this.answered === true && this.minimalSelection === true && this.mapVisible === "visible") ||
            (this.answered === false && this.answered !== undefined  && this.mapVisible === "visible");
    }

    public sliderUpdate(event: MatSliderChange): void {
        if (event.value) {
            const newOffset = (event.value - this.oldSliderValue) * 50;
            this.map.change3DOffset(this.currentHeight,
                newOffset,
                this.baseIndividualID);
            this.oldSliderValue = event.value;
            this.currentHeight = this.currentHeight + newOffset;
            this.es.addSliderChange();
        }
    }

    /**
     * If we're the base individual, or the experiment state is OPAQUE, set the Opacity to 1,
     * otherwise, 0.7
     * @param {boolean} isBase - Are we the base individual?
     * @returns {number} - Opacity from 0 - 1.0
     */
    private getLayerOpacity(isBase: boolean): number {
        if (isBase || this.es.isOpaque(this.experimentState)) {
            return 1.0;
        }
        return 0.7;
    }

    /**
     * Gets the offset height of the polygon, based on its end temporal
     * @param {TrestleTemporal} temporal - End temporal of individual
     * @returns {number} - Offset height
     */
    private getHeight(temporal: TrestleTemporal): number {
        const to = temporal.getTo();
        if (to === undefined) {
            return this.maxHeight;
        } else {
            return to.get("year");
        }
    }

    private sleep(ms: number) {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }

    public static isBaseIndividual(baseIndividualID: string, individualID: string): boolean {
        return individualID === baseIndividualID;
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }
}
