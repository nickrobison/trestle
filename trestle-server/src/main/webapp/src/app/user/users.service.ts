/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {ITrestleUser} from './authentication.service';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {tap} from 'rxjs/operators';

@Injectable()
export class UserService {

  private readonly baseUrl: string;

  constructor(private trestleHttp: HttpClient) {
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Get a list of all registered users
   * Requires ADMIN permissions
   *
   * @returns {Observable<ITrestleUser[]>}
   */
  public getUsers(): Observable<ITrestleUser[]> {
    console.debug("Getting users?");
    return this.trestleHttp.get<ITrestleUser[]>(this.baseUrl + '/users')
      .pipe(tap(l => console.debug("Logging:", l)));
  }

  /**
   * Sends an {@link ITrestleUser} object to the server
   * Either creates the new user or modifies an existing one
   *
   * @param user - User to create/modify
   * @returns {Observable<string>}
   */
  public modifyUser(user: ITrestleUser): Observable<string> {
    return this.trestleHttp.post(this.baseUrl + '/users', user, {
      responseType: 'text'
    });
  }

  /**
   * Delete user from database
   * @param id - ID of user to delete
   * @returns {Observable<any>}
   */
  public deleteUser(id: number): Observable<any> {
    return this.trestleHttp.delete(this.baseUrl + '/users/' + id);
  }

  /**
   * Determines whether or not a given username exists in the application
   *
   * @param {string} username
   * @returns {Observable<boolean>}
   */
  public userExists(username: string): Observable<boolean> {
    console.debug('Checking username:', username);
    return this.trestleHttp.get<boolean>(this.baseUrl + '/users/exists/' + username);
  }
}
