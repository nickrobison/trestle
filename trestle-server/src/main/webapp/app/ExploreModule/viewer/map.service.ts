/**
 * Created by nrobison on 6/23/17.
 */
import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Response } from "@angular/http";
import { Observable } from "rxjs/Observable";
import { LngLatBounds } from "mapbox-gl";
import { Polygon } from "geojson";

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

    public tsIntersect(dataset: string, bounds: LngLatBounds, validTime: Date, dbTime?: Date): Observable<any> {
        console.debug("Intersecting at:", bounds, validTime);
        return this.http.post("/visualize/intersect", {
            dataset: dataset,
            validAt: validTime,
            databaseAt: Date.now(),
            bbox: MapService.boundsToGeoJSON(bounds)
        })
            .map((res: Response) => {
            console.debug("Intersected objects", res.json());
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    private static boundsToGeoJSON(bounds: LngLatBounds): Polygon {
        return {
            type: "Polygon",
            // need to return and array of bounds as an array of SW -> NW -> NE -> SE -> SW
            coordinates: [[bounds.getSouthWest().toArray(),
                bounds.getNorthWest().toArray(),
                bounds.getNorthEast().toArray(),
                bounds.getSouthEast().toArray(),
                bounds.getSouthWest().toArray()]],
            crs: {type: "name", properties: {name: "EPSG:4326"}}
        };
    }
}
