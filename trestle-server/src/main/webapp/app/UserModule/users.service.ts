/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Response} from "@angular/http";
import {Observable} from "rxjs";
import {ITrestleUser} from "./authentication.service";
import {TrestleHttp} from "./trestle-http.provider";

@Injectable()
export class UserService {
    constructor(private trestleHttp: TrestleHttp) {}

    /**
     * Get a list of all registered users
     * Requires ADMIN permissions
     * @returns {Observable<R>}
     */
    getUsers(): Observable<Array<ITrestleUser>> {
        return this.trestleHttp.get("/users")
            .map((res: Response) => res.json())
            .catch((error: Error) => Observable.throw(error || "Server Error"));
    }

    /**
     * Sends an {@link ITrestleUser} object to the server
     * Either creates the new user or modifies an existing one
     * @param user - User to create/modify
     * @returns {Observable<R>}
     */
    modifyUser(user: ITrestleUser): Observable<any> {
        // If the user ID is null, we know it's a new user, so we create a new one, otherwise, modify the old one
        // if (user.id == null) {
            return this.trestleHttp.post("/users", user)
            // .map((res: Response) => {})
                .catch((error: Error) => Observable.throw(error || "Error adding user"));
        // } else {
        //     return this.trestleHttp.post("/users/" + user.id, user, options)
        //         .catch((error: Error) => Observable.throw(error || "Error modifying user"));
        // }
    }

    /**
     * Delete user from database
     * @param id - ID of user to delete
     * @returns {Observable<R>}
     */
    deleteUser(id: number): Observable<any> {
        return this.trestleHttp.delete("/users/" + id)
            .catch((error: Error) => Observable.throw(error || "Error deleting user"));
    }
}