/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";
import { animate, style, transition, trigger } from "@angular/animations";
import * as Moment from "moment";
import LngLatBounds = mapboxgl.LngLatBounds;
import moment = require("moment");
import { MatSliderChange } from "@angular/material";
import { IIndividualHistory } from "../../UIModule/history-graph/history-graph.component";
import { Observable } from "rxjs/Observable";
import { VisualizeService } from "../visualize/visualize.service";
import { TrestleIndividual } from "../visualize/individual/trestle-individual";

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED,
    ERROR
}

interface IDatasetState {
    name: string,
    state: DatasetState;
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
    public loadedDataset: ITrestleMapSource;
    public minTime = moment("1990-01-01");
    public maxTime = moment("2016-01-01");
    public sliderValue = 2013;
    public selectedIndividual: TrestleIndividual;
    public objectHistory: IIndividualHistory;
    private mapBounds: LngLatBounds;
    private mapLocked = false;

    constructor(private mapService: MapService, private vs: VisualizeService) {
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

    public loadDataset(dataset: IDatasetState): void {
        this.mapLocked = true;
        console.debug("Loading:", dataset.name);
        dataset.state = DatasetState.LOADING;
        this.mapService.stIntersect(dataset.name,
            this.mapBounds, Moment()
                .year(this.sliderValue)
                .startOf("year"))
            .subscribe((data) => {
                dataset.state = DatasetState.LOADED;
                console.debug("Data:", data);
                this.loadedDataset = {
                    id: "intersection-query",
                    idField: "id",
                    data
                };
                this.mapLocked = false;
            }, (error) => {
                console.error("Error loading dataset:", error);
                dataset.state = DatasetState.ERROR;
                this.mapLocked = false;
            });
    }

    public updateBounds(bounds: LngLatBounds): void {
        console.debug("Moving, updating bounds", bounds);
        // If we've moved outside of the current bounds, get new data
        if (!this.mapLocked && this.needNewData(bounds)) {
            this.mapBounds = bounds;
            // this.availableDatasets
            //     .filter((ds) => ds.state === DatasetState.LOADED)
            //     .forEach((ds) => this.loadDataset(ds));
        } else {
            this.mapBounds = bounds;
        }
    }

    public sliderChanged = (event: MatSliderChange): void => {
        console.debug("Value changed to:", event);
        this.sliderValue = event.value;
        //    Reload all the currently loaded datasets
        this.availableDatasets
            .filter((ds) => ds.state === DatasetState.LOADED)
            .forEach((ds) => this.loadDataset(ds));
    };

    public mapClicked = (event: string): void => {
        console.debug("Clicked:", event);
        this.vs.getIndividualAttributes(event)
            .subscribe((data) => {
                console.debug("Has individual", data);
                this.buildHistoryGraph(data);
            });
    };

    private buildHistoryGraph(individual: TrestleIndividual): void {
        console.debug("has events", individual.getEvents());
        //    Get the split/merged events
        const splitMerge = individual
            .getRelations()
            .filter((relation) => (relation.getType() === "MERGED_FROM")
                || (relation.getType() === "MERGED_INTO")
                || (relation.getType() === "SPLIT_FROM")
                || (relation.getType() === "SPLIT_INTO"));
        const history: IIndividualHistory = {
            entities: []
        };
        history.entities.push({
            label: this.filterID(individual.getID()),
            start: individual.getTemporal().getFrom().toDate(),
            end: individual.getTemporal().getTo().toDate(),
            value: individual.getID()
        });
        //    For all the other individuals, add them as well
        console.debug("Has some individuals:", splitMerge.length);
        const obsArray = splitMerge.map((relation) => {
            console.debug("Getting attributes for:", relation.getObject());
            return this.vs.getIndividualAttributes(relation.getObject());
        });
        Observable.forkJoin(obsArray)
            .subscribe((objects) => {
                console.debug("Have all observables:", objects);
                objects.forEach((object) => {
                    history.entities.push({
                        label: this.filterID(object.getID()),
                        start: object.getTemporal().getFrom().toDate(),
                        end: object.getTemporal().getTo().toDate(),
                        value: object.getID()
                    });
                });
                console.debug("History", history);
                this.objectHistory = history;
            });
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

    private filterID(id: string): string {
        const strings = id.split("#");
        const idStrings = strings[1].split(":");
        return idStrings[0] + ":" + idStrings[1];
    }
}
