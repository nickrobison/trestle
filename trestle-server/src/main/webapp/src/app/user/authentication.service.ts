/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from '@angular/core';
import {Router} from '@angular/router';
import {Privileges, TrestleUser} from './trestle-user';
import {HttpClient} from '@angular/common/http';
import {JwtHelperService} from '@auth0/angular-jwt';
import {environment} from '../../environments/environment';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

const _key: string = 'access_token';

export class TrestleToken {

  private readonly exp: number;
  private readonly iat: number;
  private readonly user: ITrestleUser;

  public constructor(token: any) {
    this.exp = token['exp'];
    this.iat = token['iat'];
    this.user = JSON.parse(token['data4j']);
  }

  /**
   * Get token expiration time (in ms from Unix epoch)
   * @returns {number}
   */
  public getExpiration() {
    return this.exp;
  }

  /**
   * Get issuance time (in ms from Unix epoch)
   * @returns {number}
   */
  public getIssue() {
    return this.iat;
  }

  /**
   * Get the user from the token
   * @returns {ITrestleUser}
   */
  public getUser() {
    return this.user;
  }
}

export interface ITrestleUser {
  id?: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  privileges: number;
}

@Injectable()
export class AuthService {

  private jwtHelper: JwtHelperService;
  private readonly baseUrl;

  constructor(private router: Router, private http: HttpClient) {
    this.jwtHelper = new JwtHelperService();
    this.baseUrl = environment.baseUrl;
  }

  /**
   * Attempt to login the given use
   * @param {string} username
   * @param {string} password
   */
  public login(username: string, password: string): Observable<{ user: TrestleUser, token: string }> {
    return this.http.post(this.baseUrl + '/auth/login', {username, password: password}, {
      responseType: 'text'
    })
      .pipe(map(token => {
        const t = this.getToken2(token);
        return {user: new TrestleUser(t.getUser()), token}
      }));
  }

  /**
   * Logout the user
   */
  public logout(): Observable<string> {
    console.debug("Logging out");
    return this.http.post(this.baseUrl + '/auth/logout', null, {
      responseType: 'text'
    });
  }

  /**
   * Is the user currently logged in?
   * @returns {boolean}
   */
  public loggedIn(): boolean {
    const token = localStorage.getItem(_key);
    if (token) {
      console.debug(
        this.jwtHelper.decodeToken(token),
        this.jwtHelper.getTokenExpirationDate(token),
        this.jwtHelper.isTokenExpired(token)
      );
    }
    return !this.jwtHelper.isTokenExpired(token);
  }

  /**
   * Does the user have AT LEAST Admin permissions?
   * @returns {boolean}
   */
  public isAdmin(): boolean {
    if (this.loggedIn()) {
      const token = this.getToken();
      // tslint:disable-next-line:no-bitwise
      return (token !== null) && ((token.getUser().privileges & Privileges.ADMIN) > 0);
    }
    return false;
  }

  /**
   * Get the user, if one exists
   * @returns {TrestleUser | null}
   */
  public getUser(): TrestleUser | null {
    if (this.loggedIn()) {
      const token = this.getToken();
      if (token) {
        return new TrestleUser(token.getUser());
      }
    }
    console.error('User is not logged in');
    return null;
  }

  /**
   * Determines if the logged-in user has the necessary roles to perform a certain function
   * @param roles - Array of required Privileges
   * @returns {boolean} - has all the required roles
   */
  public hasRequiredRoles(roles: Privileges[]): boolean {
    const user = this.getUser();
    if (user == null) {
      return false;
    }

    return user.hasRequiredPrivileges(roles);
  }

  public getEncodedToken(): string | null {
    return localStorage.getItem(_key);
  }

  private getToken(): TrestleToken | null {
    const jwtToken = this.getEncodedToken();
    if (jwtToken) {
      return new TrestleToken(this.jwtHelper.decodeToken(jwtToken));
    }
    return null;
  }

  private getToken2 = (jwtToken: string): TrestleToken => {
    return new TrestleToken(this.jwtHelper.decodeToken(jwtToken));
  };
}
