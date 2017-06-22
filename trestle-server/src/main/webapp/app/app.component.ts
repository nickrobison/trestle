/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit, ViewEncapsulation} from "@angular/core";
import {BehaviorSubject} from "rxjs/BehaviorSubject";
import {Subject} from "rxjs/Subject";
import {Router} from "@angular/router";
import {AuthService, Privileges} from "./UserModule/authentication.service";
import * as CryptoJS from "crypto-js";

@Component({
    selector: "app-root",
    templateUrl: "./app.component.html",
    styleUrls: ["../theme.scss", "./app.component.css"],
    encapsulation: ViewEncapsulation.None
})

export class AppComponent implements OnInit {
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