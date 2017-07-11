/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";
import LngLatBounds = mapboxgl.LngLatBounds;
import { animate, state, style, transition, trigger } from "@angular/animations";

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED
}

interface IDatasetState {
    name: string,
    state: DatasetState
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
            // transition(":leave", [
            //     style({opacity: 1, transform: "scale(1)"}),
            //     animate("200ms", style({opacity: 0, transform: "scale(0)"}))
            // ])
            // state("in", style({
            //     opacity: "1.0"
            // })),
            // state("out", style({
            //     opacity: "0.0"
            // })),
            // transition("in => out", animate("400ms ease-in-out")),
            // transition("out => in", animate("400ms ease-in-out")),

        ])
    ]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: IDatasetState[] = [];
    public DatasetState = DatasetState;
    public loadedDataset: ITrestleMapSource;
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService) {
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
        if (dataset.state !== DatasetState.UNLOADED) {
            return;
        }
        console.debug("Loading:", dataset.name);
        dataset.state = DatasetState.LOADING;
        this.mapService.stIntersect(dataset.name, this.mapBounds, new Date("1990-01-01"))
            .subscribe((data) => {
                dataset.state = DatasetState.LOADED;
                console.debug("Data:", data);
                this.loadedDataset = {
                    id: "intersection-query",
                    idField: "id",
                    data
                };
            });
    }

    public updateBounds(bounds: LngLatBounds): void {
        this.mapBounds = bounds;
    }
}