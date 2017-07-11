/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import LngLatBounds = mapboxgl.LngLatBounds;
import { ITrestleMapSource } from "../../UIModule/map/trestle-map.component";

@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: string[] = [];
    public loadedDataset: ITrestleMapSource;
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService) {
    }

    public ngOnInit(): void {
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
                this.availableDatasets = results;
            });
    }

    public loadDataset(dataset: string): void {
        console.debug("Loading:", dataset);
        this.mapService.stIntersect(dataset, this.mapBounds, new Date("1990-01-01"))
            .subscribe((data) => {
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