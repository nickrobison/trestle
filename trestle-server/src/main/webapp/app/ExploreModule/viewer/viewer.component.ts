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
import { VisualizeService } from "../visualize/visualize.service";
import { MatSliderChange } from "@angular/material";

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
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService, private vs: VisualizeService) {
    }

    public ngOnInit(): void {
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
        console.debug("Loading:", dataset.name);
        dataset.state = DatasetState.LOADING;
        this.mapService.stIntersect(dataset.name, this.mapBounds, Moment().year(this.sliderValue).startOf("year"))
            .subscribe((data) => {
                dataset.state = DatasetState.LOADED;
                console.debug("Data:", data);
                this.loadedDataset = {
                    id: "intersection-query",
                    idField: "id",
                    data
                };
            }, (error) => {
                console.error("Error loading dataset:", error);
                dataset.state = DatasetState.ERROR;
            });
    }

    public updateBounds(bounds: LngLatBounds): void {
        this.mapBounds = bounds;
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
                console.log("Has individual", data);
            })
    }
}