/**
 * Created by nrobison on 1/18/17.
 */
import {Component, OnInit} from "@angular/core";
import {AuthService} from "../authentication.service";
import {Router} from "@angular/router";
import * as CryptoJS from "crypto-js";
import {Observable, BehaviorSubject, Subject} from "rxjs";

@Component({
    selector: "admin-root",
    templateUrl: "./admin.component.html",
    styleUrls: ["./admin.component.css"],
    // styleUrls: ["../../theme.scss", "./admin.component.css"],
    // encapsulation: ViewEncapsulation.None
})

export class AdminComponent implements OnInit {

    public sideNavOpen: boolean;
    userLoggedIn: Subject<boolean> = new BehaviorSubject<boolean>(false);
    private gravatarURL: string;
    constructor(private authService: AuthService, private router: Router) {}

    ngOnInit(): void {
        this.sideNavOpen = true;
        console.debug("Init check for logged in");
        this.userLoggedIn.next(this.authService.loggedIn());
    }

    public login(): void {
        if (!this.userLoggedIn) {
            this.router.navigate(["/login"]);
        }
    }

    public getGravatarURL(): string {
        if (this.gravatarURL == null) {
            let user = this.authService.getUser();
            if (user != null) {
                let hash = CryptoJS.MD5(user.email.trim().toLowerCase()).toString();
                this.gravatarURL = "https://www.gravatar.com/avatar/" + hash + "?d=identicon" + "&s=50";
                return this.gravatarURL;
            }
        }
        return this.gravatarURL;
    }

    public logout(): void {
        this.authService.logout();
        this.userLoggedIn.next(false);
    }
}