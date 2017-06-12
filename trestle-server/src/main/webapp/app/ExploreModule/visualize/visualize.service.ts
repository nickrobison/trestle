/**
 * Created by nrobison on 3/7/17.
 */
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {URLSearchParams, Response} from "@angular/http";
import {TrestleHttp} from "../../UserModule/trestle-http.provider";
import moment = require("moment");
import {ISO_8601, Moment} from "moment";
import {GeometryObject} from "geojson";
var parse = require("wellknown");


export class TrestleIndividual {

    private id: string;
    private facts: Map<string, TrestleFact> = new Map();
    private relations: Array<TrestleRelation> = [];
    private existsTemporal: TrestleTemporal;

    constructor(individual: ITrestleIndividual) {
        console.debug("Building individual", individual.individualID);
        this.id = individual.individualID;
        this.existsTemporal = new TrestleTemporal(individual.individualTemporal);
        individual.facts.forEach((fact) => {
            let factClass = new TrestleFact(fact);
            this.facts.set(factClass.getName(), factClass);
        });
        individual.relations.forEach((relation) => {
            this.relations.push(new TrestleRelation(relation));
        })
    }

    public getID(): string {
        return this.id;
    }

    public getTemporal(): TrestleTemporal {
        return this.existsTemporal;
    }

    public getSpatialValue(): GeometryObject {
        let returnValue = null;
        this.facts.forEach((fact) => {
            if (fact.isSpatial()) {
                let geojson = parse(fact.getValue());
                if (geojson !== null) {
                    returnValue = geojson;
                } else {
                    console.error("Failed to parse:", fact.getValue());
                }
            }
        });
        return returnValue;
    }

    public getFacts(): Array<TrestleFact> {
        let facts: Array<TrestleFact> = [];
        this.facts.forEach((value, key) => {
            facts.push(value);
        });

        return facts;
    }

    public getRelations(): Array<TrestleRelation> {
        return this.relations;
    }

    public asInterface(): ITrestleIndividual {
        let returnValue: ITrestleIndividual = {
            individualID: this.id,
            individualTemporal: this.existsTemporal.asInterface(),
            facts: [],
            relations: []
        };
        this.facts.forEach((value, key) => {
            returnValue.facts.push(value.asInterface());
        });
        this.relations.forEach(value => {
            returnValue.relations.push(value.asInterface());
        });
        return returnValue;
    }
}

export class TrestleFact {
    private identifier: string;
    private name: string;
    private type: string;
    private value: string;
    private databaseTemporal: TrestleTemporal;
    private validTemporal: TrestleTemporal;

    constructor(fact: ITrestleFact) {
        console.debug("Building fact", fact.identifier);
        this.identifier = fact.identifier;
        this.name = fact.name;
        this.type = fact.type;
        this.value = fact.value;
        this.databaseTemporal = new TrestleTemporal(fact.databaseTemporal);
        this.validTemporal = new TrestleTemporal(fact.validTemporal);
    }

    public getID(): string {
        return this.identifier;
    }

    public getName(): string {
        return this.name;
    }

    public getValue(): string {
        return this.value;
    }

    public getType(): string {
        return this.type;
    }

    public getValidTemporal(): TrestleTemporal {
        return this.validTemporal;
    }

    public getDatabaseTemporal(): TrestleTemporal {
        return this.databaseTemporal;
    }

    public isSpatial(): boolean {
        return this.name == "asWKT";
    }

    public asInterface(): ITrestleFact {
        return {
            identifier: this.identifier,
            name: this.name,
            type: this.type,
            value: this.value,
            databaseTemporal: this.databaseTemporal.asInterface(),
            validTemporal: this.validTemporal.asInterface()
        };
    }
}

export class TrestleRelation {
    private subject: string;
    private object: string;
    private type: TrestleRelationType;

    constructor(relation: ITrestleRelation) {
        this.subject = relation.subject;
        this.object = relation.object;
        this.type = relation.relation;
    }

    public getSubject(): string {
        return this.subject;
    }

    public getObject(): string {
        return this.object;
    }

    public getType(): TrestleRelationType {
        return this.type;
    }

    public asInterface(): ITrestleRelation {
        return {
            subject: this.subject,
            object: this.object,
            relation: this.type
        }
    }
}

export class TrestleTemporal {
    private id: string;
    private from: Moment;
    private to?: Moment;

    constructor(temporal: ITrestleTemporal) {
        this.id = temporal.validID;
        this.from = moment(temporal.validFrom, ISO_8601);
        if (temporal.validTo !== null) {
            this.to = moment.utc(temporal.validTo, ISO_8601);
        }
    }

    public getID(): string {
        return this.id;
    }

    public getFrom(): Moment {
        return this.from;
    }

    public getTo(): Moment {
        return this.to;
    }

    isContinuing(): boolean {
        return this.to == null || !this.to.isValid();
    }

    public asInterface(): ITrestleTemporal {
        let returnValue: ITrestleTemporal = {
            validID: this.id,
            validFrom: this.from.toDate()
        };
        if (!this.isContinuing()) {
            returnValue.validTo = this.to.toDate();
        }
        return returnValue;
    }
}


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

    constructor(private trestleHttp: TrestleHttp) {
    }

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

    getIndividualAttributes(name: string): Observable<TrestleIndividual> {
        let params = new URLSearchParams();
        params.set("name", name);
        return this.trestleHttp.get("/visualize/retrieve", {
            search: params
        })
            .map((res: Response) => {
                let response = res.json();
                console.debug("Has response, building object", response);
                return new TrestleIndividual(response);
            })
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}