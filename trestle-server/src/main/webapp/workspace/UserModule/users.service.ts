/**
 * Created by nrobison on 1/19/17.
 */
import { Injectable } from "@angular/core";
import { Response } from "@angular/http";
import { Observable } from "rxjs";
import { ITrestleUser } from "./authentication.service";
import { TrestleHttp } from "./trestle-http.provider";

@Injectable()
export class UserService {
    constructor(private trestleHttp: TrestleHttp) {
    }

    /**
     * Get a list of all registered users
     * Requires ADMIN permissions
     *
     * @returns {Observable<R>}
     */
    public getUsers(): Observable<Array<ITrestleUser>> {
        return this.trestleHttp.get("/users")
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Sends an {@link ITrestleUser} object to the server
     * Either creates the new user or modifies an existing one
     *
     * @param user - User to create/modify
     * @returns {Observable<R>}
     */
    public modifyUser(user: ITrestleUser): Observable<any> {
        return this.trestleHttp.post("/users", user)
            .catch((error: Error) => Observable.throw(error || "Error adding user"));
    }

    /**
     * Delete user from database
     * @param id - ID of user to delete
     * @returns {Observable<R>}
     */
    public deleteUser(id: number): Observable<any> {
        return this.trestleHttp.delete("/users/" + id)
            .catch((error: Error) => Observable.throw(error || "Error deleting user"));
    }

    /**
     * Determines whether or not a given username exists in the application
     *
     * @param {string} username
     * @returns {Observable<boolean>}
     */
    public userExists(username: string): Observable<boolean> {
        console.debug("Checking username:", username);
        return this.trestleHttp.get("/users/exists/" + username)
            .map((response) => response.text() == "true")
            .catch((error: Error) => Observable.throw(error || "Error checking user existence"));
    }
}
