/**
 * Created by nrobison on 1/19/17.
 */
import { Component, HostListener, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from "@angular/core";
import { Router } from "@angular/router";
import { AuthService, Privileges } from "./UserModule/authentication.service";
import { MD5 } from "crypto-js";
import { EventBus, UserLoginEvent } from "./UIModule/eventBus/eventBus.service";
import { Subscription } from "rxjs/Subscription";
import { MatSidenav } from "@angular/material";
import { TrestleUser } from "./UserModule/trestle-user";

@Component({
    selector: "app-root",
    templateUrl: "./workspace.component.html",
    styleUrls: ["../theme.scss", "./workspace.component.css"],
    encapsulation: ViewEncapsulation.None
})
export class WorkspaceComponent implements OnInit, OnDestroy {
    public gravatarURL: string;
    // We need this in order to access the Privileges enum from the template
    public Privileges = Privileges;

    @ViewChild("sidenav") public sideNav: MatSidenav;
    private loginSubscription: Subscription;
    private user: TrestleUser | null;

    constructor(private authService: AuthService,
                private router: Router,
                private eventBus: EventBus) {
    }

    public ngOnInit(): void {
        // Get the current user, if it exists
        this.user = this.authService.getUser();
        this.checkMenu();
        this.loginSubscription = this.eventBus.subscribe(UserLoginEvent).subscribe((event) => {
            console.debug("User event, is logged in?", event.isLoggedIn());
            if (event.isLoggedIn()) {
                this.user = this.authService.getUser();
            } else {
                this.user = null;
            }
        });
    }

    public ngOnDestroy(): void {
        this.loginSubscription.unsubscribe();
    }

    public login(): void {
        if (this.user == null) {
            this.router.navigate(["/login"]);
        }
    }

    public userHasRequiredPermissions(requiredPrivs: Privileges[]): boolean {
        if (this.user == null) {
            return false;
        }
        return this.user.hasRequiredPrivileges(requiredPrivs);
    }

    public getGravatarURL(): string {
        if (this.gravatarURL == null) {
            const user = this.authService.getUser();
            if (user !== null) {
                const hash = MD5(user.email.trim().toLowerCase()).toString();
                this.gravatarURL = "https://www.gravatar.com/avatar/" + hash + "?d=identicon" + "&s=36";
                return this.gravatarURL;
            }
        }
        return this.gravatarURL;
    }

    public logout(): void {
        this.authService.logout();
        this.user = null;
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
