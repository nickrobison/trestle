/**
 * Created by nrobison on 3/7/17.
 */
import {Injectable} from "@angular/core";
import {AuthHttp} from "angular2-jwt";
import {Observable} from "rxjs";
import {URLSearchParams, Response} from "@angular/http";

export interface ITrestleFact {
    identifier: string
    name: string;
    value: string;
    databaseTemporal: ITrestleTemporal;
    validTemporal: ITrestleTemporal;

}

export interface ITrestleIndividual {
    individualID: string;
    individualTemporal: ITrestleTemporal;
    facts: Array<ITrestleFact>;

}

export interface ITrestleTemporal {
    validID: string;
    validFrom: Date;
    validTo?: Date;
}

@Injectable()
export class VisualizeService {

    constructor(private authHttp: AuthHttp) {}

    searchForIndividual(name: string, dataset = "", limit = 10): Observable<Array<string>> {
        let params = new URLSearchParams();
        params.set("name", name);
        params.set("dataset", dataset);
        params.set("limit", limit.toString());
        return this.authHttp.get("/visualize/search", {
            search: params
        })
            .map((res: Response) => {
            console.debug("Search response:", res.json());
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    getIndividualAttributes(name: string): Observable<ITrestleIndividual> {
        let params = new URLSearchParams();
        params.set("name", name);
        return this.authHttp.get("/visualize/retrieve", {
            search: params
        })
            .map((res: Response) => {
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}