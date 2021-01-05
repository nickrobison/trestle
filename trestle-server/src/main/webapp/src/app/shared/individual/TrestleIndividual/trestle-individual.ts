import { ITrestleTemporal, TrestleTemporal } from "./trestle-temporal";
import { ITrestleFact, TrestleFact } from "./trestle-fact";
import { ITrestleRelation, TrestleRelation } from "./trestle-relation";
import { ITrestleEvent, TrestleEvent } from "./trestle-event";
import { GeometryObject } from "geojson";
import { IInterfacable } from "../../interfacable";
import { parse } from "wellknown";
import SortedArray from 'sorted-array';

export interface ITrestleIndividual {
    individualID: string;
    existsTemporal: ITrestleTemporal;
    facts: ITrestleFact[];
    relations: ITrestleRelation[];
    events: ITrestleEvent[];
}

export class TrestleIndividual implements IInterfacable<ITrestleIndividual> {

    private static suffixRegex = /.*[\/#]([^\/#]*)$/g;
    private static prefixRegex = /.*[\/#]/g;
    private static hostnameRegex = /\w+:\/\/[^\/]*/g;
    private readonly id: string;
    private filteredID?: string;
    private facts: SortedArray<TrestleFact>;
    private spatialFact?: TrestleFact;
    private relations: TrestleRelation[] = [];
    private events: TrestleEvent[] = [];
    private readonly existsTemporal: TrestleTemporal;

    constructor(individual: ITrestleIndividual) {
        this.id = individual.individualID;
        this.facts = new SortedArray<TrestleFact>([], TrestleIndividual.factSort);
        this.existsTemporal = new TrestleTemporal(individual.existsTemporal);
        individual.facts.forEach((fact) => {
            const factClass = new TrestleFact(fact);
            // Set as spatial fact, if that's the case
            if (factClass.isSpatial()) {
                this.spatialFact = factClass;
            }
            this.facts.insert(factClass);
        });
        individual.relations.forEach((relation) => {
            this.relations.push(new TrestleRelation(relation));
        });
        individual.events.forEach((event) => this.events.push(new TrestleEvent(event)));
    }

    /**
     * Get Individual ID
     * @returns {string}
     */
    public getID(): string {
        return this.id;
    }

    /**
     * Get the individual ID, without the URI base or temporal range
     * @returns {string}
     */
    public getFilteredID(): string {
        if (this.filteredID) {
            return this.filteredID;
        }
        this.filteredID = TrestleIndividual.filterID(this.id);
        return this.filteredID;
    }

    /**
     * Gets the URI base of the individual ID
     * @returns {string}
     */
    public getHostname(): string {
        return TrestleIndividual.extractHostname(this.id);
    }

    /**
     * Get the individual ID, without the URI base
     * @returns {string}
     */
    public withoutHostname(): string {
        return TrestleIndividual.withoutHostname(this.id);
    }

    /**
     * Return the filtered individual ID as a hashed numeric value
     * Currently using the SBDM algorithm
     * @returns {number}
     */
    public getIDAsInteger(): number {
        return TrestleIndividual.hashID(this.getFilteredID());
    }

    /**
     * Get the Existence temporal for the individual
     * @returns {TrestleTemporal}
     */
    public getTemporal(): TrestleTemporal {
        return this.existsTemporal;
    }

    /**
     * Get the spatial fact for the individual, parsed as a {GeometryObject}
     * @returns {GeometryObject}
     */
    public getSpatialValue(): GeometryObject {

        if (this.spatialFact) {
            const geojson = parse(this.spatialFact.getValue());
            if (geojson !== null) {
                return geojson;
            }
            console.error("Failed to parse:", this.spatialFact.getValue());
        }
        throw new Error("Individual " + this.getID() + " is not spatial and should be");
    }

    /**
     * Get the sptial fact for the indivdual, as a WKT string
     * @returns {string}
     */
    public getSpatialValueAsWKT(): string {
        if (this.spatialFact) {
            return this.spatialFact.getValue();
        }
        throw new Error("Individual " + this.getID() + " is not spatial and should be");
    }

    /**
     * Get an {Iterable} of {TrestleFact} of all facts for the individual
     * @returns {TrestleFact[]}
     */
    public getFacts(): TrestleFact[] {
        return this.facts.array;
    }

    /**
     * Returns a collection of fact names and associated values
     * @returns {{[p: string]: any}}
     */
    public getFactValues(): { [name: string]: any } {
        const values: { [name: string]: any } = {};
        this.facts.array.forEach((value) => {
            values[value.getName()] = value.getValue();
        });
        return values;
    }

    /**
     * Get all Individual relations
     * @returns {TrestleRelation[]}
     */
    public getRelations(): TrestleRelation[] {
        return this.relations;
    }

    /**
     * Get all individual events
     * @returns {TrestleEvent[]}
     */
    public getEvents(): TrestleEvent[] {
        return this.events;
    }

    /**
     * Gets the start event for the given selection.
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

    /**
     * Is this individual a part of a spatial union?
     * @returns {boolean}
     */
    public isUnion(): boolean {
        return this.relations.some((relation) => relation.isUnionType());
    }

    /**
     * Transform the individual back into its Interface type
     * @returns {ITrestleIndividual}
     */
    public asInterface(): ITrestleIndividual {
        const returnValue: ITrestleIndividual = {
            individualID: this.id,
            existsTemporal: this.existsTemporal.asInterface(),
            facts: [],
            relations: [],
            events: []
        };
        this.facts.array.forEach((value) => {
            returnValue.facts.push(value.asInterface());
        });
        this.relations.forEach((value) => {
            returnValue.relations.push(value.asInterface());
        });
        this.events.forEach((event) => returnValue.events.push(event.asInterface()));
        return returnValue;
    }

    /**
     * Remove dates from ID
     * @param {string} id
     * @returns {string}
     */
    public static filterID(id: string): string {
        const suffix = TrestleIndividual.extractSuffix(id);
        const idStrings = suffix.split(":");
        // return idStrings[0] + ":" + idStrings[1];
        return idStrings[0];
    }

    /**
     * Filter ID string to remove hostname (authority)
     * @param {string} id
     * @returns {string}
     */
    public static withoutHostname(id: string): string {
        // Manually reset regex match, because Javascript
        TrestleIndividual.hostnameRegex.lastIndex = 0;
        return id.replace(TrestleIndividual.hostnameRegex, "");
    }

    /**
     * Get the URI hostname
     * Returns an empty string if nothing matches
     * @param {string} id
     * @returns {string}
     */
    public static extractHostname(id: string): string {
        // Manually reset regex match, because Javascript
        TrestleIndividual.hostnameRegex.lastIndex = 0;
        const matches = id.match(TrestleIndividual.hostnameRegex);
        if (matches !== null) {
            return matches[0];
        }
        return "";
    }


    /**
     * Extracts the suffix from the individual, or returns an empty string
     * @param {string} id
     * @returns {string}
     */
    public static extractSuffix(id: string): string {
        // Manually reset regex match, because Javascript
        TrestleIndividual.suffixRegex.lastIndex = 0;
        const matches = TrestleIndividual.suffixRegex.exec(id);
        if (matches !== null) {
            return matches[1];
        }
        return "";
    }

    /**
     * Extracts the prefix from the individual, or returns an empty string
     * @param {string} id
     * @returns {string}
     */
    public static extractPrefix(id: string): string {
        // Manually reset regex match, because Javascript
        TrestleIndividual.prefixRegex.lastIndex = 0;
        const matches = id.match(TrestleIndividual.prefixRegex);
        if (matches !== null) {
            return matches[0];
        }
        return "";
    }

    /**
     * SDBM algorithm for generating a numeric value of a provided string
     * @param {string} id
     * @returns {number}
     */
    public static hashID(id: string): number {
        let hash = 0;
        for (let i = 0; i < id.length; i++) {
            const char = id.charCodeAt(i);
            // eslint-disable-next-line no-bitwise
            hash = char + (hash << 6) + (hash << 16) - hash;
        }
        return hash;
    }

    /**
     * Sorter for the Fact array which sorts by ID -> validFrom -> dbFrom
     *
     * @param {TrestleFact} a
     * @param {TrestleFact} b
     * @returns {number}
     */
    private static factSort(a: TrestleFact, b: TrestleFact): number {
        const idCompare = a.getID().localeCompare(b.getID());
        // If they're not equal, return
        if (idCompare !== 0) {
            return idCompare;
        }
        //    Next, compare on valid from
        const vEqual = a.getValidTemporal().getFrom().isSame(b.getValidTemporal().getFrom());
        // If they're not equal, is A before?
        if (!vEqual) {
            return a.getValidTemporal().getFrom().isBefore(b.getValidTemporal().getFrom()) ? -1 : 1;
        }
        //    Finally, db time
        const dEqual = a.getDatabaseTemporal().getFrom().isSame(b.getDatabaseTemporal().getFrom());
        if (!dEqual) {
            return a.getDatabaseTemporal().getFrom().isBefore(b.getDatabaseTemporal().getFrom()) ? -1 : 1;
        }
        return 0;
    }
}
