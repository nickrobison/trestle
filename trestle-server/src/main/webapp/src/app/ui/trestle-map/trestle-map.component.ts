/**
 * Created by nrobison on 6/11/17.
 */
import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChange} from "@angular/core";
import {Feature, FeatureCollection, GeometryObject} from "geojson";
import {BehaviorSubject, Subject} from "rxjs";
import {TrestleIndividual} from "../../shared/individual/TrestleIndividual/trestle-individual";
import extent from "@mapbox/geojson-extent";
import {
  FillLayer,
  FillPaint,
  GeoJSONSource,
  GeoJSONSourceRaw,
  ImageSource,
  LineLayer,
  LngLatBounds,
  Map as MapboxMap,
  MapboxOptions,
  MapMouseEvent,
  RasterSource,
  SymbolLayer,
  VectorSource,
  VideoSource
} from "mapbox-gl";

export interface IMapFillLayer extends FillLayer {
  type: 'fill';
}

export interface IMapLineLayer extends LineLayer {
  type: 'line';
}

export interface IMapHoverLayer extends FillLayer {
  type: 'fill';
  filter: ['==', 'name', ''];
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
  labelField?: string;
  labelFunction?: (label: string) => string;
  labelValue?: string;
}

export interface I3DMapSource extends ITrestleMapSource {
  extrude: SymbolLayer;
}

interface GeoJSONDataSource extends GeoJSONSource {
  _data: Feature<GeometryObject> | FeatureCollection<GeometryObject>;
}

export interface IMapAttributeChange {
  individual: string;
  changes: Array<{ attribute: string, value: any }>;
  // attribute: string;
  // value: any;
}

export interface IMapEventHandler {
  event: MapEvent;
  handler: (event: any) => void;
}

export type MapEvent = 'mousemove' | 'mouseleave' | 'click' | 'moveend';
export type MapSource = I3DMapSource | ITrestleMapSource;
export type MapBoxSource = GeoJSONSource | VectorSource | RasterSource | ImageSource | VideoSource | GeoJSONSourceRaw;

@Component({
  selector: 'trestle-map',
  templateUrl: './trestle-map.component.html',
  styleUrls: ['./trestle-map.component.scss']
})

export class TrestleMapComponent implements OnInit, OnChanges {

  @Input() public data: MapSource;
  @Input() public single: boolean;
  @Input() public multiSelect: boolean;
  @Input() public clickLayerSuffix: string;
  @Input() public zoomOnLoad?: boolean;
  @Input() public config?: MapboxOptions;
  @Input() public dataChanges: Subject<MapSource | undefined>;
  @Input() public attributeChanges: Subject<IMapAttributeChange>;
  @Input() public handlers: IMapEventHandler[];
  @Output() public mapBounds: EventEmitter<LngLatBounds> = new EventEmitter();
  @Output() public clicked: EventEmitter<string> = new EventEmitter();
  private centerMapOnLoad: BehaviorSubject<boolean>;
  private baseConfig: MapboxOptions;
  private map: MapboxMap;
  private mapSources: Map<string, string[]>;
  // This has to be integers, in order to match against the numeric IDs
  private filteredIDs: string[];
  private previousValue: MapSource;

  constructor() {

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

    console.debug('Creating map, ' +
      'singleSelect?', this.single,
      'mulitSelect?', this.multiSelect,
      'zoom?', this.centerMapOnLoad.getValue());

    // Merge the map configs together
    const mergedConfig = Object.assign(this.baseConfig, this.config);
    this.map = new MapboxMap(mergedConfig);

    this.map.on('click', this.layerClick);
    this.map.on('mouseover', this.mouseOver);
    this.map.on('mouseleave', this.mouseOut);
    this.map.on('moveend', this.moveHandler);

    // Register any additional handlers
    if (this.handlers) {
      this.handlers.forEach((h) => {
        this.map.on(h.event, h.handler);
      });
    }

    // Once the map is loaded, setup the subscriptions
    this.map.on('style.load', () => {
      // If it's null, create a dummy one
      if (this.dataChanges === undefined) {
        console.debug('Creating dummy data changes subscription');
        this.dataChanges = new Subject();
      }
      console.debug('Subscribing to data changes observable');
      this.dataChanges.subscribe((data) => {
        console.debug('Map has new data to load', data);
        if (data !== undefined) {
          if (this.single && this.previousValue) {
            this.removeSource(this.previousValue);
          }
          this.addSource(data);
          this.previousValue = data;
        }
      });

      if (this.attributeChanges === undefined) {
        console.debug('Creating dummy attribute subscription');
        this.attributeChanges = new Subject();
      }
      console.debug('Subscribing to attribute changes observable');
      this.attributeChanges.subscribe((change) => {
        this.changeIndividualAttribute(change);
      });
      this.mapBounds.emit(this.map.getBounds());
    });
  }

