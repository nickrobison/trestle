import { IInterfacable } from "../../interfacable";
import * as moment from "moment";

export interface ITrestleTemporal {
    validID: string;
    validFrom: Date;
    validTo?: Date;
}

export class TrestleTemporal implements IInterfacable<ITrestleTemporal> {
    private id: string;
    private from: moment.Moment;
    private to?: moment.Moment;

    constructor(temporal: ITrestleTemporal) {
        this.id = temporal.validID;
        this.from = moment(temporal.validFrom, moment.ISO_8601);
        if (temporal.validTo !== null) {
            this.to = moment.utc(temporal.validTo, moment.ISO_8601);
        }
    }

    public getID(): string {
        return this.id;
    }

    public getFrom(): moment.Moment {
        return this.from;
    }

    public getTo(): moment.Moment {
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
