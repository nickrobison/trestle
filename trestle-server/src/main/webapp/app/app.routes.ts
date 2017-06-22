/**
 * Created by nrobison on 1/18/17.
 */
import {Route} from "@angular/router"
import {AppComponent} from "./app.component";
import {LoginComponent} from "./NavigationModule/login/app.login";

export const AppRoutes: Array<Route> = [
    // { path: "", component: AppComponent},
    { path: "admin",  loadChildren: "./AdminModule/admin.module#AdminModule"},
    { path: "explore",  loadChildren: "./ExploreModule/explore.module#ExploreModule"},
    { path: "login", component: LoginComponent},
];
