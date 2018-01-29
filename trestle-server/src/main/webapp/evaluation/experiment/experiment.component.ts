import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { EvaluationService, MapState } from "../eval-service/evaluation.service";
import { IMapEventHandler, MapSource, TrestleMapComponent } from "../../workspace/UIModule/map/trestle-map.component";
import { TrestleTemporal } from "../../workspace/SharedModule/individual/TrestleIndividual/trestle-temporal";
import { SelectionTableComponent } from "./selection-table/selection-table.component";
import { ReplaySubject } from "rxjs/ReplaySubject";
import { MatSliderChange } from "@angular/material";
import { ColorService } from "../../workspace/SharedModule/color/color.service";
import { Feature, FeatureCollection, GeometryObject } from "geojson";

@Component({
    selector: "experiment",
    templateUrl: "./experiment.component.html",
    styleUrls: ["./experiment.component.css"]
})
export class ExperimentComponent implements OnInit, AfterViewInit {

    public experimentValue: number;
    public answered: boolean | undefined;
    public dataChanges: ReplaySubject<MapSource>;
    public tableData: string[];
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

    public constructor(private es: EvaluationService, private cs: ColorService) {
        this.experimentValue = 1;
        this.dataChanges = new ReplaySubject<MapSource>(50);
        this.maxHeight = 2016;
        this.minimalSelection = false;

        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [32.3558991, -25.6854313],
            zoom: 8,
            pitch: 40,
            bearing: 20
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
            selectedIndividuals.push(this.tableData[Number.parseInt(row)]);
        });
        console.debug("Selected:", selectedIndividuals);
        // Add results
        this.es.submitResults(finish, this.experimentValue, this.startTime, this.experimentState, this.answered === true, selectedIndividuals);

        if (!finish) {
            this.answered = undefined;
            this.experimentValue += 1;
            this.selectionTable.reset();
            this.loadNextMatch();
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
                console.debug("has it:", experiment);
                console.debug("Overlay?", this.es.isOverlay(experiment.state));
                this.experimentState = experiment.state;

                // Add to table
                this.tableData = experiment.unionOf;

                // Build the geojson feature collection, so we know what to zoom to.
                const features: Array<Feature<GeometryObject>> = [];

                experiment.individuals.forEach((individual, idx) => {
                    console.debug("Individual:", individual, idx);

                    const height = this.getHeight(individual.getTemporal());
                    const baseHeight = ExperimentComponent.getBase(individual.getTemporal());
                    const filteredID = individual.getFilteredID();
                    const isBase = ExperimentComponent.isBaseIndividual(experiment.union, filteredID);
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
                                "fill-extrusion-color": isBase ? "red" : this.cs.getColor(idx),
                                "fill-extrusion-height": isBase ? height + 3001 : height,
                                "fill-extrusion-base": isBase ? baseHeight + 3001 : baseHeight,
                                "fill-extrusion-opacity": isBase ? 1.0 : 0.7
                            }
                        },
                        labelValue: idx.toString()
                    });
                });

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
        console.debug("Base:", baseIndividualID, "Individual:", individualID, "Match?:", individualID === baseIndividualID);
        // Strip off GAUL
        return individualID === baseIndividualID;
    }

    private static getBase(temporal: TrestleTemporal): number {
        return temporal.getFrom().get("year");
    }
}
