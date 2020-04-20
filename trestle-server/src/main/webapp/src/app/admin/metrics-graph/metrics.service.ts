/**
 * Created by nrobison on 3/24/17.
 */
import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {Observable, throwError} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';

export interface ITrestleMetricsHeader {
  upTime: number;
  startTime: number;
  meters: Map<string, string>;
}

export interface IMetricsData {
  metric: string;
  values: IMetricsValue[];
}

export interface IMetricsValue {
  timestamp: Date;
  value: number;
}

@Injectable()
export class MetricsService {

  private readonly baseUrl: string;

  constructor(private authHttp: HttpClient) {
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Get initial data about available metrics
   * @returns {Observable<ITrestleMetricsHeader>}
   */
  public getMetrics(): Observable<ITrestleMetricsHeader> {
    return this.authHttp.get<ITrestleMetricsHeader>(this.baseUrl + '/metrics')
      .pipe(catchError((error: Error) => throwError(error || 'Server Error')));
  }

  /**
   * Get values for a specified metric ID that falls within the given range
   * @param {string} metricID to fetch
   * @param {number} start of temporal period for metrics data
   * @param {number} end of temporal period for metrics data
   * @returns {Observable<IMetricsData>}
   */
  public getMetricValues(metricID: string, start: number, end: number): Observable<IMetricsData> {
    console.debug('Retrieving values for metric: ' + metricID + ' from: ' + start + ' to: ' + end);
    const params = new HttpHeaders();
    params.append('start', start.toString());
    params.append('end', end.toString());
    return this.authHttp.get(this.baseUrl + '/metrics/metric/' + metricID, {
      params: {
        start: start.toString(),
        end: end.toString()
      }
    })
      .pipe(
        tap(json => console.debug('Metric values:', json)),
        map(json => {
          const metricValues: IMetricsValue[] = [];
          Object.keys(json).forEach((key) => {
            const longKey = parseInt(key, 10);
            if (longKey !== 0) {
              metricValues.push({
                timestamp: new Date(longKey),
                value: json[longKey]
              });
            }
          });
          return {
            metric: metricID,
            values: metricValues.sort((a, b) => {
              if (a.timestamp === b.timestamp) {
                return 0;
              }
              if (a.timestamp < b.timestamp) {
                return -1;
              }
              return 1;
            })
          };
        }));
  }

  /**
   * Export the given metric values as a CSV file
   * @param {string[] | null} metrics to export
   * @param {number} start of temporal period
   * @param {number} end of temporal period
   * @returns {Observable<Blob>}
   */
  public exportMetricValues(metrics: null | string[], start: number, end?: number): Observable<Blob> {
    return this.authHttp.post(this.baseUrl + '/metrics/export', {
        metrics,
        start,
        end
      },
      {
        responseType: 'blob'
      });
  }
}
