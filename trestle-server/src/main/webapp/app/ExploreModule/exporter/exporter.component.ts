import {Component, Input} from "@angular/core";
import {ExporterService} from "./exporter.service";
import {saveAs} from "file-saver";
import {Observable} from "rxjs/Observable";

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

    @Input() public dataExport: IDataExport[];
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
        if ((this.dataExport !== undefined)) {
            this.loading = true;

            const exportArray = this.dataExport
                .filter((de) => de.individuals.length > 0)
                .map((de) => {
                    return this.es.exportIndividuals({
                        dataset: de.dataset,
                        individuals: de.individuals,
                        type: this.selectedValue
                    });
                });
            Observable.forkJoin(exportArray)
                .finally(() => this.loading = false)
                .subscribe((exports) => {
                    exports.forEach((data) => {
                        console.debug("exported data:", data);
                        const fileName = "trestle-test.zip";
                        saveAs(data, fileName);
                    });
                });
            //
            //
            // this.es
            //     .exportIndividuals({
            //         dataset: this.dataExport.dataset,
            //         individuals: this.dataExport.individuals,
            //         type: this.selectedValue
            //     })
            //     .finally(() => this.loading = false)
            //     .subscribe((data) => {
            //
            //     });
        }

    }
}