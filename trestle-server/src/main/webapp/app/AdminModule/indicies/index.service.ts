import { Injectable } from "@angular/core";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { Observable } from "rxjs/Observable";

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

    public constructor(private http: TrestleHttp) {

    }

    public getIndexStatistics(): Observable<ICacheStatistics> {
        return this.http
            .get("/cache/index")
            .map((res) => {
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}