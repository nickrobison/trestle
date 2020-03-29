/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {ITrestleUser} from "./authentication.service";
import {HttpClient} from "@angular/common/http";
import {map} from "rxjs/operators";

@Injectable()
export class UserService {
  constructor(private trestleHttp: HttpClient) {
  }

  /**
   * Get a list of all registered users
   * Requires ADMIN permissions
   *
   * @returns {Observable<R>}
   */
  public getUsers(): Observable<ITrestleUser[]> {
    return this.trestleHttp.get<ITrestleUser[]>("/users");
  }

  /**
   * Sends an {@link ITrestleUser} object to the server
   * Either creates the new user or modifies an existing one
   *
   * @param user - User to create/modify
   * @returns {Observable<R>}
   */
  public modifyUser(user: ITrestleUser): Observable<any> {
    return this.trestleHttp.post("/users", user);
  }

  /**
   * Delete user from database
   * @param id - ID of user to delete
   * @returns {Observable<R>}
   */
  public deleteUser(id: number): Observable<any> {
    return this.trestleHttp.delete("/users/" + id);
  }

  /**
   * Determines whether or not a given username exists in the application
   *
   * @param {string} username
   * @returns {Observable<boolean>}
   */
  public userExists(username: string): Observable<boolean> {
    console.debug("Checking username:", username);
    return this.trestleHttp.get<string>("/users/exists/" + username)
      .pipe(map(res => res === "true"));
  }
}
