/**
 * Created by nrobison on 1/17/17.
 */
import {NgModule} from "@angular/core";
import {HttpModule} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {RouterModule} from "@angular/router";
import {AppRoutes} from "./app.routes";
import {AppComponent} from "./app.component";
import {MaterializeModule} from "angular2-materialize";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {UserModule} from "./UserModule/user.module";
import {NavigationModule} from "./NavigationModule/navigation.module";
import {UIModule} from "./UIModule/ui.module";
import {MaterialModule} from "./MaterialModule/material.module";
import {SharedModule} from "./SharedModule/shared.module";

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        BrowserAnimationsModule,
        RouterModule.forRoot(AppRoutes),
        MaterialModule,
        MaterializeModule,
        UserModule,
        NavigationModule,
        UIModule,
        SharedModule
    ],
    declarations: [AppComponent],
    bootstrap: [AppComponent]

})
export class AppModule {
}
