/**
 * Created by nrobison on 6/27/17.
 */
import { Injectable } from "@angular/core";
import { Subject } from "rxjs/Subject";
import { Observable } from "rxjs/Observable";
import { ReplaySubject } from "rxjs/ReplaySubject";

interface IMessage {
    channel: Function;
    data: any;
}

export class UserLoginEvent {
    private loggedIn: boolean;
    constructor(loggedIn: boolean) {
        this.loggedIn = loggedIn;
    }

    public isLoggedIn(): boolean {
        return this.loggedIn;
    }
}

type Constructable<T> = new (...args: any[]) => T;

@Injectable()
export class EventBus {

    private messages: Subject<IMessage>;

    constructor() {
        this.messages = new ReplaySubject<IMessage>();
    }

    public publish<T>(message: T): void {
        const channel = message.constructor;
        this.messages.next({
            channel: channel,
            data: message
        });
    }

    public subscribe<T>(messageClass: Constructable<T>): Observable<T> {
        return this.messages.filter((m) => m.channel === messageClass).map((m) => m.data);
    }
}