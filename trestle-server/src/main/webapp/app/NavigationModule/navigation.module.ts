/**
 * Created by nrobison on 5/10/17.
 */
import {Route} from "@angular/router";
import {NgModule} from "@angular/core";
import {MaterialModule} from "@angular/material";
import {UserModule} from "../UserModule/user.module";
import {CommonModule} from "@angular/common";
import {LoginComponent} from "./login/app.login";
import {FormsModule, ReactiveFormsModule} from "@angular/forms"
import {Privileges} from "../UserModule/authentication.service";

export interface ITrestleRoute extends Route {
    data?: ITrestleRouteData
}

export interface ITrestleRouteData {
    roles: Array<Privileges>;
}

@NgModule({
    declarations: [
        LoginComponent
    ],
    imports: [
        MaterialModule,
        UserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
    ],
    exports: [LoginComponent]
})

export class NavigationModule {}
