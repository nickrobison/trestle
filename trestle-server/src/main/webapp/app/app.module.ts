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

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        MaterialModule.forRoot(),
        RouterModule.forRoot(AppRoutes)
    ],
    declarations: [AppComponent, LoginComponent, MaterializeDirective],
    bootstrap: [AppComponent]

})
export class AppModule {}
