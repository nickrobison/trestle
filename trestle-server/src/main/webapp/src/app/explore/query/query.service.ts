/**
 * Created by nrobison on 2/27/17.
 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export interface ITrestleResultSet {
  rows: number;
  bindingNames: Array<string>;
  results: Array<ITrestleResult>;
}

export interface ITrestleResult {
  resultValues: Map<string, string>;
}

@Injectable()
export class QueryService {

  private readonly baseUrl: string;

  constructor(private trestleHttp: HttpClient) {
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Get currently registered prefixes from the database
   * @returns {Observable<any>}
   */
  public getPrefixes(): Observable<any> {
    return this.trestleHttp.get(this.baseUrl+ '/query');
  }

  /**
   * Execute SPARQL query and return the results
   * @param {string} queryString
   * @returns {Observable<ITrestleResultSet>}
   */
  public executeQuery(queryString: string): Observable<ITrestleResultSet> {
    console.debug('Query string:', queryString);
    return this.trestleHttp.post<ITrestleResultSet>(this.baseUrl+ '/query', queryString);
  }
}
