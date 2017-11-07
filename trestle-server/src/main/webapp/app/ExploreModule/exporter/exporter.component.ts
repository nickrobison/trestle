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

    @Input() public dataExport: IDataExport;
    @Input() public label = true;
    public options: { value: string, viewValue: string }[];
    public selectedValue: string;
    public loading: boolean;

    public constructor(private es: ExporterService) {
        this.options = [
            {value: "SHAPEFILE", viewValue: "Shapefile"},
            // {value: "GEOJSON", viewValue: "GeoJSON"},
            // {value: "KML", viewValue: "KML"},
            // {value: "TOPOJSON", viewValue: "TopoJSON"}
        ];
        this.selectedValue = this.options[0].value;
        this.loading = false;
    }

    public click(): void {
        console.debug("Clicked export", this.dataExport);
        // If the input is undefined, or there are not individuals, skip
        if ((this.dataExport !== undefined) &&
            (this.dataExport.individuals.length > 0)) {
            this.loading = true;
            this.es
                .exportIndividuals({
                    dataset: this.dataExport.dataset,
                    individuals: this.dataExport.individuals,
                    type: this.selectedValue
                })
                .finally(() => this.loading = false)
                .subscribe((data) => {
                    console.debug("exported data:", data);
                    const fileName = "trestle-test.zip";
                    saveAs(data, fileName);
                });
        }

    }
}