import {Inject, Injectable, InjectionToken} from '@angular/core';
import {TrestleIndividual} from '../individual/TrestleIndividual/trestle-individual';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {environment} from '../../../environments/environment';
import {CacheService} from '../cache/cache.service';
import {map, tap} from 'rxjs/operators';

export const DATASET_CACHE = new InjectionToken<CacheService<string, string[]>>('dataset.cache');

@Injectable()
export class DatasetService {

  private readonly baseUrl;

  constructor(private http: HttpClient,
              @Inject(DATASET_CACHE) private cache: CacheService<string, string[]>) {
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Returns the list of currently registered datasets from the database
   * @returns {Observable<string[]>}
   */
  public getAvailableDatasets(): Observable<string[]> {
    // Try from cache first, then hit the API
    return this.cache.get('datasets', this.dsAPICall());
  }

  /**
   * Get all the data properties for the given dataset, returns them as a list with everything but the property name removed.
   *
   * @param {string} dataset - Dataset to query
   * @returns {Observable<string[]>} - Filtered list of registered data properties
   */
  public getDatasetProperties(dataset: string): Observable<string[]> {
    return this.http.get(this.baseUrl + '/datasets/' + dataset)
      .pipe(map(DatasetService.filterPropertyNames));
  }

  /**
   * Get a sampling of unique values for the given dataset property
   * Defaults to 100 values if no limit is provided.
   *
   * @param {string} dataset - Dataset to query
   * @param {string} fact - Property to get values for
   * @param {number} limit - Optional limit of values to return (defaults to 100)
   * @returns {Observable<string[]>} - List of property values
   */
  public getDatasetFactValues(dataset: string, fact: string, limit = 100): Observable<string[]> {

    const datsetURL = this.baseUrl + '/datasets/' + dataset + '/' + fact + '/values';

    const params = new HttpParams();
    params.append('limit', limit.toString());

    return this.http.get<string[]>(datsetURL, {
      params
    });
    // .catch(DatasetService.errorHandler);
  }

  private dsAPICall(): Observable<string[]> {
    return this.http.get<string[]>(this.baseUrl + '/datasets')
      .pipe(tap((res) => console.debug('Available datasets:', res)));

    // .map((res: Response) => res.json())
    // .catch(DatasetService.errorHandler);
  }

  public static errorHandler(error: any): Observable<Error> {
    return throwError(error || 'Server Error');
  }

  public static filterPropertyNames(properties: string[]): string[] {
    return properties
      .map((property) => {
        return TrestleIndividual.extractSuffix(property);
      });
  }
}
