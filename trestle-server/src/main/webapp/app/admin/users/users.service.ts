/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Http, Response} from "@angular/http";
import {Observable} from "rxjs";

export interface ITrestleUser {
    id: string;
    firstName: string;
    lastName: string;
    username: string;
    email: string;
}

@Injectable()
export class UserService {
    constructor(private http: Http) {}

    getUsers(): Observable<Array<ITrestleUser>> {
        return this.http.get("/users")
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }
}