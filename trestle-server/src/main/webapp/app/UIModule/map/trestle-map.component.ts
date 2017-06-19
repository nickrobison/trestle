/**
 * Created by nrobison on 6/11/17.
 */
import mapboxgl = require("mapbox-gl");
import {Component, Input, OnChanges, OnInit, SimpleChange} from "@angular/core";
import {FeatureCollection, GeometryObject} from "geojson";
import {MapMouseEvent} from "mapbox-gl";

export interface ITrestleMapSource {
    id: string;
    data: FeatureCollection<GeometryObject>;
}

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit, OnChanges {

    @Input() individual: ITrestleMapSource;
    @Input() single: boolean;
    @Input() multiSelect: boolean;
    private map: mapboxgl.Map;
    private mapSources: Array<string>;

    constructor() {
        mapboxgl.accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";
    }

    ngOnInit(): void {
        console.debug("Creating map, singleSelect?", this.single, "mulitSelect?", this.multiSelect);
        this.mapSources = [];
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
            if (inputChanges.previousValue != null && this.single) {
                this.removeSource(inputChanges.previousValue);
            }
            this.addSource(inputChanges.currentValue);
        }
    }

    private removeSource(removeSource: ITrestleMapSource): void {
        this.map.removeLayer(removeSource.id + "-fill");
        this.map.removeLayer(removeSource.id + "-line");
        this.map.removeLayer(removeSource.id + "-hover");
        this.map.removeSource(removeSource.id);
        let idx = this.mapSources.indexOf(removeSource.id);
        if (idx >= 0) {
            this.mapSources.splice(idx, 1);
        }
    }

    private addSource(inputLayer: ITrestleMapSource): void {
        console.debug("Adding source data:", inputLayer.data);
        this.map.addSource(inputLayer.id, {
            type: "geojson",
            data: inputLayer.data
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
        });
        this.map.addLayer({
            id: inputLayer.id + "-hover",
            type: "fill",
            source: inputLayer.id,
            paint: {
                "fill-color": "#627BC1",
                "fill-opacity": 1
            },
            filter: ["==", "name", ""]
        });
        this.mapSources.push(inputLayer.id);
    }

    private layerClick = (e: MapMouseEvent): void => {
        console.debug("Clicked:", e);
        let features = this.map.queryRenderedFeatures(e.point, {
           layers: this.mapSources.map(val => val + "-fill")
        });

        // If we don't filter on anything, deselect it all
        if (!this.multiSelect && !(features.length > 0)) {
            console.debug("Deselecting", this.mapSources);
            this.mapSources.forEach(source => {
                this.map.setFilter(source + "-hover", ["==", "id", ""]);
            });
            return;
        }
        console.debug("Filtered features", features);
        let feature: any = features[0];
        let layerID = features[0].layer.id;
        layerID = layerID.replace("-fill", "");
        this.map.setFilter(layerID + "-hover", ["==", "id", feature.properties.id]);
        // If multi-select is not enabled, deselect everything else
        if (!this.multiSelect) {
            this.mapSources.forEach(layer => {
                if (layer !== layerID) {
                    this.map.setFilter(layer + "-hover", ["==", "id", ""])
                }
            });
        }
    };


    private mouseOver = (e: MapMouseEvent): void => {
        console.debug("Moused over:", e);
    };

    private mouseOut = (e: MapMouseEvent): void => {
        console.debug("Mouse out:", e);
    }
}
