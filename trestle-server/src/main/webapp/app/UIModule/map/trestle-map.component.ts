/**
 * Created by nrobison on 6/11/17.
 */
import mapboxgl = require("mapbox-gl");
import {Component, Input, OnChanges, OnInit, SimpleChange} from "@angular/core";
import {GeometryObject} from "geojson";

export interface ITrestleMapSource {
    id: string;
    data: GeometryObject;
}

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit, OnChanges {

    @Input() individual: ITrestleMapSource;
    private map: mapboxgl.Map;

    constructor() {
        mapboxgl.accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";
    }

    ngOnInit(): void {
        this.map = new mapboxgl.Map({
            container: "map",
            style: "mapbox://styles/mapbox/dark-v9",
            center: [-74.50, 40], // starting position
            zoom: 9 // starting zoom
        })
    }

    ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
        let inputChanges = changes["individual"];
        if (inputChanges != null && !inputChanges.isFirstChange() && (inputChanges.currentValue !== inputChanges.previousValue)) {
            console.debug("New change, updating", inputChanges);
            if (inputChanges.previousValue != null) {
                this.removeSource(inputChanges.previousValue.id);
            }
            this.addSource(inputChanges.currentValue);
        }
    }

    private addSource(inputLayer: ITrestleMapSource): void {
        // let source = new mapboxgl.GeoJSONSource();
        // source.setData({
        //     type: "Feature",
        //     properties: null,
        //     geometry: inputLayer.data
        // });
        this.map.addSource(inputLayer.id, {
            type: "geojson",
            data: {
                type: "Feature",
                geometry: inputLayer.data
            }
        });
    }

    private removeSource(sourceID: string): void {
        this.map.removeSource(sourceID);
    }




}