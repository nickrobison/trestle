import {Injectable} from '@angular/core';
import {GeometryObject} from 'geojson';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';

export const BBOX_PROPERTY = 'BOUNDING_BOX';
export type AggregationOperation = 'EQ' | 'NEQ' | 'GT' | 'GTEQ' | 'LT' | 'LTEQ';

export interface IAggregationRestriction {
  dataset: string;
  property: string;
  value: object;
}

export interface IAggregationStrategy {
  field: string;
  operation: AggregationOperation;
  value: object;
}

export interface IAggregationRequest {
  restriction: IAggregationRestriction;
  strategy: IAggregationStrategy;
}

@Injectable()
export class AggregationService {

  private readonly baseURL: string;

  constructor(private http: HttpClient) {
    this.baseURL = environment.baseUrl;
  }

  public performAggregation<T>(request: IAggregationRequest): Observable<GeometryObject> {
    console.debug('Aggregating!', request);
    return this.http.post<GeometryObject>(this.baseURL + '/aggregate', request)
  }
}
