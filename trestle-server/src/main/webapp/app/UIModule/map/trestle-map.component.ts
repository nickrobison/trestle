/**
 * Created by nrobison on 6/11/17.
 */
import mapboxgl = require("mapbox-gl");
import {
    Component, EventEmitter, Input, OnChanges, OnInit, Output,
    SimpleChange
} from "@angular/core";
import {
    FeatureCollection, GeometryObject, LineString, MultiLineString, MultiPoint, MultiPolygon,
    Point, Polygon
} from "geojson";
import { MapMouseEvent, LngLatBounds } from "mapbox-gl";
const extent = require("@mapbox/geojson-extent");

export interface ITrestleMapSource {
    id: string;
    idField?: string;
    data: FeatureCollection<GeometryObject>;
}

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit, OnChanges {

    @Input() public individual: ITrestleMapSource;
    @Input() public single: boolean;
    @Input() public multiSelect: boolean;
    @Output() public mapBounds: EventEmitter<LngLatBounds> = new EventEmitter();
    @Output() public clicked: EventEmitter<string> = new EventEmitter();
    private map: mapboxgl.Map;
    private mapSources: string[];

    constructor() {
        mapboxgl.accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";
    }

    public ngOnInit(): void {
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
        this.map.on("moveend", this.moveHandler);
        this.mapBounds.emit(this.map.getBounds());
    }

    public ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
        const inputChanges = changes["individual"];
        if (inputChanges != null
            && !inputChanges.isFirstChange()
            && (inputChanges.currentValue !== inputChanges.previousValue)) {
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
        const idx = this.mapSources.indexOf(removeSource.id);
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
                "fill-opacity": 1,
                // Repaint the lines so that they're still visible
            },
            filter: ["==", "name", ""]
        });
        this.mapSources.push(inputLayer.id);
        //    Center map
        this.getBoundingBox(inputLayer.data);
    }

    private layerClick = (e: MapMouseEvent): void => {
        console.debug("Clicked:", e);
        const features = this.map.queryRenderedFeatures(e.point, {
            layers: this.mapSources.map((val) => val + "-fill")
        });
        // Set the hover filter using either the provided id field, or a default property
        const idField = this.individual.idField == null ? "id" : this.individual.idField;
        console.debug("Accessing ID field:", idField);

        // If we don't filter on anything, deselect it all
        if (!this.multiSelect && !(features.length > 0)) {
            console.debug("Deselecting", this.mapSources);
            this.mapSources.forEach((source) => {
                this.map.setFilter(source + "-hover", ["==", idField, ""]);
            });
            return;
        }
        console.debug("Filtered features", features);
        const feature: any = features[0];
        let layerID = features[0].layer.id;
        // Emit the clicked layer
        const featureID = feature.properties[idField];
        this.clicked.emit(featureID);
        layerID = layerID.replace("-fill", "");
        this.map.setFilter(layerID + "-hover", ["==", idField, featureID]);
        // If multi-select is not enabled, deselect everything else
        if (!this.multiSelect) {
            this.mapSources.forEach((layer) => {
                if (layer !== layerID) {
                    this.map.setFilter(layer + "-hover", ["==", idField, ""]);
                }
            });
        }
    };


    private mouseOver = (e: MapMouseEvent): void => {
        console.debug("Moused over:", e);
    };

    private mouseOut = (e: MapMouseEvent): void => {
        console.debug("Mouse out:", e);
    };

    private moveHandler = () => {
        console.debug("New bounds", this.map.getBounds());
        this.mapBounds.emit(this.map.getBounds());
    };

    private getBoundingBox(geom: FeatureCollection<GeometryObject>): void {
        if (geom.bbox) {
            this.map.fitBounds(LngLatBounds.convert(geom.bbox));
        } else {
            const bbox = extent(geom);
            if (bbox) {
                this.map.fitBounds(LngLatBounds.convert(bbox));
            }
        }
    }

    private static extractGeometryPoints(geom: GeometryObject): number[][] {
        switch (geom.type) {
            case "Point": {
                return [(geom as Point).coordinates];
            }
            case "MultiPoint":
                return (geom as MultiPoint).coordinates;
            case "LineString":
                return (geom as LineString).coordinates;
            case "MultiLineString":
                return (geom as MultiLineString).coordinates[0];
            case "Polygon":
                return (geom as Polygon).coordinates[0];
            case "MultiPolygon":
                return (geom as MultiPolygon).coordinates[0][0];
            default:
                console.error("Unable to get coordinates for object of type:", geom.type);
                return null;
        }
    };
}
