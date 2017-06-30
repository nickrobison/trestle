/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";
import LngLatBounds = mapboxgl.LngLatBounds;


@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: string[] = [];
    private mapBounds: LngLatBounds;

    constructor(private mapService: MapService) {}

    public ngOnInit(): void {
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
            this.availableDatasets = results;
            });
    }

    public loadDataset(dataset: string): void {
        console.debug("Loading:", dataset);
        this.mapService.tsIntersect(dataset, this.mapBounds, new Date("1990-01-01"));
    }

    public updateBounds(bounds: LngLatBounds): void {
        this.mapBounds = bounds;
    }
}