  public ngOnChanges(changes: { [propKey: string]: SimpleChange }): void {
    // Individual changes
    const inputChanges = changes['data'];
    if (inputChanges != null
      && !inputChanges.isFirstChange()
      && (inputChanges.currentValue !== inputChanges.previousValue)) {
      console.debug('New change, updating', inputChanges);
      if (inputChanges.previousValue != null && this.single) {
        // mapChanges.previousValue= inputChanges.previousValue;
        this.removeSource(inputChanges.previousValue);
      }
      // this.dataChanges.next(mapChanges);
      this.addSource(inputChanges.currentValue);
    }

    //    Zoom On Load changes
    const zoomChanges = changes['zoomOnLoad'];
    if (zoomChanges != null
      && !zoomChanges.isFirstChange()) {
      console.debug('Changing zoom value');
      this.centerMapOnLoad.next(zoomChanges.currentValue);
    }

    // Event handlers
    // const handlerChanges = changes["handlers"];
    // if (handlerChanges != null
    //     && (handlerChanges.currentValue !== handlerChanges.previousValue)) {
    //     console.debug("Registering event handlers");
    //     (handlerChanges.currentValue as IMapEventHandler[]).forEach((e) => {
    //         this.map.on(e.event, e.handler);
    //     });
    // }
  }

  /**
   * Get the current map bounds
   * @returns {mapboxgl.LngLatBounds}
   */
  public getMapBounds(): LngLatBounds {
    return this.map.getBounds();
  }

  /**
   * Remove individual from the map, which clears the source and linked layers
   * @param {string} individual
   */
  public removeIndividual(individual: string): void {
    console.debug('Removing selection %s from the map', individual);

    // Is the data a source?
    if (this.mapSources.has(individual)) {
      this.removeSource(individual);
    } else {
      //    Otherwise find the matching layer and remove it
    }
  }

  /**
   * Modify the specified attribute for the given individual
   * @param {IMapAttributeChange} attributeChange
   */
  public changeIndividualAttribute(attributeChange: IMapAttributeChange): void {
    console.debug('Changing attribute:', attributeChange);

    //    Try to get the source first
    const layers = this.mapSources.get(attributeChange.individual);
    if (layers !== undefined) {
      console.debug('Changing layers:', layers);
      layers.filter((layer) => !layer.startsWith('label'))
        .forEach((layer) => {
          attributeChange.changes.forEach((change) => {
            this.map.setPaintProperty(layer, change.attribute, change.value);
          });

        });
    }
    //    I don't think we can do this with individuals yet, but maybe?
  }

