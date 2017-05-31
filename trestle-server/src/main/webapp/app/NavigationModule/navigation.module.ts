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
import {AdminModule} from "../AdminModule/admin.module";
import {LoggedInGuard} from "../UserModule/LoggedInGuard";
import {MetricsComponent} from "../AdminModule/metrics/metrics.component";
import {Privileges} from "../UserModule/authentication.service";
import {PermissionsGuard} from "../UserModule/PermissionsGuard";
import {UsersComponent} from "../AdminModule/users/users.component";

interface ITrestleRoute extends Route {
    data?: ITrestleRouteData
}

interface ITrestleRouteData {
    roles: Array<Privileges>;
}

const routes: Array<ITrestleRoute> = [
    {path: "", component: NavigationComponent,
        children: [
            {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
            {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
            {path: "metrics", component: MetricsComponent, canActivate: [LoggedInGuard, PermissionsGuard], data: {roles: [Privileges.ADMIN]}},
            {path: "users", component: UsersComponent, canActivate: [LoggedInGuard, PermissionsGuard], data: {roles: [Privileges.ADMIN]}},
            ]
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
        ExploreModule,
        AdminModule
    ],
    bootstrap: [NavigationComponent]
})

export class NavigationModule {}
