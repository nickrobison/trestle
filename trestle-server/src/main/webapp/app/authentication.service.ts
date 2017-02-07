/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Http, URLSearchParams, Response} from "@angular/http";
import {tokenNotExpired, JwtHelper} from "angular2-jwt";
import {Observable} from "rxjs";

const _key: string = "id_token";

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
        console.debug("Logging out");
        this.http.post("/auth/logout", null);
        localStorage.removeItem(_key);
        console.log("Logged out user");
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

    public getUser(): ITrestleUser {
        if (this.loggedIn()) {
            let token = new TrestleToken(this.jwtHelper.decodeToken(localStorage.getItem(_key)));
            return token.user;
        }
        console.error("User is not logged in");
        return null;
    }

    public hasRoles(roles: Array<Privileges>): boolean {
        let token = new TrestleToken(this.jwtHelper.decodeToken(localStorage.getItem(_key)));
        console.debug("Role token", token);

        if (token) {
            return (token.user.privileges & this.buildRoleValue(roles)) > 0;
        }

        return false;
    }

    private buildRoleValue(roles: Array<Privileges>): number {
        let roleValue = 0;
        roles.forEach((role) => {
            console.log(Privileges[role]);
            roleValue = roleValue | role;
        });
        console.debug("Role value", roleValue);
        return roleValue;
    }


}