/**
 * Created by nrobison on 6/23/17.
 */
import { Inject, Injectable, InjectionToken } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Response } from "@angular/http";
import { Observable } from "rxjs/Observable";
import { LngLatBounds } from "mapbox-gl";
import { FeatureCollection, GeometryObject, MultiPolygon, Polygon } from "geojson";
import { Moment } from "moment";
import { ITrestleIndividual, TrestleIndividual } from "../../SharedModule/individual/TrestleIndividual/trestle-individual";
import { Subscriber } from "rxjs/Subscriber";
import { isNullOrUndefined } from "util";
import * as Worker from "worker-loader!./map.worker";
import { CacheService } from "../../SharedModule/cache/cache.service";

export type wktType = "POINT" |
    "MULTIPOINT" |
    "LINESTRING" |
    "MULTILINESTRING" |
    "POLYGON" |
    "MULTIPOLYGON";

export type wktValue = LngLatBounds | GeometryObject;

export interface IContributionReport {
    object: any;
    area: number;
    contributingParts: IContributionPart[];
}

export interface IComparisonReport {
    union: IContributionReport | null;
    reports: ISpatialComparisonReport[];
}

export interface IContributionPart {
    object: any;
    contribution: number;
}

export interface ISpatialComparisonReport {
    objectAID: string;
    objectBID: string;
    relations: string[];
    equality?: number;
    spatialOverlap?: string;
    spatialOverlapPercentage?: number;
}

interface IIntersectionBody {
    dataset: string;
    geojson: Polygon | MultiPolygon;
    buffer: number;
    validAt?: string;
    databaseAt?: string;
}

interface ICompareBody {
    compare: string;
    compareAgainst: string[];
}

export interface IMapWorkerRequest {
    id: number;
    response: object[];
}

export interface IMapWorkerResponse {
    id: number;
    geom: FeatureCollection<GeometryObject>;
}

export const DATASET_CACHE = new InjectionToken<CacheService<string, string[]>>("dataset.cache");

@Injectable()
export class MapService {
    private worker: Worker;
    private workerStream: Observable<IMapWorkerResponse>;

    constructor(private http: TrestleHttp,
                @Inject(DATASET_CACHE) private cache: CacheService<string, string[]>) {
        //    Create the worker and register a stream for the results
        this.worker = new Worker();
        this.workerStream = Observable.fromEvent(this.worker, "message")
            .map((m: MessageEvent) => (m.data as IMapWorkerResponse));
    }

    /**
     * Returns the list of currently registered datasets from the database
     * @returns {Observable<string[]>}
     */
    public getAvailableDatasets(): Observable<string[]> {
        // Try from cache first, then hit the API
        return this.cache.get("datasets", this.dsAPICall());
    }

    public getDatasetFactValues(dataset: string, fact: string, limit: number): Observable<string[]> {
        return this.http.post("/visualize/values", {
            dataset,
            fact,
            limit
        })
            .map((res) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Before a spatio-temporal interesction for the given WKT bounding box, returning a GeoJSON Feature Collection
     * @param {string} dataset to use
     * @param {wktValue} wkt boundary
     * @param {moment.Moment} validTime of intersection
     * @param {moment.Moment} dbTime of intersection
     * @param {number} buffer (in meters) around boundary
     * @returns {Observable<FeatureCollection<GeometryObject>>}
     */
    public stIntersect(dataset: string,
                       wkt: wktValue,
                       validTime: Moment,
                       dbTime?: Moment,
                       buffer: number = 0): Observable<FeatureCollection<GeometryObject>> {
        console.debug("Intersecting at:", wkt, validTime.toISOString());

        if (isNullOrUndefined(wkt)) {
            return Observable.throw("Intersection boundary cannot be empty");
        }

        const postBody: IIntersectionBody = {
            dataset,
            validAt: validTime.toISOString(),
            databaseAt: new Date().toISOString(),
            geojson: MapService.normalizeToGeoJSON(wkt),
            buffer
        };
        console.debug("Post body", postBody);
        return this.http.post("/visualize/intersect", postBody)
            .map((res) => res.json())
            .flatMap(this.parseToGeoJSONWorker)
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Performa a spatio-temporal intersection for the given WKT boundary, returning the results as a list of {TrestleIndividual}
     * @param {string} dataset to use
     * @param {wktValue} wkt boundary
     * @param {moment.Moment} validTime of intersection
     * @param {moment.Moment} dbTime of intersection
     * @param {number} buffer (in meters) around boundary
     * @returns {Observable<TrestleIndividual[]>}
     */
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

        return this.http.post("/individual/intersect-individuals", postBody)
            .map(MapService.parseResponseToIndividuals);
    }

    /**
     * Perform a spatio-temporal comparison between the input object and the given set of comparison objects
     * @param {ICompareBody} request
     * @returns {Observable<IComparisonReport>}
     */
    public compareIndividuals(request: ICompareBody): Observable<IComparisonReport> {
        return this.http.post("/visualize/compare", request)
            .map((results) => results.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Parses an input set of generic objects, by sending them to a web worker to do the interesting stuff
     * @param {object[]} objects
     * @returns {Observable<FeatureCollection<GeometryObject>>}
     */
    private parseToGeoJSONWorker = (objects: object[]): Observable<FeatureCollection<GeometryObject>> => {
        console.debug("Sending to worker");
        const id = new Date().getTime();
        const workerRequest: IMapWorkerRequest = {
            id,
            response: objects
        };

        //    Dispatch the event
        this.worker.postMessage(workerRequest);

        //    Subscribe to the event stream
        return Observable.create((observer: Subscriber<FeatureCollection<GeometryObject>>) => {
            this.workerStream
                .filter((m) => m.id === id)
                .subscribe((msg) => {
                    console.debug("Has from worker:", msg);
                    observer.next(msg.geom);
                    observer.complete();
                });
        });
    };

    private dsAPICall(): Observable<string[]> {
        return this.http.get("/visualize/datasets")
            .do((res) => console.debug("Available datasets:", res.text()))
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public static normalizeToGeoJSON(geom: wktValue): Polygon | MultiPolygon {
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

    private static parseResponseToIndividuals(res: Response): TrestleIndividual[] {
        const json = res.json();
        console.debug("Intersected result from server:", json);
        return json
            .map((individual: ITrestleIndividual) => new TrestleIndividual(individual));
    }

    private static isGeometryObject(x: any): x is GeometryObject {
        return (x as GeometryObject).type !== undefined;
    }
}
