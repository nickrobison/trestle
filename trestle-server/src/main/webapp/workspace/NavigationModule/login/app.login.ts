/**
 * Created by nrobison on 1/19/17.
 */
import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { AuthService } from "../../UserModule/authentication.service";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { state, style, transition, trigger, animate } from "@angular/animations";
import { EventBus, UserLoginEvent } from "../../UIModule/eventBus/eventBus.service";

interface IUserLogin {
    username: string;
    password: string;
}

@Component({
    selector: "login",
    templateUrl: "./app.login.html",
    styleUrls: ["./app.login.scss"],
    animations: [
        trigger("errorMessage", [
            state("inactive", style({
                backgroundColor: "white"
            })),
            state("active", style({
                backgroundColor: "red"
            })),
            transition("inactive => active", animate("100ms ease-in")),
            transition("active => inactive", animate("100ms ease-out"))
        ])
    ]
})

export class LoginComponent implements OnInit {

    public backgroundImage: any = require("../../../public/images/trestle-logo.jpg");
    public loginForm: FormGroup;
    public errorMessage: string;
    public errorState: string;
    private returnUrl: string;

    constructor(private fb: FormBuilder, private authService: AuthService, private route: ActivatedRoute, private router: Router, private eventBus: EventBus) {
    }

    public ngOnInit(): void {

        this.authService.logout();
        this.returnUrl = this.route.snapshot.queryParams["returnUrl"] || "/";
        this.loginForm = this.fb.group({
            username: [null, Validators.required],
            password: [null, Validators.required]
        });
        this.errorMessage = "";
        this.errorState = "inactive";
    }

    /**
     * Attempt to login the given user
     * @param {IUserLogin} user
     */
    public login(user: IUserLogin) {
        console.debug("Logging in with", user.username, "and", user.password);
        this.authService.login(user.username, user.password).subscribe((data: any) => {
            this.eventBus.publish(new UserLoginEvent(true));
            this.router.navigate([this.returnUrl]);
        }, (error: Response) => {
            console.error("Error logging in: ", error.status);
            if (error.status == 401) {
                this.errorState = "active";
                this.errorMessage = "Incorrect Username or Password";
            }
        });
    }
}