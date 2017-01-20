/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Http, URLSearchParams, Response} from "@angular/http";
import {tokenNotExpired, JwtHelper} from "angular2-jwt";

const _key: string = "id_token";

@Injectable()
export class AuthService {

    private jwtHelper: JwtHelper;

    constructor(private router: Router, private http: Http) {
        this.jwtHelper = new JwtHelper();
    }

    public login(username: string, password: string): void {
        this.http.post("/auth/login", {username: username, password: password})
            .subscribe(response => {
            console.debug("has token");
            console.log(response.text());
            localStorage.setItem(_key, response.text());
        }, (error: Error) => console.log(error))
    }

    public logout(): void {
        console.debug("Logging out");
        this.http.post("/auth/logout", null);
        localStorage.removeItem(_key);
        console.log("Logged out use");
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
}