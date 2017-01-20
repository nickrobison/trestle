/**
 * Created by nrobison on 1/17/17.
 */
import {NgModule} from "@angular/core";
import {HttpModule} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {MaterialModule} from "@angular/material";
import {RouterModule} from "@angular/router";
import {AppRoutes} from "./app.routes";
import {LoginComponent} from "./login/app.login";
import {AppComponent} from "./app.component";
import {MaterializeDirective} from "angular2-materialize";
import {FormsModule} from "@angular/forms";
import {AuthService} from "./authentication.service";
import {AuthGuard} from "../AuthGuard";

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        FormsModule,
        MaterialModule.forRoot(),
        RouterModule.forRoot(AppRoutes)
    ],
    declarations: [AppComponent, LoginComponent, MaterializeDirective],
    providers: [AuthService, AuthGuard],
    bootstrap: [AppComponent]

})
export class AppModule {}
