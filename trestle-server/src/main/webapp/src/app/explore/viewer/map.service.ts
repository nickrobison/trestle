/**
 * Created by nrobison on 6/23/17.
 */
import {Injectable} from '@angular/core';
import {LngLatBounds} from 'mapbox-gl';
import {FeatureCollection, GeometryObject, MultiPolygon, Polygon} from 'geojson';
import {Moment} from 'moment';
import {fromEvent, Observable, Subscriber, throwError} from 'rxjs';
import {filter, flatMap, map} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {ITrestleIndividual, TrestleIndividual} from '../../shared/individual/TrestleIndividual/trestle-individual';

export type wktType = 'POINT' |
  'MULTIPOINT' |
  'LINESTRING' |
  'MULTILINESTRING' |
  'POLYGON' |
  'MULTIPOLYGON';

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


@Injectable()
export class MapService {
  private worker: Worker;
  private workerStream: Observable<IMapWorkerResponse>;
  private readonly baseURL;

  constructor(private http: HttpClient) {
    //    Create the worker and register a stream for the results
    this.worker = new Worker("/projector");
    this.workerStream = fromEvent(this.worker, 'message')
      .pipe(map((m: MessageEvent) => (m.data as IMapWorkerResponse)));
    this.baseURL = environment.baseUrl;
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
    console.debug('Intersecting at:', wkt, validTime.toISOString());

    if (wkt === null || wkt === undefined) {
      return throwError('Intersection boundary cannot be empty');
    }

    const postBody: IIntersectionBody = {
      dataset,
      validAt: validTime.toISOString(),
      databaseAt: new Date().toISOString(),
      geojson: MapService.normalizeToGeoJSON(wkt),
      buffer
    };
    console.debug('Post body', postBody);
    return this.http.post(this.baseURL + '/visualize/intersect', postBody)
      .pipe(flatMap(this.parseToGeoJSONWorker))
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

    console.debug('Intersecting individuals with', postBody);

    return this.http.post(this.baseURL + '/individual/intersect-individuals', postBody)
      .pipe(map(MapService.parseResponseToIndividuals));
  }

  /**
   * Perform a spatio-temporal comparison between the input object and the given set of comparison objects
   * @param {ICompareBody} request
   * @returns {Observable<IComparisonReport>}
   */
  public compareIndividuals(request: ICompareBody): Observable<IComparisonReport> {
    return this.http.post<IComparisonReport>(this.baseURL+ '/visualize/compare', request);
  }

  /**
   * Parses an input set of generic objects, by sending them to a web worker to do the interesting stuff
   * @param {object[]} objects
   * @returns {Observable<FeatureCollection<GeometryObject>>}
   */
  private parseToGeoJSONWorker = (objects: object[]): Observable<FeatureCollection<GeometryObject>> => {
    console.debug('Sending to worker');
    const id = new Date().getTime();
    const workerRequest: IMapWorkerRequest = {
      id,
      response: objects
    };

    //    Dispatch the event
    this.worker.postMessage(workerRequest);

    //    Subscribe to the event stream
    return new Observable((observer: Subscriber<FeatureCollection<GeometryObject>>) => {
      this.workerStream
        .pipe(filter(m => m.id === id))
        .subscribe((msg) => {
          console.debug('Has from worker:', msg);
          observer.next(msg.geom);
          observer.complete();
        });
    });
  };

  public static normalizeToGeoJSON(geom: wktValue): Polygon | MultiPolygon {
    if (MapService.isGeometryObject(geom)) {
      if (geom.type === 'Polygon') {
        return (geom as Polygon);
      } else if (geom.type === 'MultiPolygon') {
        return (geom as MultiPolygon);
      } else {
        console.error('Not correct geom', geom);
        throw new Error('Not correct geometry');
      }
    }
    return {
      type: 'Polygon',
      // need to return and array of bounds as an array of SW -> NW -> NE -> SE -> SW
      coordinates: [[geom.getSouthWest().toArray(),
        geom.getNorthWest().toArray(),
        geom.getNorthEast().toArray(),
        geom.getSouthEast().toArray(),
        geom.getSouthWest().toArray()]]
      // crs: {type: "name", properties: {name: "EPSG:4326"}}
    };
  }

  private static parseResponseToIndividuals(res: ITrestleIndividual[]): TrestleIndividual[] {
    console.debug('Intersected result from server:', res);
    return res
      .map((individual: ITrestleIndividual) => new TrestleIndividual(individual));
  }

  private static isGeometryObject(x: any): x is GeometryObject {
    return (x as GeometryObject).type !== undefined;
  }
}
