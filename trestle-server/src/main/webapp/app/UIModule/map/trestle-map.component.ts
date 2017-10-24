/**
 * Created by nrobison on 6/11/17.
 */
import * as mapboxgl from "mapbox-gl";
import {LngLatBounds, MapMouseEvent, VectorSource, GeoJSONSource, GeoJSONSourceOptions} from "mapbox-gl";
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
    Polygon,
    Feature
} from "geojson";
import {BehaviorSubject} from "rxjs/BehaviorSubject";
import {TrestleIndividual} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import {off} from "codemirror";

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
    data: FeatureCollection<GeometryObject> | Feature<GeometryObject>;
    layers?: ITrestleMapLayers;
}

export interface I3DMapSource extends ITrestleMapSource {
    extrude: mapboxgl.Layer;
}

interface GeoJSONDataSource extends GeoJSONSource {
    _data: Feature<GeometryObject> | FeatureCollection<GeometryObject>;
}

export type MapSource = I3DMapSource | ITrestleMapSource;

@Component({
    selector: "trestle-map",
    templateUrl: "./trestle-map.component.html",
    styleUrls: ["./trestle-map.component.css"]
})

export class TrestleMapComponent implements OnInit, OnChanges {

    @Input() public data: MapSource;
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
    // This has to be integers, in order to match against the numeric IDs
    private filteredIDs: string[];

    constructor() {
        // FIXME(nrobison): Fix this
        (mapboxgl as any).accessToken = "pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA";

        this.mapSources = new Map();
        this.filteredIDs = [];

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
        const inputChanges = changes["data"];
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

        // Is the data a source?
        if (this.mapSources.has(individual)) {
            this.removeSource(individual);
        } else {
            //    Otherwise find the matching layer and remove it
        }
    }

    public toggleIndividualVisibility(individual: string, setVisible: boolean): void {
        console.debug("setting visible?", setVisible);
        //    See if the data is a source
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
            //    If not, figure out which layers have the data
        } else {
            console.debug("Looking for matching individual id:",
                TrestleMapComponent.buildFilterID(individual));
            for (const source of Array.from(this.mapSources.keys())) {
                const mapSource = this.map.getSource(source);
                if (TrestleMapComponent.isGeoJSON(mapSource)) {
                    console.debug("Checking source:", mapSource);
                    console.debug("Has data:", (mapSource as any)._data);
                    const data = mapSource._data;
                    // If it's a feature collection, dive into it
                    if (TrestleMapComponent.isCollection(data)) {
                        for (const feature of data.features) {
                            // TODO(nickrobison): This will fail if the features don't have an ID property
                            if ((feature.properties as any).id === TrestleMapComponent
                                    .buildFilterID(individual)) {
                                console.debug("Source %s matches individual %s",
                                    source, individual);
                                this.toggleSourceVisibility(source, setVisible, individual);
                                break;
                            }
                        }
                    } else {
                        // TODO(nickrobison): This will fail if the features don't have an ID property
                        if ((data.properties as any).id === TrestleMapComponent
                                .buildFilterID(individual)) {
                            console.debug("Source feature %s matches individual %s",
                                source, individual);
                            this.toggleSourceVisibility(source, setVisible, individual);
                            break;
                        }
                    }
                }
            }
        }
    }

    public clearMap(): void {
        console.debug("Clearing map");
        this.mapSources.forEach((_, source) => {
            console.debug("removing:", source);
            this.removeSource(source);
        });
    }

    public change3DOffset(height: number, offset: number, individual?: string): void {

        if (individual) {
            this.mapSources.forEach((layers, key) => {
                // If we have the individual's source, change its layers
                if (key === individual) {
                    layers.forEach((layer) => {
                        const layerHeight = this.map.getPaintProperty(layer,
                            "fill-extrusion-height");
                        if (layerHeight) {
                            this.map.setPaintProperty(layer,
                                "fill-extrusion-height",
                                layerHeight + offset);
                            const layerBase = this.map.getPaintProperty(layer,
                                "fill-extrusion-base");
                            if (layerBase) {
                                this.map.setPaintProperty(layer,
                                    "fill-extrusion-base",
                                    layerBase + offset);
                            }
                        }

                    });
                }
            });
        } else {
            //    Find all the individuals that have the same property
            //    For each layer, get its height
            this.mapSources.forEach((layers) => {
                layers.forEach((layer) => {
                    const layerHeight = this.map.getPaintProperty(layer,
                        "fill-extrusion-height");
                    // If it matches the height of the layer, increase it
                    if (layerHeight === height) {
                        console.debug("Changing individuals");
                        const layerBase = this.map.getPaintProperty(layer,
                            "fill-extrusion-base");
                        if (layerBase) {
                            this.map.setPaintProperty(layer,
                                "fill-extrusion-base",
                                layerBase + offset);
                        }
                        this.map.setPaintProperty(layer,
                            "fill-extrusion-height",
                            layerHeight + offset);
                    }
                });
            });
        }

    }

    private toggleSourceVisibility(source: string, setVisible: boolean, individual?: string): void {
        const layers = this.mapSources.get(source);
        if (layers !== undefined) {
            console.debug("Has layers:", layers);
            //    If we're a source, turn off all the layers
            layers
                .forEach((layer) => {
                    // If we're filtering a layer and not a source,
                    // set a filter to remove the individual
                    if (individual) {
                        // If we're setting the layer visible again,
                        // remove it from the list and update the filter
                        const filteredID = TrestleMapComponent.buildFilterID(individual);
                        if (setVisible) {
                            const idx = this.filteredIDs
                                .indexOf(filteredID);
                            if (idx > -1) {
                                this.filteredIDs.splice(idx, 1);
                            }
                            //   If we're setting the layer invisible,
                            // add the individual to the list of filtered IDs
                        } else {
                            console.debug("Removing individual %s from layer %s",
                                individual, layer);
                            this.filteredIDs.push(filteredID);
                        }
                        // If we have items to filter, add them,
                        // otherwise remove the filter
                        if (this.filteredIDs.length > 0) {
                            // TODO(nickrobison): This will fail if the features don't have an ID property
                            const filterValues = ["!in", "id"].concat(this.filteredIDs);

                            console.debug("Filtered Features:", this.map.querySourceFeatures(source,
                                {
                                    sourceLayer: layer,
                                    filter: filterValues
                                }));
                            console.debug("Setting filter of %O on layer:", filterValues, layer);
                            this.map.setFilter(layer, filterValues);
                        } else {
                            console.debug("Removing filter from layer:", layer);
                            (this.map as any).setFilter(layer, null);
                        }
                    } else {
                        if (setVisible) {
                            this.map.setLayoutProperty(layer, "visibility", "visible");
                        } else {
                            this.map.setLayoutProperty(layer, "visibility", "none");
                        }
                    }
                });
        }

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
        const idField = this.data.idField == null ? "id" : this.data.idField;
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

    private centerMap(geom: FeatureCollection<GeometryObject> | Feature<GeometryObject>): void {
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

    private static isVector(x: any): x is VectorSource {
        return (x as VectorSource).type === "vector";
    }

    private static isGeoJSON(x: any): x is GeoJSONDataSource {
        return (x as GeoJSONSource).type === "geojson";
    }

    private static isCollection(x: any): x is FeatureCollection<GeometryObject> {
        return (x as FeatureCollection<GeometryObject>).type === "FeatureCollection";
    }

    private static buildFilterID(individual: string): string {
        console.debug("Filtering:", individual);
        return TrestleIndividual.filterID(individual)
            .replace(/-/g, " ")
            .replace(":", "-");
    }
}
