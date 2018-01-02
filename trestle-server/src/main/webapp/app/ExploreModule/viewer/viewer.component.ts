/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit, ViewChild } from "@angular/core";
import { MapService } from "./map.service";
import { MapSource, TrestleMapComponent } from "../../UIModule/map/trestle-map.component";
import { animate, style, transition, trigger } from "@angular/animations";
import * as moment from "moment";
import { MatSliderChange } from "@angular/material";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";
import { IndividualService } from "../../SharedModule/individual/individual.service";
import { TrestleIndividual } from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import { Subject } from "rxjs/Subject";
import { IDataExport } from "../exporter/exporter.component";
import LngLatBounds = mapboxgl.LngLatBounds;

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

interface IDatasetState {
    name: string;
    state: DatasetState;
    error?: string;
}

@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"],
    animations: [
        trigger("fadeInOut", [
            transition(":enter", [
                style({transform: "scale(0)", opacity: 0}),
                animate("500ms", style({transform: "scale(1)", opacity: 1}))
            ]),
        ])
    ]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: IDatasetState[] = [];
    public DatasetState = DatasetState;
    public minTime = moment("1990-01-01");
    public maxTime = moment("2016-01-01");
    public sliderValue = 2013;
    public selectedIndividual: TrestleIndividual;
    public objectHistory: IIndividualHistory;
    public dataChanges: Subject<MapSource>;
    public exportIndividuals: IDataExport[];
    @ViewChild("map")
    public map: TrestleMapComponent;
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService, private vs: IndividualService) {
        this.dataChanges = new Subject();
        this.exportIndividuals = [];
    }

    public ngOnInit(): void {
        this.objectHistory = {
            entities: []
        };
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
                results.forEach((ds) => {
                    this.availableDatasets.push({
                        name: ds,
                        state: DatasetState.UNLOADED
                    });
                });
            });
    }

    /**
     * Load new data (or an entirely new dataset)
     * If one's already loaded, unload it, unless we mark the keep flag
     * @param {IDatasetState} dataset - dataset to load
     * @param {boolean} keepLoaded - true if we need to keep the dataset loaded, false to unload it
     */
    public loadDataset(dataset: IDatasetState, keepLoaded = false): void {
        if (dataset.state === DatasetState.LOADED && !keepLoaded) {
            console.debug("Unloading dataset:", dataset.name);
            this.map.removeIndividual("intersection-query");
            dataset.state = DatasetState.UNLOADED;
        } else {
            console.debug("Loading:", dataset.name);
            dataset.state = DatasetState.LOADING;
            this.mapService.stIntersect(dataset.name,
                this.mapBounds, moment()
                    .year(this.sliderValue)
                    .startOf("year"))
                .subscribe((data) => {
                    dataset.state = DatasetState.LOADED;
                    console.debug("Data:", data);
                    // Get the list of individuals, for exporting
                    this.exportIndividuals.push({
                        dataset: this.availableDatasets[0].name,
                        individuals: (data.features
                            .filter((feature) => feature.id)
                            // We can do this cast, because we filter to make sure the features have an id
                            .map((feature) => feature.id) as string[])
                    });
                    this.dataChanges.next({
                        id: "intersection-query",
                        idField: "id",
                        data
                    });
                }, (error) => {
                    console.error("Error loading dataset:", error);
                    dataset.state = DatasetState.ERROR;
                    dataset.error = error;
                });
        }
    }

    public updateBounds(bounds: LngLatBounds): void {
        console.debug("Moving, updating bounds", bounds);
        // If we've moved outside of the current bounds, get new data
        if (this.needNewData(bounds)) {
            this.mapBounds = bounds;
            this.availableDatasets
                .filter((ds) => ds.state === DatasetState.LOADED)
                .forEach((ds) => this.loadDataset(ds, true));
        }
        // On the first time around, set the map bounds
        if (!this.mapBounds) {
            this.mapBounds = bounds;
        }
    }

    public sliderChanged = (event: MatSliderChange): void => {
        console.debug("Value changed to:", event);
        if (event.value) {
            this.sliderValue = event.value;
            //    Reload all the currently loaded datasets
            this.availableDatasets
                .filter((ds) => ds.state === DatasetState.LOADED)
                .forEach((ds) => this.loadDataset(ds, true));
        }
    };

    public mapClicked = (event: string): void => {
        console.debug("Clicked:", event);
        this.vs.getTrestleIndividual(event)
            .subscribe((data) => {
                console.debug("Has selection", data);
                this.selectedIndividual = data;
            });
    };

    public getError(ds: IDatasetState): string {
        return ds.error === undefined ? "Error" : ds.error;
    }

    private needNewData(newBounds: mapboxgl.LngLatBounds) {
        console.debug("Need new data", newBounds, "old Data", this.mapBounds);
        // This short circuits the checks to avoid loading data on the first go 'round.
        if (newBounds === null || this.mapBounds === undefined) {
            return false;
        }
        // Moved up/down
        if ((newBounds.getNorth() > this.mapBounds.getNorth())
            || (newBounds.getSouth() < this.mapBounds.getSouth())) {
            console.debug(newBounds.getNorth() + ", " + this.mapBounds.getNorth());
            console.debug(newBounds.getSouth() + ", " + this.mapBounds.getSouth());
            console.debug("Moved north/south, so true");
            return true;
            //    Moved east/west
        } else if ((newBounds.getEast() > this.mapBounds.getEast())
            || (newBounds.getWest() < this.mapBounds.getWest())) {
            console.debug(newBounds.getEast() + ", " + this.mapBounds.getEast());
            console.debug(newBounds.getWest() + ", " + this.mapBounds.getWest());
            console.debug("Moved east/west, so true");
        }
        return false;
    }
}
