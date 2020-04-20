import {Injectable} from "@angular/core";
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';

export interface IExportRequest {
    dataset: string;
    individuals: string[];
    type: string;
}

@Injectable()
export class ExporterService {

    constructor(private http: HttpClient) {

    }

    /**
     * Export indivduals and return the built result from the database
     * @param {IExportRequest} request
     * @returns {Observable<Blob>}
     */
    public exportIndividuals(request: IExportRequest): Observable<Blob> {
        return this.http.post("/export", request, {
            responseType: 'blob'
        });
    }
}
