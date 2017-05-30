/**
 * Created by nrobison on 1/18/17.
 */
import {Route} from "@angular/router"
import {LoginComponent} from "./NavigationModule/login/app.login";
import {NavigationComponent} from "./NavigationModule/navigation.component";

export const AppRoutes: Array<Route> = [
    { path: "", component: NavigationComponent},
    { path: "login", component: LoginComponent},
];
