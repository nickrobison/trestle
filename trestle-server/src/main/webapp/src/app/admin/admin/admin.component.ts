/**
 * Created by nrobison on 1/18/17.
 */
import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import * as CryptoJS from "crypto-js";
import {BehaviorSubject, Subject} from "rxjs";
import {AuthService} from '../../user/authentication.service';

@Component({
    selector: "admin-root",
    templateUrl: "./admin.component.html",
    styleUrls: ["./admin.component.scss"],
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

    /**
     * If the user is not logged in, send them to the login page
     */
    public login(): void {
        if (!this.userLoggedIn) {
            this.router.navigate(["/login"]);
        }
    }

    /**
     * Get the Gravitar URL for the logged in user
     * @returns {string}
     */
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

    /**
     * Logout the user
     */
    public logout(): void {
        this.authService.logout();
        this.userLoggedIn.next(false);
    }
}
