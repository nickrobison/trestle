/**
 * Created by nrobison on 1/18/17.
 */
import {Route} from "@angular/router"
import {LoginComponent} from "./NavigationModule/login/app.login";
import {NavigationComponent} from "./NavigationModule/navigation.component";
import {LoggedInGuard} from "./UserModule/LoggedInGuard";
import {VisualizeComponent} from "./ExploreModule/visualize/visualize.component";
import {QueryComponent} from "./ExploreModule/query/query.component";

export const AppRoutes: Array<Route> = [
    // { path: "", loadChildren: "./admin/admin.module#AdminModule"},
    // { path: "", loadChildren: "./navigation/navigation.module#NavigationModule"},
    { path: "", component: NavigationComponent},
    { path: "login", component: LoginComponent},
    {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
    {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
    // { path: "", redirectTo: "/dashboard", pathMatch: "full"}
];
