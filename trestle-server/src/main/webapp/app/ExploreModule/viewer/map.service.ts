/**
 * Created by nrobison on 6/23/17.
 */
import {Injectable} from "@angular/core";
import {TrestleHttp} from "../../UserModule/trestle-http.provider";
import {Response} from "@angular/http";
import {Observable} from "rxjs/Observable";
import {LngLatBounds} from "mapbox-gl";
import {Feature, FeatureCollection, GeometryObject, MultiPolygon, Polygon} from "geojson";
import {Moment} from "moment";
import {
    ITrestleIndividual,
    TrestleIndividual
} from "../../SharedModule/individual/TrestleIndividual/trestle-individual";

var parse = require("wellknown");

type wktType = "POINT" |
    "MULTIPOINT" |
    "LINESTRING" |
    "MULTILINESTRING" |
    "POLYGON" |
    "MULTIPOLYGON";

export type wktValue = LngLatBounds | GeometryObject;

interface IIntersectionBody {
    dataset: string;
    geojson: Polygon | MultiPolygon;
    buffer: number;
    validAt?: string;
    databaseAt?: string;
}

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
                       wkt: wktValue,
                       validTime: Moment,
                       dbTime?: Moment,
                       buffer: number = 0): Observable<FeatureCollection<GeometryObject>> {
        console.debug("Intersecting at:", wkt, validTime.toISOString());

        const postBody: IIntersectionBody = {
            dataset,
            validAt: validTime.toISOString(),
            databaseAt: new Date().toISOString(),
            geojson: MapService.normalizeToGeoJSON(wkt),
            buffer
        };
        console.debug("Post body", postBody);
        return this.http.post("/visualize/intersect", postBody)
            .map(MapService.parseObjectToGeoJSON)
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public stIntersectIndividual(dataset: string,
                                 wkt: wktValue,
                                 validTime?: Moment,
                                 dbTime?: Moment,
                                 buffer: number = 0): Observable<TrestleIndividual[]> {
        const postBody: IIntersectionBody = {
            dataset,
            buffer,
            geojson: MapService.normalizeToGeoJSON(wkt)
        };

        if (validTime) {
            postBody.validAt = validTime.toISOString();
        }

        if (dbTime) {
            postBody.databaseAt = dbTime.toISOString();
        }

        console.debug("Intersecting individuals with", postBody);

        return this.http.post("/visualize/intersect-individuals", postBody)
            .map(MapService.parseResponseToIndividuals);
    }

    private static parseResponseToIndividuals(res: Response): TrestleIndividual[] {
        const json = res.json();
        console.debug("Intersected result from server:", json);
        return json
            .map((individual: ITrestleIndividual) => new TrestleIndividual(individual));
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
                    if (MapService.isID(key)) {
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

    private static normalizeToGeoJSON(geom: wktValue): Polygon | MultiPolygon {
        if (MapService.isGeometryObject(geom)) {
            if (geom.type === "Polygon") {
                return (geom as Polygon);
            } else if (geom.type === "MultiPolygon") {
                return (geom as MultiPolygon);
            } else {
                console.error("Not correct geom", geom);
                throw new Error("Not correct geometry");
            }
        }
        return {
            type: "Polygon",
            // need to return and array of bounds as an array of SW -> NW -> NE -> SE -> SW
            coordinates: [[geom.getSouthWest().toArray(),
                geom.getNorthWest().toArray(),
                geom.getNorthEast().toArray(),
                geom.getSouthEast().toArray(),
                geom.getSouthWest().toArray()]]
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
        if (x.toLowerCase() === "id") {
            return true;
        }
        if (x.length >= 2) {
            const sub = x.substring(x.length - 3, x.length - 1);
            return sub.toLowerCase() === "id";
        }
        return false;
    }

    private static isGeometryObject(x: any): x is GeometryObject {
        return (x as GeometryObject).type !== undefined;
    }
}
