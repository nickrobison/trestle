/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Response, Headers, RequestOptions} from "@angular/http";
import {Observable} from "rxjs";
import {AuthHttp} from "angular2-jwt";
import {ITrestleUser} from "../../authentication.service";

@Injectable()
export class UserService {
    constructor(private authHttp: AuthHttp) {}

    /**
     * Get a list of all registered users
     * Requires ADMIN permissions
     * @returns {Observable<R>}
     */
    getUsers(): Observable<Array<ITrestleUser>> {
        return this.authHttp.get("/users")
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
        let headers = new Headers({"Content-Type": "application/json"});
        let options = new RequestOptions({headers: headers});
        console.debug("Adding user:", {username: user.username, password: user.password, privileges: user.privileges});
        return this.authHttp.post("/users", user, options)
            // .map((res: Response) => {})
            .catch((error: Error) => Observable.throw(error || "Error adding user"));
    }

    /**
     * Delete user from database
     * @param id - ID of user to delete
     * @returns {Observable<R>}
     */
    deleteUser(id: number): Observable<any> {
        return this.authHttp.delete("/users/" + id)
            .catch((error: Error) => Observable.throw(error || "Error deleting user"));
    }
}