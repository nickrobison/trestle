/**
 * Created by nrobison on 5/10/17.
 */
import {Route, RouterModule} from "@angular/router";
import {NgModule} from "@angular/core";
import {NavigationComponent} from "./navigation.component";
import {MaterialModule} from "@angular/material";

const routes: Array<Route> = [
    {path: "", component: NavigationComponent}
];

@NgModule({
    declarations: [
        NavigationComponent
    ],
    imports: [
        MaterialModule,
        RouterModule.forChild(routes)
    ],
    bootstrap: [NavigationComponent]
})

export class NavigationModule {}
