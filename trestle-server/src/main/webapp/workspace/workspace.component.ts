/**
 * Created by nrobison on 1/19/17.
 */
import { Component, HostListener, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from "@angular/core";
import { BehaviorSubject } from "rxjs/BehaviorSubject";
import { Subject } from "rxjs/Subject";
import { Router } from "@angular/router";
import { AuthService, Privileges } from "./UserModule/authentication.service";
import * as CryptoJS from "crypto-js";
import { EventBus, UserLoginEvent } from "./UIModule/eventBus/eventBus.service";
import { Subscription } from "rxjs/Subscription";
import { MatSidenav } from "@angular/material";

@Component({
    selector: "app-root",
    templateUrl: "./workspace.component.html",
    styleUrls: ["../theme.scss", "./workspace.component.css"],
    encapsulation: ViewEncapsulation.None
})
export class WorkspaceComponent implements OnInit, OnDestroy {
    public userLoggedIn: Subject<boolean> = new BehaviorSubject<boolean>(false);
    public gravatarURL: string;
    // We need this in order to access the Privileges enum from the template
    public Privileges = Privileges;

    @ViewChild("sidenav") public sideNav: MatSidenav;
    private loginSubscription: Subscription;

    constructor(private authService: AuthService,
                private router: Router,
                private eventBus: EventBus) {
    }

    public ngOnInit(): void {
        this.checkMenu();
        this.loginSubscription = this.eventBus.subscribe(UserLoginEvent).subscribe((event) => {
            console.debug("User event, is logged in?", event.isLoggedIn());
        });
        this.userLoggedIn.next(this.authService.loggedIn());
    }

    public ngOnDestroy(): void {
        this.loginSubscription.unsubscribe();
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

//    Resize function for sidenav
    @HostListener("window:resize", ["$event"])
    public onResize(event: any): void {
        this.checkMenu();
    }

    private checkMenu() {
        if (window.innerWidth <= 800) {
            this.sideNav.close();
        } else {
            this.sideNav.open();
        }
    }
}
