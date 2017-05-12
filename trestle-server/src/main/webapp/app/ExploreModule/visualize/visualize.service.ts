/**
 * Created by nrobison on 3/7/17.
 */
import {Injectable} from "@angular/core";
import {AuthHttp} from "angular2-jwt";
import {Observable} from "rxjs";
import {URLSearchParams, Response} from "@angular/http";
import {TrestleHttp} from "../../UserModule/trestle-http.provider";

export interface ITrestleIndividual {
    individualID: string;
    individualTemporal: ITrestleTemporal;
    facts: Array<ITrestleFact>;
    relations: Array<ITrestleRelation>;
}

export interface ITrestleFact {
    identifier: string
    name: string;
    type: string;
    value: string;
    databaseTemporal: ITrestleTemporal;
    validTemporal: ITrestleTemporal;
}

export interface ITrestleTemporal {
    validID: string;
    validFrom: Date;
    validTo?: Date;
}

export interface ITrestleRelation {
    subject: string;
    object: string;
    relation: TrestleRelationType;
}

export enum TrestleRelationType {
    // Spatial
    CONTAINS,
    COVERS,
    DISJOINT,
    EQUALS,
    INSIDE,
    MEETS,
    SPATIAL_OVERLAPS,
    // Temporal
    AFTER,
    BEFORE,
    BEGINS,
    DURING,
    ENDS,
    TEMPORAL_OVERLAPS
}

@Injectable()
export class VisualizeService {

    constructor(private trestleHttp: TrestleHttp) {}

    searchForIndividual(name: string, dataset = "", limit = 10): Observable<Array<string>> {
        let params = new URLSearchParams();
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

    getIndividualAttributes(name: string): Observable<ITrestleIndividual> {
        let params = new URLSearchParams();
        params.set("name", name);
        return this.trestleHttp.get("/visualize/retrieve", {
            search: params
        })
            .map((res: Response) => {
            return res.json();
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}