/**
 * Created by nrobison on 1/19/17.
 */
import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Http, URLSearchParams, Response} from "@angular/http";
import {tokenNotExpired, JwtHelper} from "angular2-jwt";

@Injectable()
export class AuthService {

    constructor(private router: Router, private http: Http) {
    }

    public login(username: string): void {
        let params = new URLSearchParams();
        // params.append("username", {username: username});
        this.http.post("/auth/login", {username: username})
            .subscribe(response => {
            console.log("has token", response.toString());
            localStorage.setItem("id_token", response.toString());
        }, (error: Error) => console.log(error))
    }

    public logout(): void {
        console.debug("Logging out");
        this.http.post("/auth/logout", null);
        localStorage.removeItem("id_token");
        console.log("Logged out use");
    }

    public loggedIn(): boolean {
        return tokenNotExpired();
    }
}