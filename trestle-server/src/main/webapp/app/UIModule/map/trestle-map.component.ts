/**
 * Created by nrobison on 6/11/17.
 */
import mapboxgl = require("mapbox-gl");
import {Component, Input, OnChanges, OnInit, SimpleChange} from "@angular/core";
import {GeometryObject} from "geojson";
import {MapMouseEvent} from "mapbox-gl";

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
            style: "mapbox://styles/mapbox/light-v9",
            center: new mapboxgl.LngLat(32.3558991, -25.6854313),
            // center: [-74.50, 40], // starting position
            zoom: 8 // starting zoom
        });
        this.map.on("click", this.layerClick);
        this.map.on("mouseover", this.mouseOver);
        this.map.on("mouseleave", this.mouseOut);
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
        console.debug("Adding source data:", inputLayer.data);
        this.map.addSource(inputLayer.id, {
            type: "geojson",
            data: {
                type: "Feature",
                geometry: inputLayer.data
            }
        });
        this.map.addLayer({
            id: inputLayer.id + "-fill",
            type: "fill",
            source: inputLayer.id,
            paint: {
                "fill-color": "#627BC1",
                "fill-opacity": 0.7,
            }
        });
        this.map.addLayer({
            id: inputLayer.id + "-line",
            type: "line",
            source: inputLayer.id,
            paint: {
                "line-color": "white",
                "line-width": 2
            }
        })
    }

    private removeSource(sourceID: string): void {
        this.map.removeLayer(sourceID + "-fill");
        this.map.removeLayer(sourceID + "-line");
        this.map.removeSource(sourceID);
    };

    private layerClick = (e: MapMouseEvent): void => {
        console.debug("Clicked:", e);
    };

    private mouseOver = (e: MapMouseEvent): void => {
        console.debug("Moused over:", e);
    };

    private mouseOut = (e: MapMouseEvent): void => {
        console.debug("Mouse out:", e);
    }
}
