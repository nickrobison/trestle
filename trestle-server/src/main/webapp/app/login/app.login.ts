/**
 * Created by nrobison on 1/19/17.
 */
import {Component} from "@angular/core";
import {AuthService} from "../authentication.service";

@Component({
    selector: "login",
    templateUrl: "./app.login.html",
    styleUrls: ["./app.login.scss"]
})

export class LoginComponent {
    public username: string;
    public password: string;
    constructor(private authService: AuthService) {}

    public login() {
        console.debug("Logging in with", this.username, "and", this.password);
        this.authService.login(this.username);
    }
}