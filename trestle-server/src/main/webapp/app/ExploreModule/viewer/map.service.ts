/**
 * Created by nrobison on 6/23/17.
 */
import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Response } from "@angular/http";
import { Observable } from "rxjs/Observable";

@Injectable()
export class MapService {

    constructor(private http: TrestleHttp) {

    }

    public getAvailableDatasets(): Observable<string[]> {
        return this.http.get("/visualize/datasets")
            .map((res: Response) => {
            console.debug("Available datasets:", res.text());
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}