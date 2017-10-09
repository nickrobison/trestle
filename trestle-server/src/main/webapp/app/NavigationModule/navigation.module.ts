/**
 * Created by nrobison on 5/10/17.
 */
import {Route} from "@angular/router";
import {NgModule} from "@angular/core";
import {UserModule} from "../UserModule/user.module";
import {CommonModule} from "@angular/common";
import {LoginComponent} from "./login/app.login";
import {FormsModule, ReactiveFormsModule} from "@angular/forms"
import {Privileges} from "../UserModule/authentication.service";
import { NavComponent } from "./nav.component";
import { UIModule } from "../UIModule/ui.module";
import { MaterialModule } from "../MaterialModule/material.module";

export interface ITrestleRoute extends Route {
    data?: ITrestleRouteData
}

export interface ITrestleRouteData {
    roles: Privileges[];
}

@NgModule({
    declarations: [
        LoginComponent,
        NavComponent
    ],
    imports: [
        MaterialModule,
        UserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        UIModule
    ],
    exports: [LoginComponent, NavComponent]
})

export class NavigationModule {}
