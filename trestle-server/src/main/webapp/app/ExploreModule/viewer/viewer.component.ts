/**
 * Created by nrobison on 6/23/17.
 */
import { ChangeDetectorRef, Component, NgZone, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import LngLatBounds = mapboxgl.LngLatBounds;
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";

enum DatasetState {
    UNLOADED,
    LOADING,
    LOADED
}

@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"]
})
export class DatsetViewerComponent implements OnInit {
    // public availableDatasets: string[] = [];
    public DatasetState = DatasetState;
    public loadedDataset: ITrestleMapSource;
    public datasetStates: Map<string, DatasetState> = new Map();
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService, private ref: ChangeDetectorRef) {
    }

    public ngOnInit(): void {
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
                const map = new Map<string, DatasetState>();
                results.forEach((ds) => {
                    map.set(ds, DatasetState.UNLOADED);
                });
                this.datasetStates = map;
                console.debug("DS:", this.datasetStates);
                // this.ref.detectChanges();
            });
    }

    public loadDataset(dataset: string): void {
        console.debug("Loading:", dataset);
        this.datasetStates.set(dataset, DatasetState.LOADING);
        this.mapService.stIntersect(dataset, this.mapBounds, new Date("1990-01-01"))
            .subscribe((data) => {
                this.datasetStates.set(dataset, DatasetState.LOADED);
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