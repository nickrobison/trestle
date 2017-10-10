import { ITrestleTemporal, TrestleTemporal } from "./trestle-temporal";
import { ITrestleFact, TrestleFact } from "./trestle-fact";
import { ITrestleRelation, TrestleRelation } from "./trestle-relation";
import { ITrestleEvent, TrestleEvent } from "./trestle-event";
import { GeometryObject } from "geojson";
import { IInterfacable } from "../../interfacable";
const parse = require("wellknown");

export interface ITrestleIndividual {
    individualID: string;
    individualTemporal: ITrestleTemporal;
    facts: ITrestleFact[];
    relations: ITrestleRelation[];
    events: ITrestleEvent[];
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
        this.facts.forEach((value) => {
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

    public getEvents(): TrestleEvent[] {
        return this.events;
    }

    public asInterface(): ITrestleIndividual {
        const returnValue: ITrestleIndividual = {
            individualID: this.id,
            individualTemporal: this.existsTemporal.asInterface(),
            facts: [],
            relations: [],
            events: []
        };
        this.facts.forEach((value) => {
            returnValue.facts.push(value.asInterface());
        });
        this.relations.forEach((value) => {
            returnValue.relations.push(value.asInterface());
        });
        this.events.forEach((event) => returnValue.events.push(event.asInterface()));
        return returnValue;
    }
}
