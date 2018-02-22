/**
 * Created by nrobison on 1/19/17.
 */
import { Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { Http, URLSearchParams, Response } from "@angular/http";
import { tokenNotExpired, JwtHelper } from "angular2-jwt";
import { Observable } from "rxjs";

// const _key: string = "id_token";
const _key: string = "token";

export enum Privileges {
    USER = 1,
    ADMIN = 2,
    DBA = 4
}

export class TrestleToken {

    private exp: number;
    private iat: number;
    private user: ITrestleUser;

    public constructor(token: any) {
        this.exp = token["exp"];
        this.iat = token["iat"];
        this.user = JSON.parse(token["data4j"]);
    }

    public getExpiration() {
        return this.exp;
    }

    public getIssue() {
        return this.iat;
    }

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

    private jwtHelper: JwtHelper;

    constructor(private router: Router, private http: Http) {
        this.jwtHelper = new JwtHelper();
    }

    public login(username: string, password: string): Observable<void> {
        return this.http.post("/auth/login", {username: username, password: password})
            .map((response) => {
                console.debug("has token");
                console.log(response);
                localStorage.setItem(_key, response.text());
            }, (error: Error) => console.log(error));
    }

    public logout(): void {
        if (this.loggedIn()) {
            this.http.post("/auth/logout", null);
            localStorage.removeItem(_key);
            console.debug("Logged out user");
            this.router.navigate(["/"]);
        }
    }

    public loggedIn(): boolean {
        const token = localStorage.getItem(_key);
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
            const token = this.getToken();
            // tslint:disable-next-line:no-bitwise
            return (token !== null) && ((token.getUser().privileges & Privileges.ADMIN) > 0);
        }
        return false;
    }

    public getUser(): ITrestleUser | null {
        if (this.loggedIn()) {
            const token = this.getToken();
            if (token) {
                return token.getUser();
            }
        }
        console.error("User is not logged in");
        return null;
    }

    /**
     * Determines if the logged-in user has the necessary roles to perform a certain function
     * @param roles - Array of required Privileges
     * @param {ITrestleUser} user - user object to verify permissions on
     * @returns {boolean} - has all the required roles
     */
    public hasRequiredRoles(roles: Privileges[], user?: ITrestleUser | null): boolean {
        let userPrivs = 0;
        // if the user is null, get the token
        if (user == null) {
            const token = this.getToken();
            if (token == null) {
                return false;
            }
            userPrivs = token.getUser().privileges;
        } else {
            userPrivs = user.privileges;
        }
        // tslint:disable-next-line:no-bitwise
        return (userPrivs & this.buildRoleValue(roles)) > 0;
    }

    private buildRoleValue(roles: Privileges[]): number {
        let roleValue = 0;
        roles.forEach((role) => {
            // tslint:disable-next-line:no-bitwise
            roleValue = roleValue | role;
        });
        return roleValue;
    }

    private getToken(): TrestleToken | null {
        const jwtToken = localStorage.getItem(_key);
        if (jwtToken) {
            return new TrestleToken(this.jwtHelper.decodeToken(jwtToken));
        }
        return null;
    }
}
