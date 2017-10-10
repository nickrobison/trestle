import { ITrestleTemporal, TrestleTemporal } from "./trestle-temporal";
import { IInterfacable } from "../../interfacable";

export interface ITrestleFact {
    identifier: string;
    name: string;
    type: string;
    value: string;
    databaseTemporal: ITrestleTemporal;
    validTemporal: ITrestleTemporal;
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
