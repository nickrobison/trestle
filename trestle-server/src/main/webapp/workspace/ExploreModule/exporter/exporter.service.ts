import {Injectable} from "@angular/core";
import {TrestleHttp} from "../../UserModule/trestle-http.provider";
import {Observable} from "rxjs/Observable";
import {Response, ResponseContentType} from "@angular/http";

export interface IExportRequest {
    dataset: string;
    individuals: string[];
    type: string;
}

@Injectable()
export class ExporterService {

    constructor(private http: TrestleHttp) {

    }

    /**
     * Export indivduals and return the built result from the database
     * @param {IExportRequest} request
     * @returns {Observable<Blob>}
     */
    public exportIndividuals(request: IExportRequest): Observable<Blob> {
        return this.http.post("/export", request, {
            responseType: ResponseContentType.Blob
        })
            .map((res: Response) => {
                return res.blob();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
