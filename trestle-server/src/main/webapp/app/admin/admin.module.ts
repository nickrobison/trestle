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
import {QueryComponent} from "./query/query.component";
import {CodeMirrorComponent} from "./query/codemirror/codemirror.component";
import {QueryService} from "./query/query.service";
import {QueryViewer} from "./query/query-viewer/query-viewer.component";
import {VisualizeService} from "./visualize/visualize.service";
import {VisualizeComponent} from "./visualize/visualize.component";
import {IndividualGraph} from "./visualize/individual-graph.component";
import {IndividualValueDialog} from "./visualize/individual-value.dialog";
import {MetricsComponent} from "./metrics/metrics.component";
import {MetricsService} from "./metrics/metrics.service";
import {MetricsGraph} from "./metrics/metrics-graph.component";
import {FactHistoryGraph} from "./visualize/fact-graph.component";
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
        {path: "query", component: QueryComponent, canActivate: [LoggedInGuard]},
        {path: "visualize", component: VisualizeComponent, canActivate: [LoggedInGuard]},
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
        CodeMirrorComponent,
        QueryComponent,
        QueryViewer,
        VisualizeComponent,
        IndividualGraph,
        FactHistoryGraph,
        IndividualValueDialog,
        MetricsComponent,
        MetricsGraph,
        MapValuesPipe],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild(routes),
        MaterialModule,
        UserModule
    ],
    providers: [QueryService, VisualizeService, MetricsService],
    entryComponents: [UserAddDialog, IndividualValueDialog],
    bootstrap: [AdminComponent]
})

export class AdminModule {}
