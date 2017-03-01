/**
 * Created by nrobison on 2/27/17.
 */
import {Injectable} from "@angular/core";   
import {AuthHttp} from "angular2-jwt";
import {Observable} from "rxjs";
import {Response} from "@angular/http";

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

    constructor(private authHttp: AuthHttp) {}

    getPrefixes(): Observable<any> {
        return this.authHttp.get("/query")
            .map((res: Response) => {
            console.debug("Prefix response:", res.json());
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    executeQuery(queryString: string): Observable<ITrestleResultSet> {
        return this.authHttp.post("/query", queryString)
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}