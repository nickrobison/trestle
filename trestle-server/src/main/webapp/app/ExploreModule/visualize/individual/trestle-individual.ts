import { ITrestleTemporal, TrestleTemporal } from "./trestle-temporal";
import { ITrestleFact, TrestleFact } from "./trestle-fact";
import { ITrestleRelation, TrestleRelation } from "./trestle-relation";
import { ITrestleEvent, TrestleEvent } from "./trestle-event";
import { GeometryObject } from "geojson";
import { IInterfacable } from "../../interfacable";
import { parse } from "wellknown";

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
                console.debug("Fact is spatial", fact);
                const geojson = parse(fact.getValue());
                console.debug("GeoJSON value:", geojson);
                if (geojson != null) {
                    returnValue = geojson;
                } else {
                    console.error("Failed to parse:", fact.getValue());
                }
            }
        });
        if (returnValue === null) {
            throw new Error("Individual " + this.getID() + " is not spatial and should be");
        }
        return returnValue;
    }

    public getFacts(): TrestleFact[] {
        const facts: TrestleFact[] = [];
        this.facts.forEach((value) => {
            facts.push(value);
        });

        return facts;
    }

    public getFactValues(): { [name: string]: any } {
        const values: { [name: string]: any } = {};
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

    /**
     * Gets the start event for the given individual.
     * Returns a MERGED event, if one exists
     * Otherwise, returns CREATED
     * @returns {TrestleEvent}
     */
    public getStartEvent(): TrestleEvent {
        const mergedEvent = this.getEvents()
            .filter((event) => event.getType() === "MERGED");

        if (mergedEvent.length > 0) {
            return mergedEvent[0];
        }
        const createdEvent = this.getEvents()
            .filter((event) => event.getType() === "CREATED");
        if (createdEvent.length > 0) {
            return createdEvent[0];
        }
        throw new Error("Individual: " + this.getID() + " does not have a CREATED/MERGED event");
    }

    /**
     * Returns the end event, if one exists
     * If a SPLIT event exists, returns that
     * Otherwise, returns DESTROYED
     * Returns
     * @returns {TrestleEvent}
     */
    public getEndEvent(): TrestleEvent | null {
        const splitEvent = this.getEvents()
            .filter((event) => event.getType() === "SPLIT");
        if (splitEvent.length > 0) {
            return splitEvent[0];
        }

        const destroyedEvent = this.getEvents()
            .filter((event) => event.getType() === "DESTROYED");
        if (destroyedEvent.length > 0) {
            return destroyedEvent[0];
        }

        return null;
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
