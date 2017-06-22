/**
 * Created by nrobison on 5/10/17.
 */
import {Component, OnInit} from "@angular/core";
import {AuthService, Privileges} from "../UserModule/authentication.service";
import {Router} from "@angular/router";
import {BehaviorSubject} from "rxjs/BehaviorSubject";
import {Subject} from "rxjs/Subject";
import * as CryptoJS from "crypto-js";

@Component({
    selector: "navigation",
    templateUrl: "./navigation.component.html",
    styleUrls: ["./navigation.component.css"]
})

export class NavigationComponent implements OnInit {
    userLoggedIn: Subject<boolean> = new BehaviorSubject<boolean>(false);
    gravatarURL: string;
    // We need this in order to access the Privileges enum from the template
    Privileges = Privileges;

    constructor(private authService: AuthService, private router: Router) {}

    ngOnInit(): void {
        this.userLoggedIn.next(this.authService.loggedIn());
    }

    public login(): void {
        if (!this.userLoggedIn) {
            this.router.navigate(["/login"]);
        }
    }

    public userHasRequiredPermissions(requiredPrivs: Array<Privileges>) {
        return this.authService.hasRequiredRoles(requiredPrivs);
    }

    public getGravatarURL(): string {
        if (this.gravatarURL == null) {
            let user = this.authService.getUser();
            if (user != null) {
                let hash = CryptoJS.MD5(user.email.trim().toLowerCase()).toString();
                this.gravatarURL = "https://www.gravatar.com/avatar/" + hash + "?d=identicon" + "&s=36";
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