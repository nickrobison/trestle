import {RouterModule, Routes} from "@angular/router";
import {LoggedInGuard} from "../UserModule/LoggedInGuard";
import {VisualizeComponent} from "./visualize/visualize.component";
import {QueryComponent} from "./query/query.component";
import {ModuleWithProviders} from "@angular/core";
/**
 * Created by nrobison on 5/30/17.
 */

const routes: Routes = [
    {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
    // {path: "", component: VisualizeComponent, canActivate: [LoggedInGuard]},
    {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]}
];

export const exploreRouting: ModuleWithProviders = RouterModule.forChild(routes);