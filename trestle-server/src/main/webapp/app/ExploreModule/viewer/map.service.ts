/**
 * Created by nrobison on 6/23/17.
 */
import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Response } from "@angular/http";
import { Observable } from "rxjs/Observable";
import { LngLatBounds } from "mapbox-gl";
import { Feature, FeatureCollection, GeometryObject, Polygon } from "geojson";
var parse = require("wellknown");

type wktType = "MULTIPOLYGON" | "POINT" | "POLYGON";

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
                       validTime: Date,
                       dbTime?: Date): Observable<FeatureCollection<GeometryObject>> {
        console.debug("Intersecting at:", bounds, validTime);

        const postBody = {
            dataset: dataset,
            validAt: validTime,
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
            let geometry: GeometryObject = null;
            let id = "";
            Object.keys(obj).forEach((key: string) => {
                const value: any = obj[key];
                if (MapService.isSpatial(value)) {
                    geometry = parse(value);
                } else if (typeof value === "string") {
                    if (key === "id") {
                        id = value;
                    } else {
                        properties[key] = value;
                    }
                } else if (typeof value === "number" ||
                    typeof value === "boolean") {
                    properties[key] = value;
                }
            });
            features.push({
                type: "Feature",
                id: id,
                geometry: geometry,
                properties: properties
            });
        });

        return {
            type: "FeatureCollection",
            features: features
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

    private static isSpatial(x: any): x is wktType {
        if (typeof x === "string") {
            const matches = x.match(/^([\w\-]+)/);
            if (matches != null &&
                (matches[0] === "MULTIPOLYGON" ||
                matches[0] === "POLYGON" ||
                matches[0] === "POINT")) {
                return true;
            }
        }
        return false;
    }
}
