/**
 * Created by nrobison on 6/11/17.
 */
import * as mapboxgl from "mapbox-gl";
import {LngLatBounds, MapMouseEvent} from "mapbox-gl";
import extent from "@mapbox/geojson-extent";
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange
} from "@angular/core";
import {
    FeatureCollection,
    GeometryObject,
    LineString,
    MultiLineString,
    MultiPoint,
    MultiPolygon,
    Point,
    Polygon
} from "geojson";
import {BehaviorSubject} from "rxjs/BehaviorSubject";

export interface IMapFillLayer extends mapboxgl.Layer {
    type: "fill";
}

export interface IMapLineLayer extends mapboxgl.Layer {
    type: "line";
}

export interface IMapHoverLayer extends mapboxgl.Layer {
    type: "fill";
    filter: ["==", "name", ""];
}

export interface ITrestleMapLayers {
    fill?: IMapFillLayer;
    line?: IMapLineLayer;
    hover?: IMapHoverLayer;
}

export interface ITrestleMapSource {
    id: string;
    idField?: string;
    data: FeatureCollection<GeometryObject>;
    layers?: ITrestleMapLayers;
}

export interface I3DMapSource extends ITrestleMapSource {
    extrude: mapboxgl.Layer;
}

export type MapSource = I3DMapSource | ITrestleMapSource;

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit, OnChanges {

    @Input() public individual: MapSource;
    @Input() public single: boolean;
    @Input() public multiSelect: boolean;
    @Input() public zoomOnLoad?: boolean;
    @Input() public config?: mapboxgl.MapboxOptions;
    @Output() public mapBounds: EventEmitter<LngLatBounds> = new EventEmitter();
    @Output() public clicked: EventEmitter<string> = new EventEmitter();
    private centerMapOnLoad: BehaviorSubject<boolean>;
    private baseConfig: mapboxgl.MapboxOptions;
    private baseStyle: ITrestleMapLayers;
    private map: mapboxgl.Map;
    private mapSources: Map<string, string[]>;

    constructor() {
        // FIXME(nrobison): Fix this
        (mapboxgl as any).accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";

        this.mapSources = new Map();

        //    Set defaults
        this.setupDefaults();
    }

    public ngOnInit(): void {
        if (this.zoomOnLoad === undefined) {
            this.centerMapOnLoad = new BehaviorSubject(true);
        } else {
            this.centerMapOnLoad = new BehaviorSubject(this.zoomOnLoad);
        }

        console.debug("Creating map, " +
            "singleSelect?", this.single,
            "mulitSelect?", this.multiSelect,
            "zoom?", this.centerMapOnLoad.getValue());

        // Merge the map configs together
        const mergedConfig = Object.assign(this.baseConfig, this.config);
        this.map = new mapboxgl.Map(mergedConfig);

        this.map.on("click", this.layerClick);
        this.map.on("mouseover", this.mouseOver);
        this.map.on("mouseleave", this.mouseOut);
        this.map.on("moveend", this.moveHandler);
        this.mapBounds.emit(this.map.getBounds());
    }

    public ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
        // Individual changes
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

        //    Zoom On Load changes
        const zoomChanges = changes["zoomOnLoad"];
        if (zoomChanges != null
            && !zoomChanges.isFirstChange()) {
            console.debug("Changing zoom value");
            this.centerMapOnLoad.next(zoomChanges.currentValue);
        }
    }

    public removeIndividual(individual: string): void {
        console.debug("Removing selection %s from the map", individual);
        // //    Figure out which layer the selection is a part of and remove it
        //     // FIXME(nrobison): Get rid of this type cast.
        //     this.map.querySourceFeatures()
        //     const features: any[] = this.map.queryRenderedFeatures(e.point, {
        //         layers: this.mapSources.map((val) => val + "-fill")
        //     });
        //     // Set the hover filter using either the provided id field, or a default property
        //     const idField = this.selection.idField == null ? "id" : this.selection.idField;
        //     console.debug("Accessing ID field:", idField);
        //
        //     // If we don't filter on anything, deselect it all
        //     if (!this.multiSelect && !(features.length > 0)) {
        //         console.debug("Deselecting", this.mapSources);
        //         this.mapSources.forEach((source) => {
        //             this.map.setFilter(source + "-hover", ["==", idField, ""]);
        //         });
        //         return;
        //     }
        //     console.debug("Filtered features", features);
        //     const feature: any = features[0];
        //     let layerID = features[0].layer.id;
        //     // Emit the clicked layer
        //     const featureID = feature.properties[idField];
        //     this.clicked.emit(featureID);
        //     layerID = layerID.replace("-fill", "");
        //     this.map.setFilter(layerID + "-hover", ["==", idField, featureID]);
        //     // If multi-select is not enabled, deselect everything else
        //     if (!this.multiSelect) {
        //         this.mapSources.forEach((layer) => {
        //             if (layer !== layerID) {
        //                 this.map.setFilter(layer + "-hover", ["==", idField, ""]);
        //             }
        //         });
        //     }
    }

    public toggleIndividualVisibility(individual: string, setVisible: boolean): void {
        console.debug("setting visible?", setVisible);
        //    See if the individual is a source
        const layers = this.mapSources.get(individual);
        if (layers !== undefined) {
            console.debug("Has layers:", layers);
            //    If we're a source, turn off all the layers
            layers
                .forEach((layer) => {
                    const property = this.map.getLayoutProperty(layer, "visibility");
                    if (setVisible) {
                        this.map.setLayoutProperty(layer, "visibility", "visible");
                    } else {
                        this.map.setLayoutProperty(layer, "visibility", "none");
                    }
                });
        }

    //    If not, figure out which layers have the individual

    }

    public clearMap(): void {
        console.debug("Clearing map");
        this.mapSources.forEach((_, source) => {
            console.debug("removing:", source);
            this.removeSource(source);
        });
    }

    private removeSource(source: MapSource | string): void {
        let sourceID = null;
        if (typeof source === "string") {
            sourceID = source;
        } else {
            sourceID = source.id;
        }

        // Remove all the layers for each source
        const layers = this.mapSources.get(sourceID);
        if (layers !== undefined) {
            layers
                .forEach((layer) => {
                    this.map.removeLayer(layer);
                });
        }

        this.map.removeSource(sourceID);
        this.mapSources.delete(sourceID);
    }

    private addSource(inputLayer: MapSource): void {
        console.debug("Adding source data:", inputLayer.data);

        // Merge the new source with the default layers

        this.map.addSource(inputLayer.id, {
            type: "geojson",
            data: inputLayer.data
        });

        // If it's a 3D layer, add the extrusion, otherwise add the normal layers
        if (TrestleMapComponent.is3D(inputLayer)) {
            console.debug("Adding 3D layer:", inputLayer.extrude);
            this.map.addLayer(inputLayer.extrude);
            this.mapSources.set(inputLayer.id, [inputLayer.extrude.id]);
        } else {
            // Add fill layer
            const fillID = inputLayer.id + "-fill";
            this.map.addLayer({
                id: fillID,
                type: "fill",
                source: inputLayer.id,
                paint: {
                    "fill-color": "#627BC1",
                    "fill-opacity": 0.7,
                }
            });
            // Add polygon line changes
            const lineId = inputLayer.id + "-line";
            this.map.addLayer({
                id: lineId,
                type: "line",
                source: inputLayer.id,
                paint: {
                    "line-color": "white",
                    "line-width": 2
                }
            });
            // Add hover layer
            const hoverID = inputLayer.id + "-hover";
            this.map.addLayer({
                id: hoverID,
                type: "fill",
                source: inputLayer.id,
                paint: {
                    "fill-color": "#627BC1",
                    "fill-opacity": 1,
                    // Repaint the lines so that they're still visible
                },
                filter: ["==", "name", ""]
            });
            this.mapSources.set(inputLayer.id, [fillID, lineId, hoverID]);
        }

        //    Center map
        if (this.centerMapOnLoad.getValue()) {
            this.centerMap(inputLayer.data);
        }
    }

    private layerClick = (e: MapMouseEvent): void => {
        console.debug("Clicked:", e);
        // FIXME(nrobison): Get rid of this type cast.
        // Get all the fill fillLayers
        let fillLayers: string[] = [];
        this.mapSources.forEach((values) => {
            fillLayers = fillLayers
                .concat((values
                    .filter((val) => val.includes("-fill"))));
        });
        console.debug("Querying on fillLayers:", fillLayers);
        const features: any[] = this.map.queryRenderedFeatures(e.point, {
            layers: fillLayers
        });
        // Set the hover filter using either the provided id field, or a default property
        const idField = this.individual.idField == null ? "id" : this.individual.idField;
        console.debug("Accessing ID field:", idField);

        // If we don't filter on anything, deselect it all
        if (!this.multiSelect && !(features.length > 0)) {
            let hoverLayers: string[] = [];
            this.mapSources.forEach((layers) => {
                hoverLayers = hoverLayers
                    .concat(layers
                        .filter((val) => val.includes("-hover")));
            });
            console.debug("Deselecting", hoverLayers);
            hoverLayers.forEach((layer) => {
                this.map.setFilter(layer, ["==", idField, ""]);
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
        console.debug("Filtering on layer:", layerID + "-hover");
        this.map.setFilter(layerID + "-hover", ["==", idField, featureID]);
        // If multi-select is not enabled, deselect everything else
        if (!this.multiSelect) {
            let hoverLayers: string[] = [];
            this.mapSources.forEach((values) => {
                hoverLayers = hoverLayers
                    .concat(values
                        .filter((val) => val.includes("-hover")));
            });
            console.debug("Deselecting:", hoverLayers);
            // Add hover back to the layerID, otherwise nothing will match
            layerID = layerID + "-hover";
            hoverLayers
                .forEach((layer) => {
                    if (layer !== layerID) {
                        this.map.setFilter(layer, ["==", idField, ""]);
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

    private centerMap(geom: FeatureCollection<GeometryObject>): void {
        // We have to lock the map in order to avoid sending out a notice that the move happened.
        if (geom.bbox) {
            // FIXME(nrobison): This is garbage. Fix it.
            this.map.fitBounds(LngLatBounds.convert(geom.bbox as any));
        } else {
            const bbox = extent(geom);
            console.debug("Extent", bbox);
            if (bbox) {
                // This works, but it seems to confuse the type system, so any for the win!
                this.map.fitBounds(LngLatBounds.convert(bbox as any));
            }
        }
    }

    private setupDefaults(): void {
        this.baseConfig = {
            container: "map",
            style: "mapbox://styles/mapbox/light-v9",
            center: new mapboxgl.LngLat(32.3558991, -25.6854313),
            zoom: 8
        };
    }

    private lockMap(): void {
        this.map.dragPan.disable();
        this.map.dragRotate.disable();
        this.map.scrollZoom.disable();
        this.map.keyboard.disable();
        this.map.boxZoom.disable();
        this.map.doubleClickZoom.disable();
        this.map.touchZoomRotate.disable();
    }

    private unlockMap(): void {
        this.map.dragPan.enable();
        this.map.dragRotate.enable();
        this.map.scrollZoom.enable();
        this.map.keyboard.enable();
        this.map.boxZoom.enable();
        this.map.doubleClickZoom.enable();
        this.map.touchZoomRotate.enable();
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
                throw new Error("Unable to get coordinates for object of type: " + geom.type);
        }
    }

    private static is3D(x: any): x is I3DMapSource {
        return (x as I3DMapSource).extrude !== undefined;
    }
}
