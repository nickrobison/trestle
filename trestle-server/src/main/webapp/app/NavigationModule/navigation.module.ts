/**
 * Created by nrobison on 5/10/17.
 */
import {Route, RouterModule} from "@angular/router";
import {NgModule} from "@angular/core";
import {NavigationComponent} from "./navigation.component";
import {MaterialModule} from "@angular/material";
import {UserModule} from "../UserModule/user.module";
import {CommonModule} from "@angular/common";
import {LoginComponent} from "./login/app.login";
import {FormsModule, ReactiveFormsModule} from "@angular/forms"
import {VisualizeComponent} from "../ExploreModule/visualize/visualize.component";
import {QueryComponent} from "../ExploreModule/query/query.component";
import {ExploreModule} from "../ExploreModule/explore.module";

const routes: Array<Route> = [
    {path: "", component: NavigationComponent,
        children: [
            {path: "visualize", component: VisualizeComponent},
            {path: "query", component: QueryComponent}]
    // children: [{path: "explore", loadChildren: "../ExploreModule/explore.module#ExploreModule"}]
    }
];

@NgModule({
    declarations: [
        NavigationComponent,
        LoginComponent
    ],
    imports: [
        MaterialModule,
        RouterModule.forChild(routes),
        UserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ExploreModule
    ],
    bootstrap: [NavigationComponent]
})

export class NavigationModule {}
