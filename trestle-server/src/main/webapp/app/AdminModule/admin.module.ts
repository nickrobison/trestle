/**
 * Created by nrobison on 1/19/17.
 */
import {Route, RouterModule} from "@angular/router";
import {DashboardComponent} from "./dashboard/app.dashboard";
import {NgModule} from "@angular/core";
import {MaterialModule} from "@angular/material";
import {AdminComponent} from "./admin.component";
import {UsersComponent, MapValuesPipe} from "./users/users.component";
import {CommonModule} from "@angular/common";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {UserAddDialog} from "./users/users.add.dialog";
import {MetricsComponent} from "./metrics/metrics.component";
import {MetricsService} from "./metrics/metrics.service";
import {MetricsGraph} from "./metrics/metrics-graph.component";
import {UserModule} from "../UserModule/user.module";
import {Privileges} from "../UserModule/authentication.service";
import {LoggedInGuard} from "../UserModule/LoggedInGuard";
import {PermissionsGuard} from "../UserModule/PermissionsGuard";

interface ITrestleRoute extends Route {
    data?: ITrestleRouteData
}

interface ITrestleRouteData {
    roles: Array<Privileges>;
}

const routes: Array<ITrestleRoute> = [
    {path: "", component: AdminComponent, children: [
        {path: "dashboard", component: DashboardComponent},
        // {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
        // {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
        {path: "users", component: UsersComponent, canActivate: [LoggedInGuard, PermissionsGuard], data: {roles: [Privileges.ADMIN]}},
        {path: "metrics", component: MetricsComponent, canActivate: [LoggedInGuard, PermissionsGuard], data: {roles: [Privileges.ADMIN]}},
        {path: "", redirectTo: "/dashboard", pathMatch: "full"}
    ]}
];

@NgModule({
    declarations: [DashboardComponent,
        AdminComponent,
        UsersComponent,
        UserAddDialog,
        MetricsComponent,
        MetricsGraph,
        MapValuesPipe],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        // RouterModule.forChild(routes),
        MaterialModule,
        UserModule
    ],
    providers: [MetricsService],
    entryComponents: [UserAddDialog],
    bootstrap: [AdminComponent]
})

export class AdminModule {}
