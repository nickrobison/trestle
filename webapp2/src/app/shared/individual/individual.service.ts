/**
 * Created by nrobison on 3/7/17.
 */
import {Inject, Injectable, InjectionToken} from '@angular/core';
import {ITrestleIndividual, TrestleIndividual} from './TrestleIndividual/trestle-individual';
import {CacheService} from '../cache/cache.service';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';
import {map, tap} from 'rxjs/operators';

export const INDIVIDUAL_CACHE = new InjectionToken<CacheService<string, TrestleIndividual>>('individual.cache');

@Injectable()
export class IndividualService {

  private readonly baseURL;

  constructor(private trestleHttp: HttpClient,
              @Inject(INDIVIDUAL_CACHE) private individualCache: CacheService<string, TrestleIndividual>) {
    this.baseURL = environment.baseUrl;
  }

  /**
   * Search for an individual in the database
   * @param {string} name of the individual to search for (partial value)
   * @param {string} dataset to restrict queries to
   * @param {number} limit number of return values
   * @returns {Observable<string[]>}
   */
  public searchForIndividual(name: string, dataset = '', limit = 10): Observable<string[]> {
    const params = new HttpParams();
    params.set('name', name);
    params.set('dataset', dataset);
    params.set('limit', limit.toString());
    return this.trestleHttp.get<string[]>(this.baseURL + '/visualize/search', {
      params
    })
      .pipe(tap(res => console.debug('Search response:', res)));
    // .catch((error: Error) => Observable.throw(error || "Server Error"));
  }

  /**
   * Return a {TrestleIndividual} from the API
   * Uses the cache if possible
   * @param {string} name - Individual IRI string
   * @returns {Observable<TrestleIndividual>}
   */
  public getTrestleIndividual(name: string): Observable<TrestleIndividual> {
    return this.individualCache.get(name, this.getIndividualAPI(name));
  }

  private getIndividualAPI(name: string): Observable<TrestleIndividual> {
    const params = new HttpParams();
    params.set('name', name);
    return this.trestleHttp.get<ITrestleIndividual>(this.baseURL + '/individual/retrieve', {
      params
    })
      .pipe(map(response => {
        console.debug('Has response, building object', response);
        return new TrestleIndividual(response);
      }))
    // .catch((error: Error) => Observable.throw(error || "Server Error"));
  }
}
