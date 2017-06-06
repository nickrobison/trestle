/**
 * Created by nrobison on 1/19/17.
 */
import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {AuthService} from "../../UserModule/authentication.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";

interface IUserLogin {
    username: string;
    password: string;
}

@Component({
    selector: "login",
    templateUrl: "./app.login.html",
    styleUrls: ["./app.login.scss"]
})

export class LoginComponent implements OnInit {
    loginForm: FormGroup;
    public username: string;
    public password: string;
    returnUrl: string;
    constructor(private fb: FormBuilder, private authService: AuthService, private route: ActivatedRoute, private router: Router) {}

    ngOnInit(): void {
        this.authService.logout();
        this.returnUrl = this.route.snapshot.queryParams["returnUrl"] || "/";
        this.loginForm = this.fb.group({
            "username": [null, Validators.required],
            "password": [null, Validators.required]
        });
    }

    public login(user: IUserLogin) {
        console.debug("Logging in with", user.username, "and", user.password);
        this.authService.login(user.username, user.password).subscribe((data: any) => {
            this.router.navigate([this.returnUrl]);
        }, (error: Error) => {
            console.error("Error logging in", error);
        });
    }
}