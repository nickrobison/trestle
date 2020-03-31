import { IInterfacable } from "../../interfacable";
import * as moment from "moment";

export interface ITrestleTemporal {
    ID: string;
    From: Date;
    To?: Date | string;
}

export class TrestleTemporal implements IInterfacable<ITrestleTemporal> {
    private id: string;
    private from: moment.Moment;
    private to?: moment.Moment;

    constructor(temporal: ITrestleTemporal) {
        this.id = temporal.ID;
        this.from = moment(temporal.From, moment.ISO_8601);
        // There's a problem with our serialization, which means these things might end up as strings, which is no good.
        if (temporal.To !== null && temporal.To !== "") {
            this.to = moment.utc(temporal.To, moment.ISO_8601);
        }
    }

    public getID(): string {
        return this.id;
    }

    public getFrom(): moment.Moment {
        return this.from;
    }

    public getFromAsDate(): Date {
        return this.from.toDate();
    }

    public getTo(): moment.Moment | undefined {
        return this.to;
    }

    public getToAsDate(): Date | undefined {
        if (this.to) {
            return this.to.toDate();
        }
        return undefined;
    }

    public isContinuing(): boolean {
        return this.to === undefined || !this.to.isValid();
    }

    /**
     * Does the specified temporal fall within the interval of this TemporalObject?
     * @param {moment.Moment} temporal
     * @returns {boolean}
     */
    public isActive(temporal: moment.Moment): boolean {
        if (this.to === undefined) {
            return this.from.isSameOrBefore(temporal);
        } else {
            return this.from.isSameOrBefore(temporal) &&
                this.to.isAfter(temporal);
        }
    }

    public asInterface(): ITrestleTemporal {
        const returnValue: ITrestleTemporal = {
            ID: this.id,
            From: this.from.toDate()
        };
        if (!this.isContinuing() && (this.to !== undefined)) {
            returnValue.To = this.to.toDate();
        }
        return returnValue;
    }
}
