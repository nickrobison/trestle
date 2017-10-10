/**
 * Created by nrobison on 3/7/17.
 */
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { URLSearchParams, Response } from "@angular/http";
import { TrestleHttp } from "../../UserModule/trestle-http.provider";
import { TrestleIndividual } from "./individual/trestle-individual";

@Injectable()
export class VisualizeService {

    constructor(private trestleHttp: TrestleHttp) {
    }

    public searchForIndividual(name: string, dataset = "", limit = 10): Observable<string[]> {
        const params = new URLSearchParams();
        params.set("name", name);
        params.set("dataset", dataset);
        params.set("limit", limit.toString());
        return this.trestleHttp.get("/visualize/search", {
            search: params
        })
            .map((res: Response) => {
                console.debug("Search response:", res.json());
                return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    public getIndividualAttributes(name: string): Observable<TrestleIndividual> {
        const params = new URLSearchParams();
        params.set("name", name);
        return this.trestleHttp.get("/visualize/retrieve", {
            search: params
        })
            .map((res: Response) => {
                const response = res.json();
                console.debug("Has response, building object", response);
                return new TrestleIndividual(response);
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}