  /**
   * Toggle the visibility of the layers for the given individual
   * This changes all registered layers for the given source
   * @param {string} individual
   * @param {boolean} setVisible
   */
  public toggleIndividualVisibility(individual: string, setVisible: boolean): void {
    console.debug('setting visible?', setVisible);
    //    See if the data is a source
    const layers = this.mapSources.get(individual);
    if (layers !== undefined) {
      console.debug('Has layers:', layers);
      //    If we're a source, turn off all the layers
      layers
        .forEach((layer) => {
          if (setVisible) {
            this.map.setLayoutProperty(layer, 'visibility', 'visible');
          } else {
            this.map.setLayoutProperty(layer, 'visibility', 'none');
          }
        });
      //    If not, figure out which layers have the data
    } else {
      console.debug('Looking for matching individual id:',
        TrestleMapComponent.buildFilterID(individual));
      for (const source of Array.from(this.mapSources.keys())) {
        const mapSource = this.map.getSource(source);
        if (TrestleMapComponent.isGeoJSON(mapSource)) {
          console.debug('Checking source:', mapSource);
          console.debug('Has data:', (mapSource as any)._data);
          const data = mapSource._data;
          // If it's a feature collection, dive into it
          if (TrestleMapComponent.isCollection(data)) {
            for (const feature of data.features) {
              // TODO(nickrobison): This will fail if the features don't have an ID property
              if ((feature.properties as any).id === TrestleMapComponent
                .buildFilterID(individual)) {
                console.debug('Source %s matches individual %s',
                  source, individual);
                this.toggleSourceVisibility(source, setVisible, individual);
                break;
              }
            }
          } else {
            // TODO(nickrobison): This will fail if the features don't have an ID property
            if ((data.properties as any).id === TrestleMapComponent
              .buildFilterID(individual)) {
              console.debug('Source feature %s matches individual %s',
                source, individual);
              this.toggleSourceVisibility(source, setVisible, individual);
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Clear all the layers and sources from the map
   */
  public clearMap(): void {
    console.debug('Clearing map');
    this.mapSources.forEach((_, source) => {
      console.debug('removing:', source);
      this.removeSource(source);
    });
  }

  /**
   * Center the map on the given input set, computes the bounding box if one doesn't exist
   * @param {FeatureCollection<GeometryObject> | Feature<GeometryObject>} geom
   */
  public centerMap(geom: FeatureCollection<GeometryObject> | Feature<GeometryObject>): void {
    // We have to lock the map in order to avoid sending out a notice that the move happened.
    if (geom.bbox) {
      // FIXME(nrobison): This is garbage. Fix it.
      this.map.fitBounds(LngLatBounds.convert(geom.bbox as any));
    } else {
      const bbox = extent(geom);
      if (bbox) {
        // This works, but it seems to confuse the type system, so any for the win!
        this.map.fitBounds(LngLatBounds.convert(bbox as any));
      }
    }
  }

  /**
   * Change the map base layer
   * WARNING: This will cause all the sources and layers to be removed and added again, which is slow
   * @param {string} style - New base layer URL
   */
  public setMapStyle(style: string): void {
    this.clearMap();
    this.map.setStyle(style);
  }

  /**
   * Update the map view angle
   * @param {number} pitch
   * @param {number} bearing
   */
  public setPitchBearing(pitch?: number, bearing?: number): void {
    if (pitch) {
      this.map.setPitch(pitch);
    }

    if (bearing) {
      this.map.setBearing(bearing);
    }
  }

  /**
   * Change the 3D offset of the individual, or all sources that are currently at the input height
   * @param {number} height
   * @param {number} offset
   * @param {string} individual
   */
  public change3DOffset(height: number, offset: number, individual?: string): void {

    if (individual) {
      this.mapSources.forEach((layers, key) => {
        // If we have the individual's source, change its layers
        if (key === individual) {
          layers.forEach((layer) => {
            // You can't get properties that don't exist on layers,
            // that's an error not a null, because of course
            // But I'm sure it'll still return a null
            if (!layer.startsWith('label')) {
              const layerHeight = this.map.getPaintProperty(layer,
                'fill-extrusion-height');
              if (layerHeight) {
                this.map.setPaintProperty(layer,
                  'fill-extrusion-height',
                  layerHeight + offset);
                const layerBase = this.map.getPaintProperty(layer,
                  'fill-extrusion-base');
                if (layerBase) {
                  this.map.setPaintProperty(layer,
                    'fill-extrusion-base',
                    layerBase + offset);
                }
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
            'fill-extrusion-height');
          // If it matches the height of the layer, increase it
          if (layerHeight === height) {
            console.debug('Changing individuals');
            const layerBase = this.map.getPaintProperty(layer,
              'fill-extrusion-base');
            if (layerBase) {
              this.map.setPaintProperty(layer,
                'fill-extrusion-base',
                layerBase + offset);
            }
            this.map.setPaintProperty(layer,
              'fill-extrusion-height',
              layerHeight + offset);
          }
        });
      });
    }

  }

  private toggleSourceVisibility(source: string, setVisible: boolean, individual?: string): void {
    const layers = this.mapSources.get(source);
    if (layers !== undefined) {
      console.debug('Has layers:', layers);
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
              console.debug('Removing individual %s from layer %s',
                individual, layer);
              this.filteredIDs.push(filteredID);
            }
            // If we have items to filter, add them,
            // otherwise remove the filter
            if (this.filteredIDs.length > 0) {
              // TODO(nickrobison): This will fail if the features don't have an ID property
              const filterValues = ['!in', 'id'].concat(this.filteredIDs);

              console.debug('Filtered Features:', this.map.querySourceFeatures(source,
                {
                  sourceLayer: layer,
                  filter: filterValues
                }));
              console.debug('Setting filter of %O on layer:', filterValues, layer);
              this.map.setFilter(layer, filterValues);
            } else {
              console.debug('Removing filter from layer:', layer);
              (this.map as any).setFilter(layer, null);
            }
          } else {
            if (setVisible) {
              this.map.setLayoutProperty(layer, 'visibility', 'visible');
            } else {
              this.map.setLayoutProperty(layer, 'visibility', 'none');
            }
          }
        });
    }

  }

  private removeSource(source: MapSource | string): void {
    let sourceID;
    if (typeof source === 'string') {
      sourceID = source;
    } else {
      sourceID = source.id;
    }

    if (this.mapSources.has(sourceID)) {
      console.debug('Removing source %s from map', sourceID);
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
  }

  private addSource(inputLayer: MapSource): void {
    console.debug('Adding source data:', inputLayer.data);

    // Merge the new source with the default layers
    // But only if we don't already have that layer
    if (this.mapSources.has(inputLayer.id)) {
      console.debug('Map already has source:', inputLayer.id);
      return;
    }

    this.map.addSource(inputLayer.id, {
      type: 'geojson',
      data: inputLayer.data
    });

    const attributeLayers: string[] = [];

    // If it's a 3D layer, add the extrusion, otherwise add the normal layers
    if (TrestleMapComponent.is3D(inputLayer)) {
      console.debug('Adding 3D layer:', inputLayer.extrude);
      this.map.addLayer(inputLayer.extrude);
      attributeLayers.push(inputLayer.extrude.id);
    } else {
      // Add fill layer
      const fillID = inputLayer.id + '-fill';
      this.map.addLayer({
        id: fillID,
        type: 'fill',
        source: inputLayer.id,
        paint: ({
          'fill-color': '#627BC1',
          'fill-opacity': 0.7,
        } as FillPaint)
      });
      // Add polygon line changes
      const lineId = inputLayer.id + '-line';
      this.map.addLayer({
        id: lineId,
        type: 'line',
        source: inputLayer.id,
        paint: {
          'line-color': 'white',
          'line-width': 2
        }
      });
      // Add hover layer
      const hoverID = inputLayer.id + '-hover';
      this.map.addLayer({
        id: hoverID,
        type: 'fill',
        source: inputLayer.id,
        paint: ({
          'fill-color': '#627BC1',
          'fill-opacity': 1,
          // Repaint the lines so that they're still visible
        } as FillPaint),
        filter: ['==', 'name', '']
      });
      attributeLayers.push(fillID, lineId, hoverID);
    }

    // Labels
    const labelField = inputLayer.labelField;
    const labelValue = inputLayer.labelValue;
    if (labelField || labelValue) {
      // If it's a collection for each entity, add the label
      const iData = inputLayer.data;
      if (TrestleMapComponent.isCollection(iData)) {
        // Collections only support field labels, since we need to know how to filter each one
        if (labelField == null) {
          throw new Error('Geometry collections only support label Fields, not values');
        }
        iData.features.forEach((feature) => {
          const labelLayerID = 'label-' + feature.id;
          // This is terrible, but so is the web, so who blinks first?
          // If we have a field, use it, otherwise, use the provided value
          const labelText: string = labelField ? (feature.properties as any)[(labelField as any)] : labelValue;
          this.map.addLayer({
            id: labelLayerID,
            type: 'symbol',
            source: inputLayer.id,
            layout: {
              // For the text field, if the label function exists, call it, otherwise just place the label
              'text-field': inputLayer.labelFunction ? inputLayer.labelFunction(labelText) : labelText,
              'text-font': ['Open Sans Bold', 'Arial Unicode MS Bold'],
              'text-size': 11,
              'text-transform': 'uppercase',
              'text-letter-spacing': 0.05,
              'text-offset': [0, 1.5],
              'text-allow-overlap': true
              // "text-ignore-placement": true
            },
            paint: {
              'text-color': '#202',
              'text-halo-color': '#fff',
              'text-halo-width': 2
            },
            filter: ['==', labelField, labelText]
          });
          attributeLayers.push(labelLayerID);
        });
      } else {
        const labelLayerID = 'label-' + iData.id;
        // This is terrible, but so is the web, so who blinks first?
        const labelText: string = labelField ? (iData.properties as any)[(labelField as any)] : labelValue;
        this.map.addLayer({
          id: labelLayerID,
          type: 'symbol',
          source: inputLayer.id,
          layout: {
            // For the text field, if the label function exists, call it, otherwise just place the label
            'text-field': inputLayer.labelFunction ? inputLayer.labelFunction(labelText) : labelText,
            'text-font': ['Open Sans Bold', 'Arial Unicode MS Bold'],
            'text-size': 11,
            'text-transform': 'uppercase',
            'text-letter-spacing': 0.05,
            'text-offset': [0, 1.5]
          },
          paint: {
            'text-color': '#202',
            'text-halo-color': '#fff',
            'text-halo-width': 2
          }
        });
        attributeLayers.push(labelLayerID);
      }
    }
    // Add the map sources
    this.mapSources.set(inputLayer.id, attributeLayers);

    //    Center map
    if (this.centerMapOnLoad.getValue()) {
      this.centerMap(inputLayer.data);
    }
  }

  private layerClick = (e: MapMouseEvent): void => {
    console.debug('Clicked:', e);
    // FIXME(nrobison): Get rid of this type cast.
    // Get all the fill fillLayers
    let fillLayers: string[] = [];
    this.mapSources.forEach((values) => {
      fillLayers = fillLayers
        .concat((values
          // If we have a clickLayerSuffix, filter on that, otherwise, just find the fill ¬layers
          .filter((val) => val.includes(this.clickLayerSuffix ? this.clickLayerSuffix : '-fill'))));
    });
    console.debug('Querying on fillLayers:', fillLayers);
    const features: any[] = this.map.queryRenderedFeatures(e.point, {
      layers: fillLayers
    });
    // Set the hover filter using either the provided id field, or a default property
    console.debug('Filtering with data:', this.data);
    // const idField = this.data.idField === undefined ? "id" : this.data.idField;
    const idField = 'id';
    console.debug('Accessing ID field:', idField);

    // If we don't filter on anything, deselect it all
    if (!this.multiSelect && !(features.length > 0)) {
      let hoverLayers: string[] = [];
      this.mapSources.forEach((layers) => {
        hoverLayers = hoverLayers
          .concat(layers
            .filter((val) => val.includes('-hover')));
      });
      console.debug('Deselecting', hoverLayers);
      hoverLayers.forEach((layer) => {
        this.map.setFilter(layer, ['==', idField, '']);
      });
      return;
    }
    console.debug('Filtered features', features);

    const feature: any = features[0];
    let layerID = features[0].layer.id;
    // Emit the clicked layer
    const featureID = feature.properties[idField];
    this.clicked.emit(featureID);
    layerID = layerID.replace('-fill', '');
    console.debug('Filtering on layer:', layerID + '-hover');
    this.map.setFilter(layerID + '-hover', ['==', idField, featureID]);
    // If multi-select is not enabled, deselect everything else
    if (!this.multiSelect) {
      let hoverLayers: string[] = [];
      this.mapSources.forEach((values) => {
        hoverLayers = hoverLayers
          .concat(values
            .filter((val) => val.includes('-hover')));
      });
      console.debug('Deselecting:', hoverLayers);
      // Add hover back to the layerID, otherwise nothing will match
      layerID = layerID + '-hover';
      hoverLayers
        .forEach((layer) => {
          if (layer !== layerID) {
            this.map.setFilter(layer, ['==', idField, '']);
          }
        });
    }
  };


  private mouseOver = (e: MapMouseEvent): void => {
    console.debug('Moused over:', e);
  };

  private mouseOut = (e: MapMouseEvent): void => {
    console.debug('Mouse out:', e);
  };

  private moveHandler = () => {
    this.mapBounds.emit(this.map.getBounds());
  };

  private setupDefaults(): void {
    this.baseConfig = {
      container: 'map',
      style: 'mapbox://styles/mapbox/light-v9',
      center: {lng: 32.3558991, lat: -25.6854313},
      zoom: 8,
      accessToken: 'pk.eyJ1IjoibnJvYmlzb24iLCJhIjoiY2ozdDd5dmd2MDA3bTMxcW1kdHZrZ3ppMCJ9.YcJMRphQAfmZ0H8X9HnoKA'
    };
  }

  private static is3D(x: any): x is I3DMapSource {
    return (x as I3DMapSource).extrude !== undefined;
  }

  private static isGeoJSON(x: any): x is GeoJSONDataSource {
    return (x as GeoJSONSource).type === 'geojson';
  }

  private static isCollection(x: any): x is FeatureCollection<GeometryObject> {
    return (x as FeatureCollection<GeometryObject>).type === 'FeatureCollection';
  }

  private static buildFilterID(individual: string): string {
    console.debug('Filtering:', individual);
    return TrestleIndividual.filterID(individual)
      .replace(/-/g, ' ')
      .replace(':', '-');
  }
}
