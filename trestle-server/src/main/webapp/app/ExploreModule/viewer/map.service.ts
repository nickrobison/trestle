/**
 * Created by nrobison on 6/23/17.
 */
import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Response } from "@angular/http";
import { Observable } from "rxjs/Observable";
import { LngLatBounds } from "mapbox-gl";
import { Feature, FeatureCollection, GeometryObject, Polygon } from "geojson";
import { Moment } from "moment";
var parse = require("wellknown");

type wktType = "POINT" |
    "MULTIPOINT" |
    "LINESTRING" |
    "MULTILINESTRING" |
    "POLYGON" |
    "MULTIPOLYGON";

@Injectable()
export class MapService {

    constructor(private http: TrestleHttp) {

    }

    public getAvailableDatasets(): Observable<string[]> {
        return this.http.get("/visualize/datasets")
            .map((res: Response) => {
                console.debug("Available datasets:", res.text());
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public stIntersect(dataset: string,
                       bounds: LngLatBounds,
                       validTime: Moment,
                       dbTime?: Moment): Observable<FeatureCollection<GeometryObject>> {
        console.debug("Intersecting at:", bounds, validTime.toISOString());

        const postBody = {
            dataset,
            validAt: validTime.toISOString(),
            databaseAt: new Date().toISOString(),
            bbox: MapService.boundsToGeoJSON(bounds)
        };
        console.debug("Post body", postBody);
        return this.http.post("/visualize/intersect", postBody)
            .map(MapService.parseObjectToGeoJSON)
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    private static parseObjectToGeoJSON(res: Response): FeatureCollection<GeometryObject> {
        const features: Array<Feature<GeometryObject>> = [];
        const responseObject: object[] = res.json();
        responseObject.forEach((obj: any) => {
            const properties: { [key: string]: {} } = {};
            let geometry: GeometryObject | null = null;
            let id = "";
            Object.keys(obj).forEach((key: string) => {
                const value: any = obj[key];
                if (MapService.isSpatial(value)) {
                    geometry = parse(value);
                } else if (typeof value === "string") {
                    if (MapService.isID(value)) {
                        id = value;
                        properties["id"] = value;
                    } else {
                        properties[key] = value;
                    }
                } else if (typeof value === "number" ||
                    typeof value === "boolean") {
                    properties[key] = value;
                }
            });
            if (geometry) {
                features.push({
                    type: "Feature",
                    id,
                    geometry,
                    properties
                });
            }
        });

        return {
            type: "FeatureCollection",
            features
        };
    }

    private static boundsToGeoJSON(bounds: LngLatBounds): Polygon {
        return {
            type: "Polygon",
            // need to return and array of bounds as an array of SW -> NW -> NE -> SE -> SW
            coordinates: [[bounds.getSouthWest().toArray(),
                bounds.getNorthWest().toArray(),
                bounds.getNorthEast().toArray(),
                bounds.getSouthEast().toArray(),
                bounds.getSouthWest().toArray()]]
            // crs: {type: "name", properties: {name: "EPSG:4326"}}
        };
    }

    /**
     * Crummy regex function to determine if a provided value is a WKT literal
     * @param x - any object at all
     * @returns {boolean} is wkt
     */
    private static isSpatial(x: any): x is wktType {
        if (typeof x === "string") {
            const matches = x.match(/^([\w\-]+)/);
            if (matches != null &&
                (matches[0] === "MULTIPOLYGON" ||
                matches[0] === "POLYGON" ||
                matches[0] === "POINT" ||
                matches[0] === "MULTIPOINT" ||
                matches[0] === "LINESTRING" ||
                matches[0] === "MULTILINESTRING")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a given {string} property name represents the object ID
     * It does so by peaking at the last 2 characters to see if their lowercase representation equals 'id'
     * @param x - Property name
     * @returns {boolean} is id
     */
    private static isID(x: string): boolean {
        if (x.length >= 2) {
            const sub = x.substring(x.length - 3, x.length - 1);
            return sub.toLowerCase() === "id";
        }
        return false;
    }
}
