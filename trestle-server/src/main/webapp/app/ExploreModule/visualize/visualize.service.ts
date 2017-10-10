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

export interface IInterfacable<I> {
    asInterface(): I;
    asInterface(): I;
}

export class TrestleIndividual implements IInterfacable<ITrestleIndividual> {

    private id: string;
    private facts: Map<string, TrestleFact> = new Map();
    private relations: TrestleRelation[] = [];
    private events: TrestleEvent[] = [];
    private existsTemporal: TrestleTemporal;

    constructor(individual: ITrestleIndividual) {
        console.debug("Building individual", individual.individualID);
        this.id = individual.individualID;
        this.existsTemporal = new TrestleTemporal(individual.individualTemporal);
        individual.facts.forEach((fact) => {
            const factClass = new TrestleFact(fact);
            this.facts.set(factClass.getName(), factClass);
        });
        individual.relations.forEach((relation) => {
            this.relations.push(new TrestleRelation(relation));
        });
        individual.events.forEach((event) => this.events.push(new TrestleEvent(event)));
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
                const geojson = parse(fact.getValue());
                if (geojson != null) {
                    returnValue = geojson;
                } else {
                    console.error("Failed to parse:", fact.getValue());
                }
            }
        });
        return returnValue;
    }

    public getFacts(): TrestleFact[] {
        const facts: TrestleFact[] = [];
        this.facts.forEach((value, key) => {
            facts.push(value);
        });

        return facts;
    }

    public getFactValues(): {[name: string]: any} {
        const values: {[name: string]: any} = {};
        this.facts.forEach((value) => {
            values[value.getName()] = value.getValue();
        });
        return values;
    }

    public getRelations(): TrestleRelation[] {
        return this.relations;
    }

    public asInterface(): ITrestleIndividual {
        const returnValue: ITrestleIndividual = {
            individualID: this.id,
            individualTemporal: this.existsTemporal.asInterface(),
            facts: [],
            relations: [],
            events: []
        };
        this.facts.forEach((value, key) => {
            returnValue.facts.push(value.asInterface());
        });
        this.relations.forEach((value) => {
            returnValue.relations.push(value.asInterface());
        });
        this.events.forEach((event) => returnValue.events.push(event.asInterface()));
        return returnValue;
    }
}

export class TrestleFact implements IInterfacable<ITrestleFact> {
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
        return this.name === "asWKT";
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

export class TrestleRelation implements IInterfacable<ITrestleRelation> {
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
        };
    }
}

export class TrestleEvent implements IInterfacable<ITrestleEvent> {
    private individual: string;
    private type: TrestleEventType;
    private temporal: Moment;

    constructor(event: ITrestleEvent) {
        this.individual = event.individual;
        this.type = event.type;
        this.temporal = moment(event.temporal, ISO_8601);
    }

    public getIndividual() {
        return this.individual;
    }

    public getType() {
        return this.type;
    }

    public getTemporal() {
        return this.temporal;
    }

    public asInterface() {
        return {
            individual: this.individual,
            type: this.type,
            temporal: this.temporal.toISOString()
        };
    }
}

export class TrestleTemporal implements IInterfacable<ITrestleTemporal> {
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

    public isContinuing(): boolean {
        return this.to == null || !this.to.isValid();
    }

    public asInterface(): ITrestleTemporal {
        const returnValue: ITrestleTemporal = {
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
    facts: ITrestleFact[];
    relations: ITrestleRelation[];
    events: ITrestleEvent[];
}

export interface ITrestleFact {
    identifier: string;
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

export interface ITrestleEvent {
    individual: string;
    type: TrestleEventType;
    temporal: string;
}

export enum TrestleEventType {
    CREATED,
    DESTROYED,
    BECAME,
    SPLIT,
    MERGED
}

@Injectable()
export class VisualizeService {

    constructor(private trestleHttp: TrestleHttp) {
    }

    public searchForIndividual(name: string, dataset = "", limit = 10): Observable<string[]> {
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

    public getIndividualAttributes(name: string): Observable<TrestleIndividual> {
        let params = new URLSearchParams();
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