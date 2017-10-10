import { IInterfacable } from "../../interfacable";
import { ISO_8601, Moment } from "moment";
import moment = require("moment");

export interface ITrestleTemporal {
    validID: string;
    validFrom: Date;
    validTo?: Date;
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
