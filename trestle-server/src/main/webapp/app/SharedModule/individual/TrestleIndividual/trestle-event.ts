import { IInterfacable } from "../../interfacable";
import * as moment from "moment";

export interface ITrestleEvent {
    individual: string;
    type: string;
    temporal: string;
}

export enum TrestleEventType {
    CREATED,
    DESTROYED,
    BECAME,
    SPLIT,
    MERGED
}

export class TrestleEvent implements IInterfacable<ITrestleEvent> {
    private individual: string;
    private type: string;
    private temporal: moment.Moment;

    constructor(event: ITrestleEvent) {
        this.individual = event.individual;
        this.type = event.type;
        this.temporal = moment(event.temporal, moment.ISO_8601);
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