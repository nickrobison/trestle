/**
 * Created by nrobison on 6/23/17.
 */
import { Component, OnInit } from "@angular/core";
import { MapService } from "./map.service";


@Component({
    selector: "dataset-viewer",
    templateUrl: "./viewer.component.html",
    styleUrls: ["./viewer.component.css"]
})
export class DatsetViewerComponent implements OnInit {
    public availableDatasets: string[] = [];

    constructor(private mapService: MapService) {}

    public ngOnInit(): void {
        this.mapService.getAvailableDatasets()
            .subscribe((results: string[]) => {
            this.availableDatasets = results;
            })
    }
}