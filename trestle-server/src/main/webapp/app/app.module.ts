/**
 * Created by nrobison on 1/17/17.
 */
import {NgModule} from "@angular/core";
import {HttpModule} from "@angular/http";
import {BrowserModule} from "@angular/platform-browser";
import {MaterialModule} from "@angular/material";
import {AppComponent} from "./app.component";
import {DashboardComponent} from "./dashboard/app.dashboard";
import {RouterModule} from "@angular/router";
import {AppRoutes} from "./app.routes";

@NgModule({
    imports: [
        HttpModule,
        BrowserModule,
        MaterialModule.forRoot(),
        RouterModule.forRoot(AppRoutes)
    ],
    declarations: [AppComponent, DashboardComponent],
    bootstrap: [AppComponent]

})
export class AppModule {}
