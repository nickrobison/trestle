import {Component, Input} from "@angular/core";
import {ExporterService} from "./exporter.service";
import {saveAs} from "file-saver";

export interface IDataExport {
    dataset: string;
    individuals: string[];
}

@Component({
    selector: "data-exporter",
    templateUrl: "./exporter.component.html",
    styleUrls: ["./exporter.component.css"]
})
export class ExporterComponent {

    @Input() public individuals: IDataExport;
    public options: { value: string, viewValue: string }[];
    public selectedValue: string;

    public constructor(private es: ExporterService) {
        this.options = [
            {value: "GEOJSON", viewValue: "GeoJSON"},
            {value: "SHAPEFILE", viewValue: "Shapefile"},
            {value: "KML", viewValue: "KML"},
            {value: "TOPOJSON", viewValue: "TopoJSON"}
        ];
        this.selectedValue = this.options[1].value;
    }

    public click(): void {
        console.debug("Clicked export");
        this.es
            .exportIndividuals({
                dataset: this.individuals.dataset,
                individuals: this.individuals.individuals,
                type: this.selectedValue
            })
            .subscribe((data) => {
                console.debug("exported data:", data);
                const fileName = "trestle-test.zip";
                saveAs(data, fileName);
            });
    }
}