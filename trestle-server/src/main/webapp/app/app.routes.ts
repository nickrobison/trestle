/**
 * Created by nrobison on 1/18/17.
 */
import {LoginComponent} from "./login/app.login";
import {Route} from "@angular/router"

export const AppRoutes: Array<Route> = [
    { path: "", loadChildren: "./admin/admin.module#AdminModule"},
    { path: "login", component: LoginComponent},
    // { path: "", redirectTo: "/dashboard", pathMatch: "full"}
];
