/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Http, URLSearchParams, Response} from "@angular/http";
import {tokenNotExpired, JwtHelper} from "angular2-jwt";
import {Observable} from "rxjs";

// const _key: string = "id_token";
const _key: string = "token";

export enum Privileges {
    USER = 1,
    ADMIN = 2,
    DBA = 4
}

export class TrestleToken {

    exp: number;
    iat: number;
    user: ITrestleUser;

    public constructor(token: any) {
        this.exp = token["exp"];
        this.iat = token["iat"];
        this.user = JSON.parse(token["data4j"]);
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

    private jwtHelper: JwtHelper;

    constructor(private router: Router, private http: Http) {
        this.jwtHelper = new JwtHelper();
    }

    public login(username: string, password: string): Observable<void> {
        return this.http.post("/auth/login", {username: username, password: password})
            .map(response => {
                console.debug("has token");
                console.log(response);
                localStorage.setItem(_key, response.text());
            }, (error: Error) => console.log(error))
    }

    public logout(): void {
        if (this.loggedIn()) {
            console.debug("Logging out");
            this.http.post("/auth/logout", null);
            localStorage.removeItem(_key);
            console.log("Logged out user");
            this.router.navigate(["/"]);
        }
    }

    public loggedIn(): boolean {
        let token = localStorage.getItem(_key);
        console.debug("Is user logged in?");
        console.debug("has token", token);
        if (token) {
            console.debug(
                this.jwtHelper.decodeToken(token),
                this.jwtHelper.getTokenExpirationDate(token),
                this.jwtHelper.isTokenExpired(token),
                tokenNotExpired()
            );
        }
        return tokenNotExpired();
    }

    public isAdmin(): boolean {
        if (this.loggedIn()) {
            let token = this.getToken();
            return (token.user.privileges & Privileges.ADMIN) > 0
        }
        return false;
    }

    public getUser(): ITrestleUser {
        if (this.loggedIn()) {
            let token = this.getToken();
            return token.user;
        }
        console.error("User is not logged in");
        return null;
    }

    /**
     * Determines if the logged-in user has the necessary roles to perform a certain function
     * @param roles - Array of required Privileges
     * @returns {boolean} - has all the required roles
     */
    public hasRequiredRoles(roles: Array<Privileges>): boolean {
        let token = this.getToken();
        if (token == null) {
            return false;
        }

        if (token) {
            return (token.user.privileges & this.buildRoleValue(roles)) > 0;
        }
        return false;
    }

    private buildRoleValue(roles: Array<Privileges>): number {
        let roleValue = 0;
        roles.forEach((role) => {
            roleValue = roleValue | role;
        });
        return roleValue;
    }

    private getToken(): TrestleToken {
        let jwtToken = localStorage.getItem(_key);
        if (jwtToken == null) {
            return null;
        }
        return new TrestleToken(this.jwtHelper.decodeToken(jwtToken));
    }


}