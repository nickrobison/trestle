/**
 * Created by nrobison on 1/18/17.
 */
import {Component, ViewEncapsulation, OnInit} from "@angular/core";
import {AuthService} from "../authentication.service";
import {Router} from "@angular/router";

@Component({
    selector: "admin-root",
    templateUrl: "./admin.component.html",
    styleUrls: ["./admin.component.css"],
    // styleUrls: ["../../theme.scss", "./admin.component.css"],
    // encapsulation: ViewEncapsulation.None
})

export class AdminComponent implements OnInit {

    public sideNavOpen: boolean;
    private userLoggedIn = false;
    constructor(private authService: AuthService, private router: Router) {}

    ngOnInit(): void {
        this.sideNavOpen = true;
        this.userLoggedIn = this.authService.loggedIn();
    }

    public login(): void {
        if (!this.userLoggedIn) {
            this.router.navigate(["/login"]);
        }
    }

    public logout(): void {
        this.authService.logout();
    }
}