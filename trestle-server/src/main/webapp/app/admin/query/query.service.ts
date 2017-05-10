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
        console.debug("Query string:", queryString);
        return this.authHttp.post("/query", queryString)
            .map((res: Response) => res.json())
            .catch(this.handleError);
    }

    private handleError(error: Response | any) {
        console.error(error);
        let errMsg: string;
        if (error instanceof Response) {
            errMsg = error.text();
        } else {
            errMsg = error.message ? error.message : error.toString();
        }
        return Observable.throw(errMsg);
    }
}