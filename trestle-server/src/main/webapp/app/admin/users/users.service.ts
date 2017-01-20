/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Response} from "@angular/http";
import {Observable} from "rxjs";
import {AuthHttp} from "angular2-jwt";

export interface ITrestleUser {
    id: string;
    firstName: string;
    lastName: string;
    username: string;
    email: string;
}

@Injectable()
export class UserService {
    constructor(private authHttp: AuthHttp) {}

    getUsers(): Observable<Array<ITrestleUser>> {
        return this.authHttp.get("/users")
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}