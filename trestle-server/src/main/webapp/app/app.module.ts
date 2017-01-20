/**
 * Created by nrobison on 1/17/17.
 */
import {NgModule} from "@angular/core";
import {HttpModule, RequestOptions, Http} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {MaterialModule} from "@angular/material";
import {RouterModule} from "@angular/router";
import {AppRoutes} from "./app.routes";
import {LoginComponent} from "./login/app.login";
import {AppComponent} from "./app.component";
import {MaterializeDirective} from "angular2-materialize";
import {FormsModule} from "@angular/forms";
import {AuthService} from "./authentication.service";
import {LoggedInGuard} from "./LoggedInGuard";
import {AuthHttp, AuthConfig} from "angular2-jwt";
import {PermissionsGuard} from "./PermissionsGuard";

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        FormsModule,
        MaterialModule.forRoot(),
        RouterModule.forRoot(AppRoutes)
    ],
    declarations: [AppComponent, LoginComponent, MaterializeDirective],
    providers: [AuthService,
        LoggedInGuard,
        PermissionsGuard,
        {
            provide: AuthHttp,
            useFactory: authHttpServiceFactory,
            deps: [Http, RequestOptions]
        }],
    bootstrap: [AppComponent]

})
export class AppModule {}

export function authHttpServiceFactory(http: Http, options: RequestOptions) {
    return new AuthHttp(new AuthConfig({
        noTokenScheme: true
    }), http, options);
}
