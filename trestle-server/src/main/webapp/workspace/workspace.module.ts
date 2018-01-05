/**
 * Created by nrobison on 1/17/17.
 */
import { NgModule } from "@angular/core";
import { HttpModule } from "@angular/http";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { AppRoutes } from "./workspace.routes";
import { WorkspaceComponent } from "./workspace.component";
import { MaterializeModule } from "angular2-materialize";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { UserModule } from "./UserModule/user.module";
import { NavigationModule } from "./NavigationModule/navigation.module";
import { UIModule } from "./UIModule/ui.module";
import { MaterialModule } from "./MaterialModule/material.module";
import { SharedModule } from "./SharedModule/shared.module";
import "../rxjs-operators";

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
    declarations: [WorkspaceComponent],
    bootstrap: [WorkspaceComponent]

})
export class WorkspaceModule {
}
