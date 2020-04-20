import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {Observable} from 'rxjs';

export interface IIndexLeafStatistics {
  binaryID: string;
  type: string;
  coordinates: number[];
  leafID: number;
  direction: number;
  records: number;
}

export interface ICacheStatistics {
  offsetValue: number;
  maxValue: number;
  dbIndexFragmentation: number;
  dbIndexSize: number;
  dbLeafStats: IIndexLeafStatistics[];
  validIndexFragmentation: number;
  validIndexSize: number;
  validLeafStats: IIndexLeafStatistics[];
}

@Injectable()
export class IndexService {

  private readonly baseUrl: string;

  public constructor(private http: HttpClient) {
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Get statistics for both caches
   * @returns {Observable<ICacheStatistics>}
   */
  public getIndexStatistics(): Observable<ICacheStatistics> {
    return this.http
      .get<ICacheStatistics>(this.baseUrl + '/cache/index');
  }

  /**
   * Rebuild specified index
   * @param {string} index to rebuild
   * @returns {Observable<void>}
   */
  public rebuildIndex(index: string): Observable<void> {
    return this.http
      .get<void>(this.baseUrl + '/cache/rebuild/' + index.toLocaleLowerCase());
  }

  /**
   * Purge the specified index
   * @param {string} cache to purge
   * @returns {Observable<void>}
   */
  public purgeCache(cache: string): Observable<void> {
    return this.http
      .get<void>(this.baseUrl + '/cache/purge/' + cache.toLocaleLowerCase());
  }
}
