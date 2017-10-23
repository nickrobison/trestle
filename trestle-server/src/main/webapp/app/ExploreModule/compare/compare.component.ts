import {Component} from "@angular/core";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {ITrestleMapSource} from "../../UIModule/map/trestle-map.component";
import {IndividualService} from "../../SharedModule/individual/individual.service";

@Component({
    selector: "compare",
    templateUrl: "./compare.component.html",
    styleUrls: ["./compare.component.css"]
})
export class CompareComponent {

    public zoomMap: true;
    public mapData: ITrestleMapSource;
    public mapConfig: mapboxgl.MapboxOptions;

    constructor(private is: IndividualService) {
        this.zoomMap = true;
        this.mapConfig = {
            style: "mapbox://styles/nrobison/cj3n7if3q000s2sutls5a1ny7",
            center: [-87.61694, 41.86625],
            zoom: 15.99,
            pitch: 40,
            bearing: 20
        };
    }

    public selectedHandler(individual: string): void {
        console.debug("Selected:", individual);
        this.loadSelectedIndividual(individual);
    }


    private loadSelectedIndividual(individual: string): void {
        this.is.getTrestleIndividual(individual)
            .subscribe((results: TrestleIndividual) => {
                this.mapData = {
                    id: results.getID(),
                    data: {
                        type: "FeatureCollection",
                        features: [
                            {
                                type: "Feature",
                                geometry: results.getSpatialValue(),
                                id: results.getID(),
                                properties: results.getFactValues()
                            }
                        ]
                    }
                };
            });
    }
}
