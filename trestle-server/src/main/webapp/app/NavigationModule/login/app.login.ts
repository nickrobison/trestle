/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {AuthService} from "../../UserModule/authentication.service";

@Component({
    selector: "login",
    templateUrl: "./app.login.html",
    styleUrls: ["./app.login.scss"]
})

export class LoginComponent implements OnInit {
    public username: string;
    public password: string;
    returnUrl: string;
    constructor(private authService: AuthService, private route: ActivatedRoute, private router: Router) {}

    ngOnInit(): void {
        this.authService.logout();
        this.returnUrl = this.route.snapshot.queryParams["returnUrl"] || "/";
    }

    public login() {
        console.debug("Logging in with", this.username, "and", this.password);
        this.authService.login(this.username, this.password).subscribe((data: any) => {
            this.router.navigate([this.returnUrl]);
        });
    }
}