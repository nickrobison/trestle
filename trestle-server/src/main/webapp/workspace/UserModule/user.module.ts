/**
 * Created by nrobison on 5/12/17.
 */
import {NgModule} from "@angular/core";
import {Http, RequestOptions} from "@angular/http";
import {AuthService} from "./authentication.service";
import {LoggedInGuard} from "./LoggedInGuard";
import {PermissionsGuard} from "./PermissionsGuard";
import {Router} from "@angular/router";
import {TrestleHttp} from "./trestle-http.provider";
import {CommonModule} from "@angular/common";
import {UserService} from "./users.service";
import {DefaultRouteGuard} from "./DefaultRouteGuard";
import {MaterialModule} from "../MaterialModule/material.module";
import {HttpClientModule} from "@angular/common/http";

@NgModule({
    imports: [
        HttpClientModule,
        CommonModule,
        MaterialModule
    ],
    providers: [
        AuthService,
        LoggedInGuard,
        PermissionsGuard,
        DefaultRouteGuard,
        UserService,
        {
            provide: TrestleHttp,
            useFactory: TrestleHttp.factory,
            deps: [Http, RequestOptions, Router]
        }

    ]
})

export class UserModule {
}
